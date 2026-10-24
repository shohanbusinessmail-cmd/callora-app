// Top-level build file. Plugin versions are declared in the version catalog
// and applied per-module. No application logic lives in this file.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
