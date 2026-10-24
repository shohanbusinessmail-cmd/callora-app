package com.callora.app.core.localization

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.callora.app.core.model.AppLanguage

/**
 * Per-app language handling via AppCompat's back-ported locale manager
 * (supported back to API 24; Callora's minSdk is 26). The chosen language
 * persists across process restarts inside AppCompat's own storage.
 */
object LocaleManager {

    fun apply(language: AppLanguage) {
        val locales = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentTag(): String? =
        AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() }
}
