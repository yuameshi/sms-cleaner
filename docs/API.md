# SMS Cleaner API 文档

本文档描述 SMS Cleaner 应用的核心 API，包括数据模型、用例、仓库、数据源和工具类。

## 数据模型

### SmsMessage

短信消息数据模型，表示一条 SMS 消息。

```kotlin
data class SmsMessage(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val locked: Boolean,
    val subId: Int,
    val contactName: String? = null
) {
    companion object {
        const val TYPE_INBOX = 1      // 收件箱
        const val TYPE_SENT = 2       // 已发送
        const val TYPE_DRAFT = 3      // 草稿
        const val TYPE_OUTBOX = 4     // 发件箱
        const val TYPE_FAILED = 5     // 发送失败
        const val TYPE_QUEUED = 6     // 待发送

        fun getTypeName(type: Int): String
    }
}
```

**字段说明：**

- `id`: 消息唯一标识符
- `address`: 发送方/接收方号码
- `body`: 消息内容
- `date`: 消息时间戳（毫秒）
- `type`: 消息类型（使用 TYPE\_\* 常量）
- `read`: 是否已读
- `locked`: 是否锁定
- `subId`: SIM 卡订阅 ID
- `contactName`: 联系人姓名（可选，通过号码查询获得）

### FilterState

筛选条件状态模型，用于多维度筛选短信。

```kotlin
data class FilterState(
    val keyword: String = "",
    val number: String = "",
    val dateRange: DateRange = DateRange.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val readStatus: ReadStatus = ReadStatus.ALL,
    val lockStatus: LockStatus = LockStatus.ALL,
    val messageType: MessageType = MessageType.ALL,
    val simSubscriptionId: Int? = null,
    val contactId: Long? = null
) {
    enum class DateRange {
        ALL, TODAY, LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS, CUSTOM
    }

    enum class ReadStatus {
        ALL, READ, UNREAD
    }

    enum class LockStatus {
        ALL, LOCKED, UNLOCKED
    }

    enum class MessageType {
        ALL, INBOX, SENT, DRAFT, OUTBOX
    }

    fun hasFilters(): Boolean
}
```

**字段说明：**

- `keyword`: 关键词筛选（模糊匹配消息内容）
- `number`: 号码筛选（模糊匹配发送方/接收方）
- `dateRange`: 日期范围枚举
- `customStartDate`: 自定义开始时间戳（CUSTOM 模式使用）
- `customEndDate`: 自定义结束时间戳（CUSTOM 模式使用）
- `readStatus`: 已读状态筛选
- `lockStatus`: 锁定状态筛选
- `messageType`: 消息类型筛选
- `simSubscriptionId`: SIM 卡筛选（null 表示全部）
- `contactId`: 联系人筛选（通过 thread_id）

**方法：**

- `hasFilters()`: 判断是否设置了任何筛选条件

### SelectionState

多选状态模型，管理短信列表的多选操作。

```kotlin
data class SelectionState(
    val isMultiSelectMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectAll: Boolean = false,
    val totalFilteredCount: Int = 0
) {
    val selectedCount: Int
        get() = if (isSelectAll) totalFilteredCount else selectedIds.size

    fun toggleSelection(id: Long): SelectionState
    fun selectAll(totalCount: Int): SelectionState
    fun clearSelection(): SelectionState
    fun deselectAll(): SelectionState
    fun invertSelection(allIds: List<Long>): SelectionState
    fun enterMultiSelectMode(id: Long): SelectionState
    fun exitMultiSelectMode(): SelectionState
    fun isSelected(id: Long): Boolean
}
```

**字段说明：**

- `isMultiSelectMode`: 是否处于多选模式
- `selectedIds`: 已选中的消息 ID 集合
- `isSelectAll`: 是否全选
- `totalFilteredCount`: 筛选后的总消息数（全选时使用）

**属性：**

- `selectedCount`: 计算实际选中数量

**方法：**

- `toggleSelection(id)`: 切换单条消息的选中状态
- `selectAll(totalCount)`: 全选
- `clearSelection()`: 清除选择
- `deselectAll()`: 取消全选
- `invertSelection(allIds)`: 反选
- `enterMultiSelectMode(id)`: 进入多选模式并选中指定消息
- `exitMultiSelectMode()`: 退出多选模式
- `isSelected(id)`: 判断消息是否被选中

### SimCardInfo

SIM 卡信息模型。

