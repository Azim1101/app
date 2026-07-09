buildscript {
    extra["kotlin_version"] = "2.0.21"
    extra["compose_compiler_version"] = "1.5.15"
    extra["hilt_version"] = "2.51.1"
    extra["room_version"] = "2.6.1"
    extra["onnx_version"] = "1.17.0"
    extra["media3_version"] = "1.4.1"
    extra["compose_bom_version"] = "2024.09.00"
    extra["work_version"] = "2.9.1"
    extra["navigation_version"] = "2.8.3"
    extra["lifecycle_version"] = "2.8.6"
    extra["coroutines_version"] = "1.9.0"
    extra["serialization_version"] = "1.7.3"
}

plugins {
    id("com.android.application") version "8.7.1" apply false
    id("com.android.library") version "8.7.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
}
