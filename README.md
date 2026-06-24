# SMS Cleaner - Android 短信管理工具

一款高效的 Android 短信管理工具，支持多维度筛选、批量删除和 CSV 格式导出/导入功能。

## 功能特性

- 多维度筛选（关键词、号码、日期范围、已读状态、锁定状态、消息类型、SIM 卡、联系人）
- 批量操作（多选、全选、反选、批量删除、滑动删除）
- CSV 导出/导入（UTF-8 BOM，RFC 4180）
- 默认短信应用管理
- 筛选历史记录
- 深色模式支持（Material You 动态颜色）

## 系统要求

- Android 6.0 (API 23) 及以上版本

## 快速开始

```bash
git clone <repository-url>
cd SMS-Cleaner-Workspace
./gradlew assembleDebug
```

> 构建前需配置签名：在项目根目录创建 `keystore.properties` 文件，参考 [构建配置](docs/BUILD.md)。

## 文档

详细文档请参阅 [docs/](docs/) 目录：

- [架构说明](docs/ARCHITECTURE.md) - MVVM + Clean Architecture 分层设计
- [开发指南](docs/DEVELOPMENT.md) - 环境搭建与代码规范
- [功能说明](docs/FEATURES.md) - 完整功能清单与实现细节
- [API 文档](docs/API.md) - 数据模型、用例、仓库接口
- [构建配置](docs/BUILD.md) - 依赖版本、签名、ProGuard

## 技术栈

| 类别     | 技术                                   |
| -------- | -------------------------------------- |
| 语言     | Kotlin 2.2.10 (Compose Compiler)       |
| UI       | Jetpack Compose + Material3            |
| 架构     | MVVM + Clean Architecture              |
| 依赖注入 | Hilt 2.59.2 (KSP)                      |
| 异步     | Coroutines 1.11.0                      |
| 导航     | Navigation Compose 2.9.8               |
| SDK      | compileSdk 37, minSdk 23, targetSdk 37 |
| 构建     | AGP 9.1.1, Gradle 9.4.1, JDK 17        |

## 项目结构

```
app/src/main/java/top/yuameshi/sms/cleaner/
├── data/                           # 数据层
│   ├── datasource/                 # 数据源
│   │   ├── SmsDataSource.kt        # 短信数据源，封装 ContentResolver
│   │   └── SmsSelectionBuilder.kt  # SQL 筛选条件构建器
│   ├── model/                      # 数据模型
│   │   ├── FilterState.kt          # 筛选状态模型
│   │   ├── SelectionState.kt       # 选择状态模型
│   │   ├── SimCardInfo.kt          # SIM 卡信息模型
│   │   └── SmsMessage.kt           # 短信数据模型
│   └── repository/                 # 仓库层（接口 + 实现）
│       ├── FilterHistoryRepository.kt      # 筛选历史仓库接口
│       ├── FilterHistoryRepositoryImpl.kt  # 筛选历史仓库实现
│       ├── SmsRepository.kt                # 短信仓库接口
│       └── SmsRepositoryImpl.kt            # 短信仓库实现
├── di/                             # 依赖注入
│   ├── AppModule.kt                # Hilt 模块 - Context 依赖
│   └── DataModule.kt               # Hilt 模块 - Repository 绑定
├── domain/                         # 领域层
│   └── usecase/                    # 用例
│       ├── CheckDefaultSmsUseCase.kt   # 检查默认短信 App
│       ├── CheckPermissionsUseCase.kt  # 检查权限状态
│       ├── DeleteSmsUseCase.kt         # 删除短信用例
│       ├── ExportSmsUseCase.kt         # 导出短信用例
│       ├── GetSmsUseCase.kt            # 获取短信用例
│       ├── ImportSmsUseCase.kt         # 导入短信用例
│       └── LoadSimCardsUseCase.kt      # 加载 SIM 卡用例
├── receiver/                       # 广播接收器
│   ├── ComposeSmsActivity.kt      # 发送短信 Activity
│   ├── MmsReceiver.kt             # MMS 接收器
│   ├── SmsReceiver.kt             # SMS 接收器
│   └── WapPushReceiver.kt         # WAP Push 接收器
├── service/
│   └── RespondService.kt          # 短信回复服务
├── ui/                             # UI 层
│   ├── component/                  # 可复用 UI 组件
│   │   ├── DatePickerDialog.kt     # 日期选择对话框
│   │   ├── DeleteConfirmDialog.kt  # 删除确认对话框
│   │   ├── DrawerFilterPanel.kt    # 侧边栏筛选面板
│   │   ├── ExportDialog.kt         # 导出对话框
│   │   ├── ImportDialog.kt         # 导入对话框
│   │   └── SmsListItem.kt          # 短信列表项组件
│   ├── navigation/
│   │   └── NavGraph.kt             # 导航图
│   ├── screen/                     # 屏幕级组件
│   │   ├── MainScreen.kt           # 主屏幕
│   │   ├── MainScreenDialogs.kt    # 主屏幕对话框集合
│   │   ├── MainScreenTopBar.kt     # 主屏幕顶部栏
│   │   ├── MultiSelectBottomBar.kt # 多选操作栏
│   │   ├── PermissionDeniedContent.kt  # 权限拒绝页面
│   │   ├── SelectionManager.kt     # 选择状态管理器
│   │   ├── SmsScreenUiState.kt     # 统一 UI 状态数据类
│   │   ├── SmsViewModel.kt         # 主视图模型
│   │   └── UiState.kt              # UI 状态密封类
│   └── theme/                      # 主题
│       ├── Color.kt                # 颜色定义
│       ├── Theme.kt                # 主题配置
│       └── Type.kt                 # 字体配置
├── util/                           # 工具类
│   ├── ActivityUtils.kt            # Activity 工具函数
│   ├── CsvParser.kt                # CSV 解析器（RFC 4180）
│   ├── DefaultSmsManager.kt        # 默认短信 App 管理
│   ├── DeviceUtils.kt              # 设备检测工具
│   ├── PermissionUtils.kt          # 权限工具类
│   └── SmsTextUtils.kt             # 短信文本工具函数
├── MainActivity.kt                 # 应用入口
└── SmsCleanerApp.kt                # Application 类
```

## 架构概览

本项目严格遵循 MVVM + Clean Architecture 原则：

```
UI Layer (Compose)  →  ViewModel Layer  →  Domain Layer (UseCases)  →  Data Layer (Repository + DataSource)
```

### 依赖规则

- **UI 层** -> ViewModel 层（通过 Hilt 注入）
- **ViewModel 层** -> Domain 层（UseCases）+ Repository 接口
- **Domain 层** -> Repository 接口（依赖反转）
- **Data 层** -> 实现 Repository 接口，封装数据源

### 核心设计

- **7 个 UseCase**：GetSms、DeleteSms、ExportSms、ImportSms、CheckDefaultSms、CheckPermissions、LoadSimCards
- **统一状态管理**：`SmsScreenUiState` 整合 12 个 UI 状态字段，MainScreen 仅需 1 个 `collectAsStateWithLifecycle()`
- **零 Android 框架依赖**：`SmsViewModel` 不依赖任何 Android 框架类，Context 依赖下沉到 UseCase 层
- **Repository 接口模式**：通过 Hilt `@Binds` 实现依赖反转，便于单元测试

## 许可证

[MIT License](LICENSE)

# 致谢

感谢小米发起的[Xiaomi MiMo Orbit百万亿 Token 创造者激励计划](https://100t.xiaomimimo.com/)，为本项目提供支持

同时也感谢每一个开源库的作者和贡献者，正是因为有了你们的无私奉献，才让开源社区得以繁荣发展。
