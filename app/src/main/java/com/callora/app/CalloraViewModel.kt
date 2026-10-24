package com.callora.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.callora.app.core.di.AppContainer

/** Shared ViewModel base exposing the manual DI container. */
abstract class CalloraViewModel(app: Application) : AndroidViewModel(app) {
    protected val container: AppContainer
        get() = getApplication<CalloraApplication>().container
}
