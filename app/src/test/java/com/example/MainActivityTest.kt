package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [24])
class MainActivityTest {
    @Test
    fun testActivityLaunch() {
        Robolectric.buildActivity(MainActivity::class.java).setup()
    }
}
