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
