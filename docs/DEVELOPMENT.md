# 开发指南

## 环境要求

### 必需软件

- **Android Studio**: Hedgehog (2023.1.1) 或更高版本
- **JDK**: 17
- **Android SDK**: 34
- **Gradle**: 8.4

### 推荐配置

- **内存**: 16GB RAM 或更多
- **磁盘空间**: 10GB 可用空间
- **操作系统**: Windows 10/11, macOS 12+, Linux

## 项目设置

### 1. 克隆项目

```bash
git clone <repository-url>
cd sms-cleaner
```

### 2. 打开项目

使用 Android Studio 打开项目根目录。

### 3. 同步依赖

Android Studio 会自动同步 Gradle 依赖。如果没有，点击 **File → Sync Project with Gradle Files**。

### 4. 配置 SDK

确保安装了以下 SDK：
- Android SDK Platform 34
- Android SDK Build-Tools 34.0.0
- Android SDK Platform-Tools

## 构建变体

### Debug 构建

```bash
./gradlew assembleDebug
```

- 包含调试信息
- 未混淆
- 使用调试签名

### Release 构建

```bash
./gradlew assembleRelease
```

- 未混淆（isMinifyEnabled = false）
- 需要配置签名

## 代码规范

### Kotlin 代码风格

- 使用 Kotlin 官方代码风格
- 使用 4 空格缩进
- 使用单引号字符串
- 使用尾随逗号

### 命名规范

- **类名**: PascalCase（如 `SmsViewModel`）
- **函数名**: camelCase（如 `loadMessages`）
- **变量名**: camelCase（如 `currentPage`）
- **常量名**: UPPER_SNAKE_CASE（如 `MAX_HISTORY_SIZE`）
- **包名**: 小写字母（如 `top.yuameshi.sms.cleaner`）

### Compose 规范

- 使用 `@Composable` 注解
- 使用 `remember` 保存状态
- 使用 `StateFlow` 管理状态
- 使用 `Modifier` 链式调用

### 注释规范

- 公开 API 必须添加 KDoc 注释
- 复杂逻辑必须添加注释
- 禁止添加无意义的注释

## 测试

### 单元测试

```bash
./gradlew test
```

### 仪器化测试

```bash
./gradlew connectedAndroidTest
```

### 测试覆盖率

```bash
./gradlew jacocoTestReport
```

## 调试

### 日志

使用 `android.util.Log` 记录日志：

```kotlin
Log.d(TAG, "Debug message")
Log.e(TAG, "Error message", exception)
```

### 断点调试

1. 在 Android Studio 中设置断点
2. 以调试模式运行应用
3. 使用调试窗口查看变量值

### 网络调试

使用 Android Studio 的 Network Inspector 查看网络请求。

## 常见问题

### 1. Gradle 同步失败

**问题**：Gradle 同步失败，提示依赖下载失败

**解决**：
- 检查网络连接
- 清理 Gradle 缓存：`./gradlew clean`
- 重新同步项目

### 2. 编译错误

**问题**：编译错误，提示找不到类

**解决**：
- 检查导入语句
- 同步 Gradle 依赖
- 清理并重新构建：`./gradlew clean assembleDebug`

### 3. 运行时崩溃

**问题**：应用运行时崩溃

**解决**：
- 查看 Logcat 日志
- 检查权限是否授予
- 检查设备是否支持所需 API
