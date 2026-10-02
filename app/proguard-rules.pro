# Room, Hilt, Firebase, RevenueCat and kotlinx.serialization ship consumer rules.
# Navigation type-safe routes are @Serializable classes; keep their serializers.
-keepclassmembers @kotlinx.serialization.Serializable class com.myday.litu.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.myday.litu.**$$serializer { *; }
