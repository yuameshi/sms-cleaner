# 开发指南

本文档介绍 SMS Cleaner 项目的开发环境配置、构建流程和代码规范。

## 环境要求

### 必需工具

| 工具           | 版本                   | 说明                             |
| -------------- | ---------------------- | -------------------------------- |
| Android Studio | 最新版（支持 AGP 9.x） | 推荐使用 Ladybug 或更新版本      |
| JDK            | 17                     | Android Studio 内置即可          |
| Android SDK    | API 37 (Android 16)    | compileSdk 版本                  |
| Gradle         | 9.4.1                  | 项目已包含 wrapper，无需单独安装 |

### SDK 版本

```
compileSdk = 37
minSdk = 23 (Android 6.0)
targetSdk = 37 (Android 16)
```

### 权限要求

应用运行时需要以下权限：

- `READ_SMS` - 读取短信
- `READ_CONTACTS` - 读取联系人（用于显示联系人名称）
- `READ_PHONE_STATE` - 读取手机状态（用于 SIM 卡信息）
- `POST_NOTIFICATIONS` - 发送通知（Android 13+）

## 项目设置

### 1. 克隆仓库

```bash
git clone https://github.com/yuameshi/sms-cleaner
cd sms-cleaner
```

### 2. 配置签名

Release 构建需要签名配置。在项目根目录创建 `keystore.properties` 文件：

```properties
keyAlias=your_key_alias
keyPassword=your_key_password
storeFile=path/to/your/keystore.jks
storePassword=your_store_password
```

> 注意：`keystore.properties` 已在 `.gitignore` 中，不会被提交到版本控制。

### 3. 使用 Android Studio 打开项目

1. 启动 Android Studio
2. 选择 `File > Open`
3. 选择项目根目录
4. 等待 Gradle 同步完成

### 4. 命令行构建

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 清理构建产物
./gradlew clean
```

Windows 环境使用 `gradlew.bat`：

```cmd
gradlew.bat assembleDebug
```

## 构建变体

### Debug

- 未启用代码混淆
- 包含调试工具（Compose UI Tooling）
- 使用 debug 签名
- 适合日常开发和测试

### Release

- 启用代码混淆（`isMinifyEnabled = true`）
- 使用 ProGuard 优化
- 使用 `keystore.properties` 中配置的签名
- 适合发布到应用商店

### ProGuard 配置

Release 构建使用以下 ProGuard 规则：

- 保留 Hilt 生成的代码
- 保留 Compose 相关类
- 规则文件：`app/proguard-rules.pro`

## 代码规范

### 技术栈

| 类别     | 技术                        |
| -------- | --------------------------- |
| 语言     | Kotlin                      |
| UI 框架  | Jetpack Compose + Material3 |
| 架构模式 | MVVM + Clean Architecture   |
| 依赖注入 | Hilt (使用 KSP，非 KAPT)    |
| 异步处理 | Kotlin Coroutines + Flow    |
| 导航     | Navigation Compose          |

### 命名规范

- **包名**：`top.yuameshi.sms.cleaner`
- **类名**：PascalCase（如 `SmsViewModel`、`GetSmsUseCase`）
- **函数名**：camelCase（如 `loadMessages`、`checkPermissions`）
- **变量名**：camelCase（如 `currentPage`、`allMessages`）
- **常量**：UPPER_SNAKE_CASE（如 `REQUIRED_PERMISSIONS`）
- **Compose 组件**：PascalCase（如 `MainScreen`、`SmsListItem`）

### 代码风格

- 使用 Kotlin 官方代码风格（`kotlin.code.style=official`）
- 缩进：4 个空格
- 公共 API 必须添加 KDoc 注释
- 遵循 Android 官方编码规范

### 架构分层

```
UI 层 (ui/)
  ↓ 观察 StateFlow
ViewModel 层 (ui/screen/SmsViewModel.kt)
  ↓ 调用 UseCase
Domain 层 (domain/usecase/)
  ↓ 依赖 Repository 接口
