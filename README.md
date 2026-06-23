# SMS Cleaner - Android 短信管理工具

一款高效的 Android 短信管理工具，支持多维度筛选、批量删除和 CSV 格式导出/导入功能。

## 功能特性

- 多维度筛选（关键词、号码、正则表达式、日期范围、已读状态、锁定状态、消息类型、SIM 卡）
- 批量操作（多选、全选、批量删除、滑动删除）
- CSV 导出/导入（UTF-8 BOM，RFC 4180）
- 默认短信应用管理
- 筛选历史记录
- 深色模式支持

## 系统要求

- Android 6.0 (API 23) 及以上版本

## 快速开始

```bash
git clone <repository-url>
cd sms-cleaner
./gradlew assembleDebug
```

## 文档

详细文档请参阅 [docs/](docs/) 目录：

- [架构说明](docs/ARCHITECTURE.md)
- [开发指南](docs/DEVELOPMENT.md)
- [功能说明](docs/FEATURES.md)
- [API 文档](docs/API.md)
- [构建配置](docs/BUILD.md)

## 技术栈

- Kotlin
- Jetpack Compose + Material3
- MVVM + Clean Architecture
- Hilt (依赖注入)
- Coroutines

## 项目结构

```
app/src/main/java/top/yuameshi/sms/cleaner/
├── data/                           # 数据层
│   ├── datasource/                 # 数据源
│   │   ├── SmsDataSource.kt        # 短信数据源，封装 ContentResolver
│   │   └── SmsSelectionBuilder.kt  # SQL 筛选条件构建器
│   ├── manager/
│   │   └── SmsOperationManager.kt  # 统一短信数据库操作管理器
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

## MVVM 架构合规性

本项目严格遵循 MVVM + Clean Architecture 原则：

### 依赖规则
- **UI 层** → ViewModel 层（通过 Hilt 注入）
- **ViewModel 层** → Domain 层（UseCases）+ Repository 接口
- **Domain 层** → Repository 接口（依赖反转）
- **Data 层** → 实现 Repository 接口，封装数据源

### UseCase 提取
所有业务逻辑已提取为独立的 UseCase：
- `GetSmsUseCase` - 获取短信（分页）
- `DeleteSmsUseCase` - 删除短信（单条/批量/按条件）
- `ExportSmsUseCase` - 导出短信为 CSV
- `ImportSmsUseCase` - 从 CSV 导入短信
- `CheckDefaultSmsUseCase` - 检查默认短信 App 状态
- `CheckPermissionsUseCase` - 检查权限状态
- `LoadSimCardsUseCase` - 加载 SIM 卡信息

### 状态管理
- 使用 `SmsScreenUiState` 统一管理 12 个 UI 状态字段
- 替代了原来分散的 12 个独立 `StateFlow`
- MainScreen 从 10 个 `collectAsStateWithLifecycle()` 调用减少到 1 个

### 零 Android 框架依赖
- `SmsViewModel` 不依赖任何 Android 框架类
- `Context` 依赖已下沉到 UseCase 层（`CheckPermissionsUseCase`、`ExportSmsUseCase`）
- 所有 UseCase 均可独立单元测试
