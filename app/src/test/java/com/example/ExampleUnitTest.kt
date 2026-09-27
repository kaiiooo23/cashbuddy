package com.example

import com.example.ai.LocalCashBuddyParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseKopiExpense() {
        val result = LocalCashBuddyParser.parse("beli kopi 25rb")
        assertEquals("TRANSACTION", result.mode)
        assertNotNull(result.transactions)
        val tx = result.transactions!!.first()
        assertEquals("EXPENSE", tx.type)
        assertEquals(25000L, tx.amount)
        assertEquals("Makanan", tx.category)
        assertTrue(result.reply_message.isNotBlank())
    }

    @Test
    fun testParseIncomeUangSaku() {
        val result = LocalCashBuddyParser.parse("dapat uang saku 500rb")
        assertEquals("TRANSACTION", result.mode)
        assertNotNull(result.transactions)
        val tx = result.transactions!!.first()
        assertEquals("INCOME", tx.type)
        assertEquals(500000L, tx.amount)
        assertEquals("Uang Saku", tx.category)
    }

    @Test
    fun testParseMillionAmount() {
        val result = LocalCashBuddyParser.parse("kiriman ortu 1.5jt")
        assertEquals("TRANSACTION", result.mode)
        val tx = result.transactions!!.first()
        assertEquals(1500000L, tx.amount)
        assertEquals("INCOME", tx.type)
    }

    @Test
    fun testParseStudentConsultation() {
        val result = LocalCashBuddyParser.parse("Sisa uang saku 200rb cukup ga buat seminggu?")
        assertEquals("ASSISTANT", result.mode)
        assertTrue(result.reply_message.contains("seminggu") || result.reply_message.contains("warteg"))
    }

    @Test
    fun testParseMissingAmountClarification() {
        val result = LocalCashBuddyParser.parse("beli kopi")
        assertEquals("ASSISTANT", result.mode)
        assertTrue(result.reply_message.contains("nominal") || result.reply_message.contains("rupiah") || result.reply_message.contains("harga"))
    }
}