```kotlin
data class SimCardInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String,
    val slotIndex: Int
) {
    fun getShortName(): String
    fun getFormattedName(): String
}
```

**字段说明：**

- `subscriptionId`: SIM 卡订阅 ID
- `displayName`: 显示名称
- `carrierName`: 运营商名称
- `phoneNumber`: 手机号码
- `slotIndex`: SIM 卡槽索引

**方法：**

- `getShortName()`: 获取短名称（优先 displayName，其次 carrierName，最后 "SIM {slotIndex+1}"）
- `getFormattedName()`: 获取格式化名称（短名称 + 蒙版手机号，如 "中国移动 \*1234"）

### ImportResult

导入结果数据类，定义在 ImportSmsUseCase 内部。

```kotlin
data class ImportResult(
    val imported: Int,
    val skipped: Int
)
```

**字段说明：**

- `imported`: 成功导入的消息数量
- `skipped`: 跳过的消息数量（重复或无效）

### SimCardsResult

SIM 卡加载结果数据类。

```kotlin
data class SimCardsResult(
    val simCards: List<SimCardInfo>,
    val useShortSimName: Boolean
)
```

**字段说明：**

- `simCards`: SIM 卡列表
- `useShortSimName`: 是否使用短名称（true 表示所有 SIM 卡短名称唯一）

## 用例

### GetSmsUseCase

获取短信列表的用例。

```kotlin
class GetSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    suspend fun getTotalCount(filterState: FilterState): Int
}
```

**方法：**

- `invoke(filterState, page, pageSize)`: 分页获取筛选后的短信列表
- `getTotalCount(filterState)`: 获取筛选后的总消息数

### DeleteSmsUseCase

删除短信的用例。

```kotlin
sealed class DeleteType {
    data object Single : DeleteType()      // 删除单条消息
    data object Multiple : DeleteType()    // 删除多条消息（用户手动选择）
    data object AllByFilter : DeleteType() // 按筛选条件删除全部匹配消息
}

class DeleteSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        deleteType: DeleteType,
        ids: List<Long>? = null,
        filterState: FilterState? = null
    ): Result<Int>
}
```

**参数：**

- `deleteType`: 删除类型
- `ids`: 消息 ID 列表（Single 和 Multiple 类型时必传）
- `filterState`: 筛选条件（AllByFilter 类型时必传）

**返回：**

- `Result<Int>`: 包含实际删除的消息数量

### ExportSmsUseCase

导出短信为 CSV 文件的用例。

```kotlin
class ExportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    // 按筛选条件导出
    suspend operator fun invoke(
        filterState: FilterState,
        exportAll: Boolean,
        uri: Uri,
        onProgress: (exported: Int, total: Int) -> Unit
    ): Result<Int>

    // 按 ID 列表导出（多选已选模式）
    suspend operator fun invoke(
        selectedIds: Set<Long>,
        uri: Uri,
        onProgress: (exported: Int, total: Int) -> Unit
    ): Result<Int>
}
```

**参数：**

- `filterState`: 筛选条件
- `exportAll`: 是否导出全部（忽略 filterState）
- `selectedIds`: 要导出的消息 ID 集合（多选已选模式）
- `uri`: 输出文件 URI
- `onProgress`: 进度回调（已导出数, 总数）

**返回：**

- `Result<Int>`: 包含导出的消息数量

**特性：**

- UTF-8 BOM 编码
- RFC 4180 格式（CRLF 换行）
- CSV 列：ID, 号码, 内容, 时间, 类型, 已读状态, 锁定状态, SIM卡, 发送状态

### ImportSmsUseCase

从 CSV 文件导入短信的用例。

```kotlin
class ImportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    suspend operator fun invoke(
        uri: Uri,
        onProgress: (imported: Int, skipped: Int) -> Unit
    ): Result<ImportResult>
}
```

**参数：**

- `uri`: 输入文件 URI
- `onProgress`: 进度回调（已导入数, 已跳过数）

**返回：**

- `Result<ImportResult>`: 包含导入结果

**特性：**

- 自动检测并跳过 UTF-8 BOM
- 验证 CSV 表头格式
- 重复消息检测（基于 address + body + date）
- 自动使用第一个活跃 SIM 卡的 subscriptionId

### CheckDefaultSmsUseCase

检查当前 App 是否为默认短信 App 的用例。

```kotlin
class CheckDefaultSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean
}
```

**返回：**

- `true`: 当前 App 是默认短信 App
- `false`: 当前 App 不是默认短信 App

