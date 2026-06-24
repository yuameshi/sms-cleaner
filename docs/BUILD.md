# 构建配置说明

本文档详细说明 SMS Cleaner 项目的构建环境、依赖项、构建变体、签名配置、ProGuard 规则和构建优化设置。

## 构建环境

### 工具链版本

| 工具                           | 版本   |
| ------------------------------ | ------ |
| Android Gradle Plugin (AGP)    | 9.1.1  |
| Kotlin Compose Compiler Plugin | 2.2.10 |
| KSP (Kotlin Symbol Processing) | 2.3.9  |
| Hilt Android Gradle Plugin     | 2.59.2 |
| JDK                            | 17     |

### SDK 版本

| 配置项     | 值               |
| ---------- | ---------------- |
| compileSdk | 37               |
| minSdk     | 23 (Android 6.0) |
| targetSdk  | 37               |

### Gradle 配置

在 `gradle.properties` 中定义的全局配置：

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- `org.gradle.jvmargs`: 分配 2048MB 堆内存给 Gradle 守护进程
- `android.useAndroidX`: 启用 AndroidX 支持库
- `kotlin.code.style`: 使用官方 Kotlin 代码风格
- `android.nonTransitiveRClass`: 启用非传递性 R 类，减少编译时间

### 仓库配置

项目在 `settings.gradle.kts` 和 `build.gradle.kts` 中配置了以下仓库：

- `google()`: Google Maven 仓库
- `mavenCentral()`: Maven Central 仓库
- `gradlePluginPortal()`: Gradle 插件门户

## 依赖项

### 核心依赖

| 依赖项                                     | 版本   | 用途                  |
| ------------------------------------------ | ------ | --------------------- |
| `androidx.core:core-ktx`                   | 1.17.0 | Android 核心 KTX 扩展 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.11.0 | 生命周期感知组件      |
| `androidx.activity:activity-compose`       | 1.13.0 | Activity Compose 集成 |

### Jetpack Compose

使用 BOM (Bill of Materials) 统一管理 Compose 版本：

```kotlin
implementation(platform("androidx.compose:compose-bom:2026.06.00"))
```

| 依赖项                                              | 版本管理 | 用途               |
| --------------------------------------------------- | -------- | ------------------ |
| `androidx.compose.ui:ui`                            | BOM 管理 | Compose UI 核心    |
| `androidx.compose.ui:ui-graphics`                   | BOM 管理 | 图形渲染           |
| `androidx.compose.material3:material3`              | BOM 管理 | Material3 设计系统 |
| `androidx.compose.material:material-icons-extended` | BOM 管理 | 扩展图标库         |

### ViewModel 与生命周期

| 依赖项                                           | 版本   | 用途                   |
| ------------------------------------------------ | ------ | ---------------------- |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.11.0 | ViewModel Compose 集成 |
| `androidx.lifecycle:lifecycle-runtime-compose`   | 2.11.0 | 生命周期 Compose 集成  |

### 导航

| 依赖项                                   | 版本  | 用途             |
| ---------------------------------------- | ----- | ---------------- |
| `androidx.navigation:navigation-compose` | 2.9.8 | Compose 导航框架 |

### 依赖注入 (Hilt)

| 依赖项                                  | 版本   | 用途                   |
| --------------------------------------- | ------ | ---------------------- |
| `com.google.dagger:hilt-android`        | 2.59.2 | Hilt 运行时库          |
| `com.google.dagger:hilt-compiler`       | 2.59.2 | Hilt 注解处理器 (KSP)  |
| `androidx.hilt:hilt-navigation-compose` | 1.3.0  | Hilt 导航 Compose 集成 |

注意：项目使用 KSP 而非 KAPT 进行注解处理，编译速度更快。

### 协程

| 依赖项                                             | 版本   | 用途                     |
| -------------------------------------------------- | ------ | ------------------------ |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Kotlin 协程 Android 支持 |

### 测试依赖

| 依赖项                           | 版本     | 用途                           |
| -------------------------------- | -------- | ------------------------------ |
| `junit:junit`                    | 4.13.2   | 单元测试框架                   |
| `androidx.compose.ui:ui-tooling` | BOM 管理 | Compose UI 调试工具 (仅 debug) |

## 构建变体

项目定义了两种构建类型：

### Debug 构建

- 未启用代码混淆
- 包含调试工具 (`ui-tooling`)
- 适用于开发和调试

### Release 构建

```kotlin
release {
    isMinifyEnabled = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
    signingConfig = signingConfigs.getByName("release")
}
```

- 启用代码混淆 (`isMinifyEnabled = true`)
- 使用 Android 默认 ProGuard 规则 + 项目自定义规则
- 使用 release 签名配置

## 签名配置

### Release 签名

项目从 `keystore.properties` 文件读取签名配置：

```kotlin
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
keystoreProperties.load(FileInputStream(keystorePropertiesFile))

signingConfigs {
    create("release") {
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
    }
}
```

### keystore.properties 文件格式

在项目根目录创建 `keystore.properties` 文件：

```properties
keyAlias=your_key_alias
keyPassword=your_key_password
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
```

注意：`keystore.properties` 文件已添加到 `.gitignore`，不会提交到版本控制系统。

## ProGuard 配置

项目使用两层 ProGuard 规则：

1. **Android 默认规则**: `proguard-android-optimize.txt` (AGP 内置)
2. **项目自定义规则**: `app/proguard-rules.pro`

### 自定义规则内容

```proguard
# Keep Hilt generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
```

规则说明：

- 保留 Hilt 生成的依赖注入代码，防止被混淆
- 保留 Compose 框架类，确保 UI 渲染正常

### 可选调试配置

如需在崩溃日志中保留行号信息，取消注释以下规则：

```proguard
-keepattributes SourceFile,LineNumberTable
-renameSourcefileattribute SourceFile
```

## 构建优化

### JVM 内存优化

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

分配 2GB 堆内存，适合中大型项目。如遇内存不足，可调整为 `-Xmx4096m`。

### 非传递性 R 类

```properties
android.nonTransitiveRClass=true
```

启用后，每个模块只能访问自己定义的资源，减少不必要的重新编译。

### KSP 替代 KAPT

项目使用 KSP 进行注解处理（Hilt），相比 KAPT 有以下优势：

- 编译速度提升约 2 倍
- 更低的内存占用
- 更好的增量编译支持

### Compose BOM

使用 BOM 统一管理 Compose 版本，避免版本冲突：

```kotlin
implementation(platform("androidx.compose:compose-bom:2026.06.00"))
```

只需更新 BOM 版本，所有 Compose 依赖自动同步。

### 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理构建缓存
./gradlew clean

# 运行单元测试
./gradlew test

# 查看依赖树
./gradlew :app:dependencies
```

## 项目配置

- **项目名称**: SMS-Cleaner
- **应用 ID**: `top.yuameshi.sms.cleaner`
- **命名空间**: `top.yuameshi.sms.cleaner`
- **版本号**: 1.0.0 (versionCode: 1)
- **最低支持版本**: Android 6.0 (API 23)

