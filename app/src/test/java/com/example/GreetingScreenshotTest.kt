package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.Event
import com.example.ui.home.EventCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class EventCardScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun eventCard_screenshot() {
    composeTestRule.setContent { 
      MyApplicationTheme { 
        EventCard(
          event = Event(
            id = 1,
            name = "Urodziny Marty",
            targetTimestamp = System.currentTimeMillis() + 86400000L * 162, // 162 days in future
            colorArgb = 0xFFEADDFF.toInt()
          ),
          currentTime = System.currentTimeMillis(),
          onClick = {}
        )
      } 
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/event_card.png")
  }
}
