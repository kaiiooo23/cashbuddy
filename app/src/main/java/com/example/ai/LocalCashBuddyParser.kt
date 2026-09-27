package com.example.ai

import com.example.data.CashBuddyAiResponse
import com.example.data.TransactionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object LocalCashBuddyParser {

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun parse(input: String, currentDateStr: String = DATE_FORMAT.format(Date())): CashBuddyAiResponse {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check if it's a question or financial advice consultation
        if (isConsultation(lower)) {
            val reply = generateConsultationReply(lower)
            return CashBuddyAiResponse(
                mode = "ASSISTANT",
                reply_message = reply
            )
        }

        // 2. Extract Amount
        val amount = extractAmount(lower)

        // If no amount found, check if it's an attempted transaction without amount
        if (amount == null) {
            if (isTransactionAttempt(lower)) {
                return CashBuddyAiResponse(
                    mode = "ASSISTANT",
                    reply_message = "Wah, transaksinya mau dicatat nih! Tapi nominal harganya belum ada nih bro/sis. Berapa rupiah ya? Contoh: 'Beli kopi 22rb' atau 'Nasi padang 15k' 💸"
                )
            } else {
                return CashBuddyAiResponse(
                    mode = "ASSISTANT",
                    reply_message = "Halo! Aku CashBuddy AI, siap bantu catat pengeluaran & kasih tips hemat mahasiswa. Coba ketik misal: 'Beli ayam geprek 15rb' atau tanya 'Tips hemat seminggu' 😎"
                )
            }
        }

        // 3. Determine Type (EXPENSE or INCOME)
        val isIncome = isIncomeKeywords(lower)
        val type = if (isIncome) "INCOME" else "EXPENSE"

        // 4. Determine Category
        val category = determineCategory(lower, isIncome)

        // 5. Clean Description
        val description = cleanDescription(trimmed)

        val transaction = TransactionItem(
            type = type,
            amount = amount,
            category = category,
            description = if (description.isNotBlank()) description else if (isIncome) "Pemasukan" else "Pengeluaran",
            date = currentDateStr
        )

        val formattedRupiah = formatRupiah(amount)
        val friendlyReply = if (isIncome) {
            "Mantap cuan masuk! 🤑 $description sebesar $formattedRupiah udah masuk kategori $category. Jangan langsung kalap ya!"
        } else {
            "Siap bos! $description sebesar $formattedRupiah udah dicatat ke $category. Dompet aman terkendali! ✨"
        }

        return CashBuddyAiResponse(
            mode = "TRANSACTION",
            transactions = listOf(transaction),
            reply_message = friendlyReply
        )
    }

    private fun isConsultation(text: String): Boolean {
        if (text.contains("?")) return true
        val triggers = listOf(
            "cukup ga", "cukup gak", "cukup gk", "gimana", "tips hemat",
            "saran", "rekomendasi", "bisa ga", "bisa gak", "bisa hemat",
            "cara hemat", "uang saku sisa", "sisa uang", "akhir bulan",
            "makan apa", "harus gimana", "bagaimana", "konsultasi", "solusi"
        )
        return triggers.any { text.contains(it) }
    }

    private fun isTransactionAttempt(text: String): Boolean {
        val triggers = listOf(
            "beli", "bayar", "jajan", "makan", "minum", "ongkos", "bensin",
            "kiriman", "uang saku", "gaji", "tf", "transfer", "top up", "topup", "pesan"
        )
        return triggers.any { text.contains(it) }
    }

    private fun isIncomeKeywords(text: String): Boolean {
        val incomeWords = listOf(
            "dapat", "dapet", "kiriman", "uang saku", "transfer dari", "tf dari",
            "gaji", "magang", "freelance", "beasiswa", "cuan", "nemu", "jual",
            "bonus", "hadiah", "pemasukan", "masuk"
        )
        return incomeWords.any { text.contains(it) }
    }

    fun extractAmount(text: String): Long? {
        // Pattern 1: Millions ("1.5jt", "1,5jt", "2jt", "1 juta", "setengah juta")
        if (text.contains("setengah juta") || text.contains("setengah jt")) {
            return 500_000L
        }

        val jtPattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:jt|juta)")
        val jtMatcher = jtPattern.matcher(text)
        if (jtMatcher.find()) {
            val numStr = jtMatcher.group(1)?.replace(",", ".") ?: return null
            val num = numStr.toDoubleOrNull() ?: return null
            return (num * 1_000_000).toLong()
        }

        // Pattern 2: Thousands with rb/k ("25rb", "25k", "25ribu", "25 rebu", "150 k")
        val kPattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:rb|k|ribu|rebu)")
        val kMatcher = kPattern.matcher(text)
        if (kMatcher.find()) {
            val numStr = kMatcher.group(1)?.replace(",", ".") ?: return null
            val num = numStr.toDoubleOrNull() ?: return null
            return (num * 1_000).toLong()
        }

        // Pattern 3: Full numbers with optional dots/commas ("Rp 25.000", "25000", "Rp. 500000")
        val fullNumPattern = Pattern.compile("(?:rp\\.?)?\\s*(\\d{1,3}(?:[.]\\d{3})+|\\d{4,9})")
        val fullMatcher = fullNumPattern.matcher(text)
        if (fullMatcher.find()) {
            val raw = fullMatcher.group(1)?.replace(".", "") ?: return null
            return raw.toLongOrNull()
        }

        return null
    }

    private fun determineCategory(text: String, isIncome: Boolean): String {
        if (isIncome) {
            return "Uang Saku"
        }

        // Categories: "Makanan", "Transportasi", "Akademik", "Uang Saku", "Hiburan", "Tagihan", "Lainnya"
        val makananWords = listOf(
            "kopi", "makan", "nasi", "padang", "geprek", "ayam", "indomie", "mie",
            "bakso", "soto", "warteg", "es teh", "jus", "sarapan", "dinner",
            "jajan", "snack", "roti", "kantin", "minum", "boba", "starbucks", "burger"
        )
        if (makananWords.any { text.contains(it) }) return "Makanan"

        val transportWords = listOf(
            "ojol", "gojek", "grab", "maxim", "bensin", "pertalite", "pertamax",
            "angkot", "parkir", "krl", "busway", "transjakarta", "mrt", "kereta", "tiket"
        )
        if (transportWords.any { text.contains(it) }) return "Transportasi"

        val akademikWords = listOf(
            "buku", "print", "fotocopy", "jilid", "modul", "ukt", "spp", "kuliah",
            "praktikum", "seminar", "skripsi", "alattulis", "pulpen", "kertas", "atk"
        )
        if (akademikWords.any { text.contains(it) }) return "Akademik"

        val tagihanWords = listOf(
            "kos", "kost", "kontrakan", "wifi", "indihome", "listrik", "token",
            "air", "pdam", "kuota", "pulsa", "paket data"
        )
        if (tagihanWords.any { text.contains(it) }) return "Tagihan"

        val hiburanWords = listOf(
            "nonton", "bioskop", "cinema", "xxi", "netflix", "spotify", "steam",
            "game", "top up game", "mabar", "hangout", "nongkrong", "karaoke"
        )
        if (hiburanWords.any { text.contains(it) }) return "Hiburan"

        val uangSakuWords = listOf(
            "saku", "ortu", "kiriman", "tabungan"
        )
        if (uangSakuWords.any { text.contains(it) }) return "Uang Saku"

        return "Lainnya"
    }

    private fun cleanDescription(text: String): String {
        // Remove trailing or leading conversational bits
        var clean = text
        clean = clean.replace(Regex("(?i)^(tolong\\s+)?(catat\\s+|catat dong\\s+|masukin\\s+)?"), "")
        clean = clean.replace(Regex("(?i)\\s*(ya|dong|nih|tadi|barusan|bro|sis|min)\\s*$"), "")
        return clean.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun generateConsultationReply(text: String): String {
        return when {
            text.contains("seminggu") || text.contains("7 hari") -> {
                "Sisa budget buat seminggu? Masih aman banget kalau diatur gini bro:\n" +
                        "1. Alokasi makan Rp 20rb - 25rb/hari (menu warteg / nasi telur / sayur hemat) 🍚\n" +
                        "2. Bawa tumbler air minum ke kampus biar gak jajan air mineral 💧\n" +
                        "3. Tahan dulu nongkrong kafe ber-AC, seduh kopi sachet kosan dulu! ☕\n" +
                        "Kamu pasti bisa lolos sampai kiriman berikutnya! 💪"
            }
            text.contains("kos") || text.contains("kost") || text.contains("masak") -> {
                "Tips hemat anak kos ala CashBuddy:\n" +
                        "• Rice cooker is life! Masak nasi sendiri hemat 50% pengeluaran makan.\n" +
                        "• Beli telur 1 kg & tempe/tahu di pasar tradisional untuk stok protein hemat.\n" +
                        "• Manfaatkan wifi kampus buat download materi kuliah & hiburan offline. 📶💡"
            }
            text.contains("nongkrong") || text.contains("kopi") -> {
                "Trik tetap gaul tapi gak jebol:\n" +
                        "• Pilih menu 'Americano' atau es teh jumbo kalau ke coffee shop (paling ramah kantong).\n" +
                        "• Jajan sebelum nongkrong, jadi di kafe cuma order minum aja. Dompet aman, silaturahmi tetap jalan! 🤙"
            }
            text.contains("akhir bulan") || text.contains("krisis") || text.contains("menipis") -> {
                "Mode Survival Aktif! 🚨\n" +
                        "1. Prioritaskan cuma 2 hal: Makan pokok + Transportasi kuliah.\n" +
                        "2. Bekal air & camilan dari kos saat ke kampus.\n" +
                        "3. Cek sisa kuota wifi jangan dipakai streaming video resolusi 4K dulu ya! Tetap semangat mahasiswa pejuang! 🎓✨"
            }
            else -> {
                "Pertanyaan bagus nih! Buat mahasiswa, kuncinya pakai aturan 50-30-20 yang dimodifikasi:\n" +
                        "• 60% Kebutuhan Pokok (Makan, Kos, Bensin)\n" +
                        "• 25% Akademik & Kuota\n" +
                        "• 15% Dana Darurat / Tabungan kecil-kecilan\n" +
                        "Ada yang mau kamu tanyakan lagi soal alokasi budget? CashBuddy siap bantu! 🤝📊"
            }
        }
    }

    fun formatRupiah(amount: Long): String {
        return String.format(Locale.GERMANY, "Rp %,d", amount)
    }
}
