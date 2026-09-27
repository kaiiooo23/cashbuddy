package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CategoryUtils {

    data class CategoryVisual(
        val name: String,
        val icon: ImageVector,
        val color: Color,
        val containerColor: Color
    )

    fun getCategoryVisual(category: String): CategoryVisual {
        return when (category.lowercase(Locale.ROOT)) {
            "makanan" -> CategoryVisual(
                name = "Makanan",
                icon = Icons.Default.Restaurant,
                color = Color(0xFFFF8A00),
                containerColor = Color(0x26FF8A00)
            )
            "transportasi" -> CategoryVisual(
                name = "Transportasi",
                icon = Icons.Default.DirectionsBus,
                color = Color(0xFF00B4D8),
                containerColor = Color(0x2600B4D8)
            )
            "akademik" -> CategoryVisual(
                name = "Akademik",
                icon = Icons.Default.School,
                color = Color(0xFFA855F7),
                containerColor = Color(0x26A855F7)
            )
            "uang saku" -> CategoryVisual(
                name = "Uang Saku",
                icon = Icons.Default.LocalAtm,
                color = Color(0xFF00D632),
                containerColor = Color(0x2600D632)
            )
            "hiburan" -> CategoryVisual(
                name = "Hiburan",
                icon = Icons.Default.Movie,
                color = Color(0xFFEC4899),
                containerColor = Color(0x26EC4899)
            )
            "tagihan" -> CategoryVisual(
                name = "Tagihan",
                icon = Icons.Default.Receipt,
                color = Color(0xFFFBBF24),
                containerColor = Color(0x26FBBF24)
            )
            else -> CategoryVisual(
                name = "Lainnya",
                icon = Icons.Default.Category,
                color = Color(0xFF94A3B8),
                containerColor = Color(0x2694A3B8)
            )
        }
    }

    fun formatRupiah(amount: Long): String {
        return String.format(Locale.GERMANY, "Rp %,d", amount)
    }

    fun formatDateIndo(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = parser.parse(dateStr) ?: return dateStr
            val formatter = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"))
            formatter.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}
