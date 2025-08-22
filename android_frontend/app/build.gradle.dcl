androidApplication {
    namespace = "org.example.locationtime"

    dependencies {
        // Core and Lifecycle
        implementation("androidx.core:core-ktx:1.13.1")
        implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

        // Activity
        implementation("androidx.activity:activity-ktx:1.9.2")

        // Material Components (for Theme.MaterialComponents parent)
        implementation("com.google.android.material:material:1.12.0")

        // Google Play Services Location
        implementation("com.google.android.gms:play-services-location:21.3.0")

        // Coroutines
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    }
}
