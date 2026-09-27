package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BudgetAlertLevel
import com.example.data.CashBuddyDatabase
import com.example.data.CashBuddyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CashBuddy AI", appName)
  }

  @Test
  fun `test 80 percent category budget alert trigger`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CashBuddyDatabase.getDatabase(context)
    val repo = CashBuddyRepository(db.transactionDao(), context)

    // Set budget Makanan to 100,000
    repo.setCategoryBudget("Makanan", 100_000L)

    // Previous spent: 70,000 (70%). Added: 15,000 (Total: 85,000 = 85%)
    val alert = repo.checkBudgetAlert("Makanan", 70_000L, 15_000L)

    assertNotNull(alert)
    assertEquals(BudgetAlertLevel.WARNING_80, alert?.level)
    assertEquals(85, alert?.percentage)
    assertEquals(85_000L, alert?.spentAmount)
  }

  @Test
  fun `test 100 percent category budget exceeded trigger`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CashBuddyDatabase.getDatabase(context)
    val repo = CashBuddyRepository(db.transactionDao(), context)

    // Set budget Hiburan to 200,000
    repo.setCategoryBudget("Hiburan", 200_000L)

    // Previous spent: 180,000 (90%). Added: 30,000 (Total: 210,000 = 105%)
    val alert = repo.checkBudgetAlert("Hiburan", 180_000L, 30_000L)

    assertNotNull(alert)
    assertEquals(BudgetAlertLevel.EXCEEDED_100, alert?.level)
    assertEquals(105, alert?.percentage)
    assertEquals(210_000L, alert?.spentAmount)
  }

  @Test
  fun `test under 80 percent category budget produces no alert`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = CashBuddyDatabase.getDatabase(context)
    val repo = CashBuddyRepository(db.transactionDao(), context)

    repo.setCategoryBudget("Transportasi", 200_000L)

    // Previous spent: 50,000. Added: 50,000 (Total: 100,000 = 50%)
    val alert = repo.checkBudgetAlert("Transportasi", 50_000L, 50_000L)

    assertNull(alert)
  }
}
