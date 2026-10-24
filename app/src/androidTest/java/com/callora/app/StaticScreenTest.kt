package com.callora.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callora.app.core.designsystem.component.StatusChip
import com.callora.app.core.designsystem.theme.CalloraTheme
import com.callora.app.core.model.ForwardingState
import com.callora.app.feature.about.AboutScreen
import com.callora.app.feature.troubleshooting.TroubleshootingScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaticScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun about_screen_shows_developer_credit_and_contact() {
        composeRule.setContent {
            CalloraTheme { AboutScreen(onBack = {}) }
        }
        composeRule.onNodeWithText(context.getString(R.string.about_developer_name))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.about_contact_email))
            .assertIsDisplayed()
    }

    @Test
    fun troubleshooting_lists_guidance() {
        composeRule.setContent {
            CalloraTheme { TroubleshootingScreen(onBack = {}) }
        }
        composeRule.onNodeWithText(context.getString(R.string.troubleshooting_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.trouble_api_title))
            .assertIsDisplayed()
    }

    @Test
    fun verified_state_chip_renders_localized_label() {
        composeRule.setContent {
            CalloraTheme {
                StatusChip(
                    state = ForwardingState.ACTIVE_VERIFIED,
                    label = context.getString(R.string.state_ACTIVE_VERIFIED),
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.state_ACTIVE_VERIFIED))
            .assertIsDisplayed()
    }
}
