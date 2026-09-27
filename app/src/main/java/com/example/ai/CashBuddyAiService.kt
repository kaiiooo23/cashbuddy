package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.CashBuddyAiResponse
import com.example.data.TransactionItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CashBuddyAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun processPrompt(input: String): CashBuddyAiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val isValidKey = apiKey.isNotBlank() &&
                !apiKey.startsWith("MY_GEMINI") &&
                apiKey != "null"

        if (isValidKey) {
            try {
                val geminiResponse = callGeminiApi(input, apiKey)
                if (geminiResponse != null) {
                    return@withContext geminiResponse
                }
            } catch (e: Exception) {
                Log.w("CashBuddyAiService", "Gemini API error, falling back to local engine: ${e.message}")
            }
        }

        // Fallback to our high-accuracy local parser
        LocalCashBuddyParser.parse(input, dateFormat.format(Date()))
    }

    private fun callGeminiApi(userInput: String, apiKey: String): CashBuddyAiResponse? {
        val today = dateFormat.format(Date())
        val systemPrompt = """
            Anda adalah "CashBuddy AI", asisten finansial pribadi dan pencatat keuangan cerdas untuk mahasiswa. Karakter Anda ramah, adaptif, solutif, dan menggunakan gaya bahasa yang santai namun tetap informatif (ala aplikasi Cash App).
            Tanggal hari ini adalah $today.

            TUGAS UTAMA:
            Anda menangani 2 jenis input dari pengguna:

            1. INPUT PENCATATAN TRANSAKSI (NATURAL LANGUAGE PARSING)
            Jika pengguna memberikan pernyataan transaksi (seperti "beli kopi 25rb" atau "dapat uang saku 500rb"):
            Output WAJIB berupa JSON MURNI tanpa format markdown lain di luar blok kode JSON agar mudah diproses oleh sistem back-end.

            Struktur JSON:
            {
              "mode": "TRANSACTION",
              "transactions": [
                {
                  "type": "EXPENSE" | "INCOME",
                  "amount": number (konversi k/rb/jt ke ribuan utuh, misal 25k -> 25000, 1.5jt -> 1500000),
                  "category": string ("Makanan", "Transportasi", "Akademik", "Uang Saku", "Hiburan", "Tagihan", "Lainnya"),
                  "description": string,
                  "date": "YYYY-MM-DD" (gunakan tanggal saat ini jika tidak disebutkan)
                }
              ],
              "reply_message": string (konfirmasi singkat, ramah, dan santai)
            }

            2. INPUT PERTANYAAN / KONSULTASI FINANSIAL
            Jika pengguna bertanya, meminta saran, analisis, atau tips hemat (seperti "Sisa uang saku 200rb cukup ga buat seminggu?"):
            Output WAJIB berupa format JSON berikut:
            {
              "mode": "ASSISTANT",
              "reply_message": string (jawaban informatif, to the point, memberikan solusi hemat ala kehidupan mahasiswa, dan menggunakan emoticon yang relevan)
            }

            ATURAN & KETENTUAN KHUSUS:
            1. Jika nominal transaksi tidak jelas atau ambigu, set "mode": "ASSISTANT" dan minta klarifikasi dengan ramah.
            2. Selalu konversi satuan informal (misal: "25rb", "15k", "setengah juta") ke angka numerik yang benar.
            3. Kategori harus selalu relevan dengan konteks kehidupan perkuliahan/mahasiswa.
            4. Jangan pernah menyertakan teks tambahan di luar JSON balasan. Output HANYA JSON.
        """.trimIndent()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "$systemPrompt\n\nInput Pengguna: $userInput")
                        })
                    })
                })
            }
            put("contents", contentsArray)

            // Optional generation config for pure JSON
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.4)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("CashBuddyAiService", "API call failed with code: ${response.code}")
            return null
        }

        val responseBody = response.body?.string() ?: return null
        val root = JSONObject(responseBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        if (parts.length() == 0) return null

        var text = parts.getJSONObject(0).optString("text", "")
        if (text.isBlank()) return null

        // Clean any markdown formatting if present
        text = text.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        text = text.trim()

        return parseJsonResponse(text)
    }

    private fun parseJsonResponse(rawJson: String): CashBuddyAiResponse? {
        return try {
            val obj = JSONObject(rawJson)
            val mode = obj.optString("mode", "ASSISTANT")
            val replyMessage = obj.optString("reply_message", "")

            val transactionsList = mutableListOf<TransactionItem>()
            val txArray = obj.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val tx = txArray.getJSONObject(i)
                    val type = tx.optString("type", "EXPENSE")
                    val amount = tx.optLong("amount", 0L)
                    val category = tx.optString("category", "Lainnya")
                    val description = tx.optString("description", "")
                    val date = tx.optString("date", dateFormat.format(Date()))
                    if (amount > 0) {
                        transactionsList.add(
                            TransactionItem(
                                type = type,
                                amount = amount,
                                category = category,
                                description = description,
                                date = date
                            )
                        )
                    }
                }
            }

            CashBuddyAiResponse(
                mode = mode,
                transactions = if (transactionsList.isNotEmpty()) transactionsList else null,
                reply_message = replyMessage
            )
        } catch (e: Exception) {
            Log.e("CashBuddyAiService", "Failed to parse JSON response: $rawJson", e)
            null
        }
    }
}
