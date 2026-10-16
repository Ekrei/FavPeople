plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
    // KSP — генератор кода для Room (быстрее kapt и рекомендован для Kotlin 2.x)
    id("com.google.devtools.ksp") version "2.0.21-1.0.25" apply false
}