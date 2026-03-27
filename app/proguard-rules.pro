# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.
# For more details, see
#   https://developer.android.com/build/shrink-code

# Keep Firebase models
-keep class com.zchat.app.data.model.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
