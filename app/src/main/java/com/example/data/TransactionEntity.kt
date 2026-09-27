package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" | "INCOME"
    val amount: Long, // e.g. 25000
    val category: String, // "Makanan", "Transportasi", "Akademik", "Uang Saku", "Hiburan", "Tagihan", "Lainnya"
    val description: String,
    val date: String, // "YYYY-MM-DD"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
