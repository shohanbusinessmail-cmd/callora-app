package com.callora.app.core.datastore

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.callora.app.core.model.AppLanguage
import com.callora.app.core.model.ThemeMode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsRepositoryTest {

    private val context: Application
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun defaults_and_updates_persist() = runTest {
        // Unique datastore name per class instance to avoid cross-test file state.
        val repository = SettingsRepository(context)
        val initial = repository.settings.first()
        assertThat(initial.onboardingCompleted).isFalse()
        assertThat(initial.language).isEqualTo(AppLanguage.ENGLISH)

        repository.setOnboardingCompleted(true)
        repository.setLanguage(AppLanguage.BENGALI)
        repository.setThemeMode(ThemeMode.DARK)
        repository.setNotificationsEnabled(false)

        val updated = repository.settings.first()
        assertThat(updated.onboardingCompleted).isTrue()
        assertThat(updated.language).isEqualTo(AppLanguage.BENGALI)
        assertThat(updated.themeMode).isEqualTo(ThemeMode.DARK)
        assertThat(updated.notificationsEnabled).isFalse()
    }
}
