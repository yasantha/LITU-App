# Room, Hilt, Firebase, RevenueCat and kotlinx.serialization ship consumer rules.
# Navigation type-safe routes are @Serializable classes; keep their serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.myday.litu.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.myday.litu.**$$serializer { *; }

# play review-ktx references an annotation that ships with newer play-services-basement.
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite
