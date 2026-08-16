plugins {
    id("com.android.library")
}

android {
    namespace = "com.shieldcore.security.scanner_native"
    compileSdk = 35

    defaultConfig {
        minSdk = 26

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17 -O3")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("javax.inject:javax.inject:1")
}
