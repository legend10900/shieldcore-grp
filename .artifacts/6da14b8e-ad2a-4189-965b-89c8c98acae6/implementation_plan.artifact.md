# Migrate to AGP 9.0 Built-in Kotlin

The project is using AGP 9.3.1 but is still trying to apply the `org.jetbrains.kotlin.android` plugin, which is no longer required and causes sync errors in AGP 9.0+. Additionally, `kotlin-kapt` needs to be replaced with or complemented by `com.android.legacy-kapt` to work with built-in Kotlin, and `kotlinOptions` must be migrated to `kotlin.compilerOptions`.

## User Review Required

> [!IMPORTANT]
> The migration to AGP 9.0 built-in Kotlin involves removing the `kotlin-android` plugin. This might affect custom Gradle tasks that rely on Kotlin plugin extensions.

## Proposed Changes

### Build Configuration

#### [MODIFY] [gradle.properties](file:///C:/Users/Admin/guard3/gradle.properties)
- Ensure `android.builtInKotlin` and `android.newDsl` are not explicitly set to `false`.

#### [MODIFY] [root build.gradle.kts](file:///C:/Users/Admin/guard3/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")` if present.
- Ensure `id("com.android.legacy-kapt")` is declared.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Admin/guard3/app/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.
- Add `id("com.android.legacy-kapt")` and `id("kotlin-kapt")`.
- Migrate `kotlinOptions` to `kotlin { compilerOptions { ... } }`.

#### [MODIFY] [core/build.gradle.kts](file:///C:/Users/Admin/guard3/core/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.
- Migrate `kotlinOptions`.

#### [MODIFY] [data/build.gradle.kts](file:///C:/Users/Admin/guard3/data/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.
- Add `id("com.android.legacy-kapt")` and `id("kotlin-kapt")`.
- Migrate `kotlinOptions`.

#### [MODIFY] [domain/build.gradle.kts](file:///C:/Users/Admin/guard3/domain/build.gradle.kts)
- Remove `id("org.jetbrains.kotlin.android")`.
- Migrate `kotlinOptions`.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to verify compilation.
- Run `./gradlew help` to verify Gradle sync/configuration.
