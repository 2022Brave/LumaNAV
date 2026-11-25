package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.maps.DestinationSearchRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LumaNAV", appName)
  }

  @Test
  fun `parse external google maps geo navigation and shared intents`() {
    val navCoords = DestinationSearchRepository.parseExternalMapsIntent(
      dataString = "google.navigation:q=28.6139,77.2090(India+Gate)",
      sharedText = null
    )
    assertNotNull(navCoords)
    assertEquals(28.6139, navCoords?.latitude ?: 0.0, 0.0001)
    assertEquals(77.2090, navCoords?.longitude ?: 0.0, 0.0001)

    val geoQuery = DestinationSearchRepository.parseExternalMapsIntent(
      dataString = "geo:0,0?q=Haridwar+Railway+Station",
      sharedText = null
    )
    assertEquals("Haridwar Railway Station", geoQuery?.queryLabel)

    val sharedPin = DestinationSearchRepository.parseExternalMapsIntent(
      dataString = null,
      sharedText = "Connaught Place\n28.6315, 77.2167\nhttps://maps.app.goo.gl/xyz"
    )
    assertNotNull(sharedPin)
    assertEquals("Connaught Place", sharedPin?.queryLabel)
    assertEquals(28.6315, sharedPin?.latitude ?: 0.0, 0.0001)
    assertEquals(77.2167, sharedPin?.longitude ?: 0.0, 0.0001)
  }
}
