# 构建配置

## 构建环境

### Gradle 版本

- **Gradle**: 8.11.1
- **Android Gradle Plugin**: 8.10.1
- **Kotlin**: 1.9.20

### SDK 版本

- **compileSdk**: 34
- **minSdk**: 23
- **targetSdk**: 34

### JDK 版本

- **JDK**: 17

## 依赖项

### 核心依赖

```kotlin
// AndroidX Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
implementation("androidx.activity:activity-compose:1.8.1")
```

### Jetpack Compose

```kotlin
// Compose BOM
implementation(platform("androidx.compose:compose-bom:2023.10.01"))

// Compose UI
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")

// Material3
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
```

### ViewModel

```kotlin
// ViewModel Compose
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
```

### Navigation

```kotlin
// Navigation Compose
implementation("androidx.navigation:navigation-compose:2.7.5")
```

### Hilt

```kotlin
// Hilt Android
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")

// Hilt Navigation Compose
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
```

### Coroutines

```kotlin
// Coroutines Android
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

### 测试依赖

```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")

// Android Testing
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

// Compose Testing
androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

## 构建变体

### Debug 构建

```bash
./gradlew assembleDebug
```

**配置**：
- 包含调试信息
- 未混淆
- 使用调试签名

### Release 构建

```bash
./gradlew assembleRelease
```

**配置**：
- 未混淆（isMinifyEnabled = false）
- 需要配置签名

## 签名配置

### 调试签名

Android Studio 自动生成调试签名，位于 `~/.android/debug.keystore`。

### 发布签名

在 `app/build.gradle.kts` 中配置：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = "your-store-password"
            keyAlias = "your-key-alias"
            keyPassword = "your-key-password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## ProGuard 配置

### 当前配置

```kotlin
buildTypes {
    release {
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### 推荐配置

如果启用混淆，添加以下规则：

```proguard
# 保留 Hilt 注入
-keepattributes *Annotation*
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# 保留数据类
-keep class top.yuameshi.sms.cleaner.data.model.** { *; }

# 保留 Compose
-keep class androidx.compose.** { *; }
```

## 构建优化

### Gradle 配置

在 `gradle.properties` 中添加：

```properties
# 启用构建缓存
org.gradle.caching=true

# 启用并行构建
org.gradle.parallel=true

# 增加 JVM 内存
org.gradle.jvmargs=-Xmx4096m

# 启用配置缓存
org.gradle.configuration-cache=true
```

### 依赖优化

- 使用 BOM 管理 Compose 版本
- 避免重复依赖
- 使用 `implementation` 而非 `api`

## CI/CD 配置

### GitHub Actions 示例

```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
```

## 常见问题

### 1. 构建失败

**问题**：构建失败，提示依赖下载失败

**解决**：
- 检查网络连接
- 清理 Gradle 缓存：`./gradlew clean`
- 重新同步项目

### 2. 内存不足

**问题**：构建时内存不足

**解决**：
- 增加 JVM 内存：`org.gradle.jvmargs=-Xmx4096m`
- 关闭其他应用程序
- 增加系统内存

### 3. 版本冲突

**问题**：依赖版本冲突

**解决**：
- 使用 BOM 管理版本
- 排除冲突依赖
- 统一版本号