Data 层 (data/repository/, data/datasource/)
```

### 依赖规则

- UI 层通过 Hilt 注入 ViewModel
- ViewModel 调用 UseCase 处理业务逻辑
- UseCase 依赖 Repository 接口（依赖反转）
- Data 层实现 Repository 接口，封装数据源

### 状态管理

使用 `SmsScreenUiState` 统一管理 UI 状态：

```kotlin
data class SmsScreenUiState(
    val smsState: SmsUiState = SmsUiState.Loading,
    val filterState: FilterState = FilterState(),
    val selectionState: SelectionState = SelectionState(),
    val operationState: OperationState = OperationState.Idle,
    val hasPermissions: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    // ... 其他状态字段
)
```

ViewModel 通过 `StateFlow` 暴露状态，UI 层使用 `collectAsStateWithLifecycle()` 收集。

## 测试

### 运行测试

```bash
# 运行所有单元测试
./gradlew test

# 运行特定模块测试
./gradlew :app:test
```

### 测试目录

```
app/src/test/java/        # 本地单元测试
app/src/androidTest/java/ # 设备测试（如有）
```

### 测试规范

- ViewModel 和 UseCase 应可独立单元测试
- 不依赖 Android 框架类的逻辑优先编写测试
- 使用 JUnit 4 作为测试框架

## 调试

### Compose 调试

Debug 构建包含 `ui-tooling` 依赖，支持：

- Layout Inspector
- Compose Preview
- 实时编辑

### 日志查看

使用 Android Studio 的 Logcat 窗口查看应用日志。

### 常用调试技巧

1. **状态检查**：在 ViewModel 中添加日志，观察状态变化
2. **权限调试**：使用 `PermissionUtils` 工具类检查权限状态
3. **数据库调试**：通过 Device File Explorer 查看应用数据

## 常见问题

### Gradle 同步失败

**问题**：Android Studio 打开项目后 Gradle 同步失败。

**解决**：

1. 检查网络连接，确保可以访问 Google Maven 和 Maven Central
2. 尝试 `File > Invalidate Caches / Restart`
3. 手动运行 `./gradlew --refresh-dependencies`

### SDK 版本不匹配

**问题**：编译提示 SDK 版本不支持。

**解决**：

1. 打开 SDK Manager（`Tools > SDK Manager`）
2. 安装 Android SDK Platform 37
3. 确认 JDK 版本为 17

### 签名配置错误

**问题**：Release 构建失败，提示签名配置问题。

**解决**：

1. 确认 `keystore.properties` 文件存在且格式正确
2. 检查 keystore 文件路径是否正确
3. 验证密码和别名是否匹配

### Hilt 注入失败

**问题**：运行时崩溃，提示依赖注入失败。

**解决**：

1. 确认 Application 类添加了 `@HiltAndroidApp` 注解
2. 确认 Activity/Fragment 添加了 `@AndroidEntryPoint` 注解
3. 清理并重新构建：`./gradlew clean assembleDebug`

### 权限问题

**问题**：应用无法读取短信或联系人。

**解决**：

1. 检查 AndroidManifest.xml 中的权限声明
2. 确认运行时权限已授予
3. Android 13+ 需要单独请求 `POST_NOTIFICATIONS` 权限

### 默认短信应用

**问题**：删除或导入短信功能不可用。

**解决**：

应用需要设置为默认短信应用才能执行删除和导入操作。系统会自动提示用户进行设置。

## 项目结构

```
app/src/main/java/top/yuameshi/sms/cleaner/
├── data/                    # 数据层
│   ├── datasource/          # 数据源（ContentResolver 封装）
│   ├── manager/             # 操作管理器
│   ├── model/               # 数据模型
│   └── repository/          # 仓库层（接口 + 实现）
├── di/                      # 依赖注入（Hilt 模块）
├── domain/usecase/          # 领域层（用例）
├── receiver/                # 广播接收器
├── service/                 # 服务
├── ui/                      # UI 层
│   ├── component/           # 可复用组件
│   ├── navigation/          # 导航配置
│   ├── screen/              # 屏幕级组件
│   └── theme/               # 主题配置
└── util/                    # 工具类
```

## 相关文档

- [架构说明](ARCHITECTURE.md) - 详细的架构设计文档
- [功能说明](FEATURES.md) - 功能特性介绍
- [API 文档](API.md) - 接口说明
- [构建配置](BUILD.md) - 构建配置详解

