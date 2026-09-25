package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlertLevel
import com.example.monitor.ProcNetParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NetSentry", appName)
  }

  @Test
  fun `verify assessConnectionRisk flags unencrypted port 80 as suspicious`() {
    val (alertLevel, details) = ProcNetParser.assessConnectionRisk("TCP", "93.184.216.34", 80, "ESTABLISHED")
    assertEquals(AlertLevel.SUSPICIOUS, alertLevel)
    assertNotNull(details)
  }

  @Test
  fun `verify assessConnectionRisk marks standard https 443 as normal`() {
    val (alertLevel, _) = ProcNetParser.assessConnectionRisk("TCP", "142.250.190.46", 443, "ESTABLISHED")
    assertEquals(AlertLevel.NORMAL, alertLevel)
  }
}
