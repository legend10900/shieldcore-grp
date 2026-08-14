// Top-level build file where you can add configuration options common to all sub-projects/modules.
focus on other things and features u can fix aplugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.android.library") version "9.3.1" apply false
    id("com.android.legacy-kapt") version "9.3.1" apply false
    id("com.google.devtools.ksp") version "2.4.10-1.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