### CheckPermissionsUseCase

检查是否已授予所有必需权限的用例。

```kotlin
class CheckPermissionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean
}
```

**返回：**

- `true`: 所有权限已授予
- `false`: 存在未授予的权限

**必需权限：**

- `android.permission.READ_SMS`
- `android.permission.READ_CONTACTS`

### LoadSimCardsUseCase

加载 SIM 卡信息并判断是否使用短名称的用例。

```kotlin
class LoadSimCardsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    operator fun invoke(): SimCardsResult
}
```

**返回：**

- `SimCardsResult`: 包含 SIM 卡列表和短名称唯一性标志

**逻辑：**

- 如果所有 SIM 卡的短名称都不重复，则 `useShortSimName = true`
- 否则 `useShortSimName = false`，UI 应使用 `getFormattedName()`

## 仓库

### SmsRepository

短信数据仓库接口，定义数据访问契约。

```kotlin
interface SmsRepository {
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    suspend fun getTotalCount(filterState: FilterState): Int

    suspend fun deleteMessages(ids: List<Long>): Int

    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean

    suspend fun getSmsMessagesByIds(ids: List<Long>): List<SmsMessage>

    fun getSimCards(): List<SimCardInfo>
}
```

**方法：**

- `getSmsMessages(filterState, page, pageSize)`: 分页获取筛选后的短信
- `getTotalCount(filterState)`: 获取筛选后的总数
- `deleteMessages(ids)`: 按 ID 列表删除消息
- `deleteMessagesByFilter(filterState)`: 按筛选条件删除消息
- `insertMessage(address, body, date, type, read, subId)`: 插入新消息
- `checkDuplicate(address, body, date)`: 检查消息是否重复
- `getSmsMessagesByIds(ids)`: 按 ID 列表批量获取消息（分块查询，每块 100 条）
- `getSimCards()`: 获取 SIM 卡列表

### SmsRepositoryImpl

SmsRepository 的实现类，委托给 SmsDataSource。

```kotlin
@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val smsDataSource: SmsDataSource
) : SmsRepository
```

**特点：**

- 使用 Hilt 依赖注入
- 单例模式
- 纯粹的委托实现，无额外业务逻辑

### FilterHistoryRepository

筛选历史仓库接口。

```kotlin
interface FilterHistoryRepository {
    fun getHistory(): List<String>
    fun addHistory(keyword: String)
    fun clearHistory()
}
```

**方法：**

- `getHistory()`: 获取历史记录列表（按时间倒序）
- `addHistory(keyword)`: 添加历史记录（自动去重，最多保留 5 条）
- `clearHistory()`: 清空历史记录

### FilterHistoryRepositoryImpl

FilterHistoryRepository 的实现类，基于 SharedPreferences 存储。

```kotlin
@Singleton
class FilterHistoryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FilterHistoryRepository
```

**特点：**

- 使用 SharedPreferences 存储（文件名：`filter_history`）
- 最大历史记录数：5（`MAX_HISTORY_SIZE`）
- 分隔符：`|||`
- 新记录插入到列表头部，超出上限时丢弃末尾记录

## 数据源

### SmsDataSource

短信数据源，封装 ContentResolver 操作。

```kotlin
@Singleton
class SmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    suspend fun getSmsMessagesByIds(ids: List<Long>): List<SmsMessage>

    suspend fun getTotalCount(filterState: FilterState): Int

    suspend fun deleteMessages(ids: List<Long>): Int

    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean

    fun getSimCards(): List<SimCardInfo>

    fun invalidateTotalCountCache()
}
```

**特性：**

- 使用 `Telephony.Sms.CONTENT_URI` 访问短信数据库
- 总数缓存机制（无筛选条件时缓存，删除/插入时失效）
- 删除操作分块处理（每块 100 条）
- 自动查询联系人姓名（通过 `ContactsContract.PhoneLookup`）
- 按时间倒序排列（`DATE DESC`）
- 分页查询（`LIMIT ... OFFSET ...`）

### SmsSelectionBuilder

SQL 筛选条件构建器，将 FilterState 转换为 ContentResolver 查询参数。

```kotlin
object SmsSelectionBuilder {
    fun buildSelection(filterState: FilterState): Pair<String?, Array<String>?>
    fun getTodayStart(): Long
}
```

**方法：**

- `buildSelection(filterState)`: 构建 SQL WHERE 子句和参数数组
- `getTodayStart()`: 获取今天零点的时间戳

