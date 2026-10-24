package com.callora.app

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callora.app.feature.onboarding.OnboardingScreen
import com.callora.app.feature.onboarding.OnboardingViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests require an emulator or physical device. The CI workflow compiles
 * these tests with assembleDebugAndroidTest but cannot execute them without a
 * connected device — see docs/TESTING.md for the honest verification matrix.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun first_launch_shows_welcome_page() {
        val context = composeRule.activity
        context.runOnUiThread {
            context.setContent {
                val app = context.application as CalloraApplication
                OnboardingScreen(
                    viewModel = OnboardingViewModel(app),
                    onFinished = {},
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.onboarding_welcome_title))
            .assertIsDisplayed()
    }

    @Test
    fun next_button_advances_to_principle_page() {
        val context = composeRule.activity
        context.runOnUiThread {
            context.setContent {
                val app = context.application as CalloraApplication
                OnboardingScreen(
                    viewModel = OnboardingViewModel(app),
                    onFinished = {},
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.onboarding_button_next))
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.onboarding_principle_title))
            .assertIsDisplayed()
    }
}
