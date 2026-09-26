# Kotlinx Serialization generated serializers are referenced reflectively.
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.naviify.app.**$$serializer { *; }
-keepclassmembers class com.naviify.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.naviify.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room entities and generated database accessors.
-keep class com.naviify.app.core.storage.room.** { *; }

# Media3 SessionToken is parceled across app/service boundaries.
-keep class androidx.media3.session.SessionToken { *; }
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# Retrofit interfaces are instantiated reflectively.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# DataStore/OkHttp ship their own consumer rules; keep the JVM crypto paths used
# by the Subsonic client.
-keep class okhttp3.internal.platform.** { *; }
