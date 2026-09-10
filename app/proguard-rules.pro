# Cryptogram Infinite release keep rules (design doc section 9).

# ---- kotlinx.serialization -------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep the generated serializers and companion factory methods.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}

# All @Serializable model classes in the app.
-keep @kotlinx.serialization.Serializable class dev.milan.cryptogram.** { *; }
-keepclassmembers class dev.milan.cryptogram.** {
    *** Companion;
}

# ---- Room ----------------------------------------------------------------
# Room generates implementations at build time; keep entities and the
# database/DAO types R8 might otherwise strip member metadata from.
-keep class dev.milan.cryptogram.data.db.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ---- Play Billing ------------------------------------------------------
-keep class com.android.billingclient.api.** { *; }

# ---- Google Mobile Ads / UMP ---------------------------------------
# Both ship consumer rules; keep listeners referenced from XML/reflection.
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.**

# ---- OkHttp ---------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
