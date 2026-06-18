# Android TDD and Testing Patterns Guide

## Overview

This guide provides comprehensive testing patterns for Android apps with:
- Jetpack Compose UI testing
- ViewModel unit testing  
- Repository testing with mocked ContentResolver
- UseCase testing patterns
- Hilt testing setup
- Integration test patterns for SMS operations

## Quick Reference

### Dependencies (build.gradle - app level)

`kotlin
dependencies {
    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.truth:truth:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Compose testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
    
    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.48")
}
`

### Test Organization

`
app/
├── src/
│   ├── main/
│   ├── test/           # Unit tests (fast, no device)
│   │   └── java/com/example/
│   │       ├── viewmodel/
│   │       ├── repository/
│   │       ├── usecase/
│   │       └── model/
│   └── androidTest/    # Instrumented tests (requires device)
│       └── java/com/example/
│           ├── ui/
│           ├── integration/
│           └── hilt/
`

### Test Naming Convention

`kotlin
@Test
fun 	est [action] [expected result]() { }
`

## Files in This Guide

1. [Compose UI Testing](01-compose-testing.md)
2. [ViewModel Testing](02-viewmodel-testing.md)
3. [Repository Testing](03-repository-testing.md)
4. [UseCase Testing](04-usecase-testing.md)
5. [Hilt Testing Setup](05-hilt-testing.md)
6. [Integration Testing](06-integration-testing.md)
7. [Best Practices](07-best-practices.md)
