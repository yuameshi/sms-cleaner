buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.10")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.59.2")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.9")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
