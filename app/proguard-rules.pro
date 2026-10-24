# Callora ProGuard rules.
# Keep rules intentionally minimal: all used libraries ship their own consumer rules.

# Keep Room generated implementations and entity classes.
-keep class com.callora.app.core.database.** { *; }

# libphonenumber metadata reflection safety.
-keep class com.google.i18n.phonenumbers.** { *; }
-dontwarn com.google.i18n.phonenumbers.**
