# Fix Gradle Sync Errors for AGP 9.3.1

This plan addresses the deprecation of the legacy `android` block and the unresolved `kotlinOptions` in `app/build.gradle.kts` due to the move to AGP 9.0+ and built-in Kotlin support.

## Proposed Changes

### [Component Name]

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/Admin/guard3/app/build.gradle.kts)
- Migrate `kotlinOptions` to the new `kotlin { compilerOptions { ... } }` block at the top level.
- Remove the deprecated `kotlinOptions` block from inside `android { ... }`.
- Since `jvmTarget` now defaults to `android.compileOptions.targetCompatibility` in AGP 9.0+, we can omit it if it matches (both are 17).

## Verification Plan

### Automated Tests
- Run Gradle sync to verify that the errors are resolved.
- Run `./gradlew assembleDebug` to ensure compilation still works.