**支持的筛选条件：**

- 关键词：`BODY LIKE '%keyword%'`
- 号码：`ADDRESS LIKE '%number%'`
- 日期范围：`DATE >= ?` / `DATE <= ?`
- 已读状态：`READ = 0/1`
- 锁定状态：`LOCKED = 0/1`
- 消息类型：`TYPE = 1/2/3/4`
- SIM 卡：`SUBSCRIPTION_ID = ?`
- 联系人：`THREAD_ID = ?`

## 工具类

### DefaultSmsManager

默认短信 App 管理工具。

```kotlin
object DefaultSmsManager {
    fun isDefaultSmsApp(context: Context): Boolean
    fun requestDefaultSmsRole(activity: Activity, launcher: ActivityResultLauncher<Intent>)
    fun openDefaultAppsSettings(activity: Activity)
}
```

**方法：**

- `isDefaultSmsApp(context)`: 检查当前 App 是否为默认短信 App（委托给 PermissionUtils.isDefaultSmsApp）
- `requestDefaultSmsRole(activity, launcher)`: 请求默认短信 App 角色（Android 10+ 使用 RoleManager，旧版本使用 `Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT`）
- `openDefaultAppsSettings(activity)`: 打开默认应用设置页面（`Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS`）

### PermissionUtils

权限工具类。

```kotlin
object PermissionUtils {
    val REQUIRED_PERMISSIONS: Array<String>

    fun hasAllPermissions(context: Context): Boolean
    fun getDeniedPermissions(context: Context): List<String>
    fun shouldShowRationale(activity: Activity): Boolean
    fun hasPermanentlyDeniedPermissions(activity: Activity): Boolean
    fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean
    fun isDefaultSmsApp(context: Context): Boolean
}
```

**常量：**

- `REQUIRED_PERMISSIONS`: 必需权限数组
    - `android.permission.READ_SMS`
    - `android.permission.READ_CONTACTS`

**方法：**

- `hasAllPermissions(context)`: 检查是否已授予所有必需权限
- `getDeniedPermissions(context)`: 获取被拒绝的权限列表
- `shouldShowRationale(activity)`: 检查是否应该显示权限说明（用户拒绝过一次但没有选择"不再询问"）
- `hasPermanentlyDeniedPermissions(activity)`: 检查是否有永久拒绝的权限（用户选择了"不再询问"）
- `isPermissionPermanentlyDenied(activity, permission)`: 检查特定权限是否被永久拒绝
- `isDefaultSmsApp(context)`: 检查是否为默认短信 App（Android 10+ 使用 RoleManager，Android 4.4-9 使用 Telephony.Sms）

### CsvParser

RFC 4180 兼容的 CSV 解析器。

```kotlin
object CsvParser {
    fun readCsvLine(reader: BufferedReader): List<String>?
}
```

**方法：**

- `readCsvLine(reader)`: 读取单条 CSV 记录，支持多行引用字段，返回字段值列表或 null（流结束）

**特性：**

- 支持双引号包裹的字段
- 支持字段内换行（CRLF）
- 支持转义引号（`""`）
- 支持逗号分隔

## UI 状态

### SmsUiState

短信列表 UI 状态密封类。

```kotlin
sealed class SmsUiState {
    object Loading : SmsUiState()
    data class Success(
        val messages: List<SmsMessage>,
        val totalCount: Int,
        val filteredCount: Int,
        val hasMore: Boolean,
        val isLoading: Boolean = false
    ) : SmsUiState()
    data class Error(val message: String) : SmsUiState()
}
```

**状态：**

- `Loading`: 加载中
- `Success`: 加载成功
    - `messages`: 当前页消息列表
    - `totalCount`: 总消息数
    - `filteredCount`: 筛选后消息数
    - `hasMore`: 是否有更多数据
    - `isLoading`: 是否正在加载下一页
- `Error`: 加载失败
    - `message`: 错误信息

### OperationState

操作状态密封类，用于删除、导出、导入等操作。

```kotlin
sealed class OperationState {
    object Idle : OperationState()
    data class Progress(val current: Int, val total: Int) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
```

**状态：**

- `Idle`: 空闲状态
- `Progress`: 操作进行中
    - `current`: 当前进度
    - `total`: 总数
- `Success`: 操作成功
    - `message`: 成功信息
- `Error`: 操作失败
    - `message`: 错误信息
