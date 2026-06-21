# API 文档

## 数据模型

### SmsMessage

短信数据模型。

```kotlin
data class SmsMessage(
    val id: Long,           // 短信 ID
    val address: String,    // 号码
    val body: String,       // 内容
    val date: Long,         // 时间戳
    val type: Int,          // 消息类型
    val read: Boolean,      // 是否已读
    val locked: Boolean,    // 是否锁定
    val subId: Int,         // SIM 卡 ID
    val threadId: Long,     // 会话 ID
    val contactName: String? = null  // 联系人名称
) {
    companion object {
        const val TYPE_INBOX = 1    // 收件箱
        const val TYPE_SENT = 2     // 已发送
        const val TYPE_DRAFT = 3    // 草稿
        const val TYPE_OUTBOX = 4   // 发件箱
        const val TYPE_FAILED = 5   // 发送失败
        const val TYPE_QUEUED = 6   // 待发送

        fun getTypeName(type: Int): String
    }
}
```

### FilterState

筛选状态模型。

```kotlin
data class FilterState(
    val keyword: String = "",                    // 关键词
    val number: String = "",                     // 号码
    val regex: String = "",                      // 正则表达式
    val isRegexMode: Boolean = false,            // 是否正则模式
    val dateRange: DateRange = DateRange.ALL,    // 日期范围
    val customStartDate: Long? = null,           // 自定义开始时间
    val customEndDate: Long? = null,             // 自定义结束时间
    val readStatus: ReadStatus = ReadStatus.ALL, // 已读状态
    val lockStatus: LockStatus = LockStatus.ALL, // 锁定状态
    val messageType: MessageType = MessageType.ALL, // 消息类型
    val simId: SimId = SimId.ALL,                // SIM 卡
    val contactId: Long? = null,                 // 联系人 ID
    val contactName: String? = null              // 联系人名称
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

    enum class SimId {
        ALL, SIM1, SIM2
    }

    fun hasFilters(): Boolean
    fun toQueryString(): String
}
```

### SelectionState

选择状态模型。

```kotlin
data class SelectionState(
    val isMultiSelectMode: Boolean = false,  // 是否多选模式
    val selectedIds: Set<Long> = emptySet(), // 选中的 ID 集合
    val isSelectAll: Boolean = false,        // 是否全选
    val totalFilteredCount: Int = 0          // 筛选总数
) {
    val selectedCount: Int  // 选中数量

    fun toggleSelection(id: Long): SelectionState
    fun selectAll(totalCount: Int): SelectionState
    fun clearSelection(): SelectionState
    fun enterMultiSelectMode(id: Long): SelectionState
    fun exitMultiSelectMode(): SelectionState
    fun isSelected(id: Long): Boolean
}
```

## 用例

### GetSmsUseCase

获取短信用例。

```kotlin
class GetSmsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    // 获取短信列表（分页）
    suspend operator fun invoke(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    // 获取总数
    suspend fun getTotalCount(filterState: FilterState): Int
}
```

### FilterSmsUseCase

筛选短信用例。

```kotlin
class FilterSmsUseCase @Inject constructor() {
    // 构建筛选状态
    fun buildFilterState(
        keyword: String = "",
        number: String = "",
        regex: String = "",
        isRegexMode: Boolean = false,
        dateRange: FilterState.DateRange = FilterState.DateRange.ALL,
        customStartDate: Long? = null,
        customEndDate: Long? = null,
        readStatus: FilterState.ReadStatus = FilterState.ReadStatus.ALL,
        lockStatus: FilterState.LockStatus = FilterState.LockStatus.ALL,
        messageType: FilterState.MessageType = FilterState.MessageType.ALL,
        simId: FilterState.SimId = FilterState.SimId.ALL,
        contactId: Long? = null,
        contactName: String? = null
    ): FilterState

    // 验证正则表达式
    fun validateRegex(regex: String): Boolean

    // 获取日期范围毫秒值
    fun getDateRangeMillis(dateRange: FilterState.DateRange): Pair<Long, Long>?
}
```

### ExportSmsUseCase

导出短信用例。

```kotlin
class ExportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    // 导出短信
    suspend operator fun invoke(
        filterState: FilterState,
        exportAll: Boolean,
        outputStream: OutputStream,
        onProgress: (exported: Int, total: Int) -> Unit
    ): Result<Int>
}
```

### ImportSmsUseCase

导入短信用例。

```kotlin
class ImportSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsOperationManager: SmsOperationManager
) {
    // 导入短信
    suspend operator fun invoke(
        uri: Uri,
        onProgress: (imported: Int, skipped: Int) -> Unit
    ): Result<ImportResult>

    data class ImportResult(
        val imported: Int,  // 导入数量
        val skipped: Int    // 跳过数量
    )
}
```

## 管理器

### SmsOperationManager

统一的短信数据库操作管理器。

```kotlin
@Singleton
class SmsOperationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smsRepository: SmsRepository
) {
    // 判断是否需要设置为默认短信App
    fun needsDefaultSmsApp(): Boolean

    // 按 ID 删除
    suspend fun deleteMessages(ids: List<Long>): Int

    // 按筛选条件删除
    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    // 插入短信
    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    // 检查重复
    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean
}
```

## 仓库

### SmsRepository

短信仓库。

```kotlin
@Singleton
class SmsRepository @Inject constructor(
    private val smsDataSource: SmsDataSource
) {
    // 获取短信列表（分页）
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    // 获取总数
    suspend fun getTotalCount(filterState: FilterState): Int

    // 按 ID 删除
    suspend fun deleteMessages(ids: List<Long>): Int

    // 按筛选条件删除
    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    // 插入短信
    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    // 检查重复
    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean
}
```

### FilterHistoryRepository

筛选历史仓库。

```kotlin
@Singleton
class FilterHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 获取历史记录
    fun getHistory(): List<String>

    // 添加历史记录
    fun addHistory(keyword: String)

    // 清空历史记录
    fun clearHistory()
}
```

## 数据源

### SmsDataSource

短信数据源。

```kotlin
@Singleton
class SmsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 获取短信列表（分页）
    suspend fun getSmsMessages(
        filterState: FilterState,
        page: Int,
        pageSize: Int = 50
    ): List<SmsMessage>

    // 获取总数
    suspend fun getTotalCount(filterState: FilterState): Int

    // 按 ID 删除
    suspend fun deleteMessages(ids: List<Long>): Int

    // 按筛选条件删除
    suspend fun deleteMessagesByFilter(filterState: FilterState): Int

    // 插入短信
    suspend fun insertMessage(
        address: String,
        body: String,
        date: Long,
        type: Int,
        read: Boolean,
        subId: Int
    ): Uri?

    // 检查重复
    suspend fun checkDuplicate(address: String, body: String, date: Long): Boolean
}
```

## 工具类

### DefaultSmsManager

默认短信应用管理器。

```kotlin
object DefaultSmsManager {
    // 是否为默认短信应用
    fun isDefaultSmsApp(context: Context): Boolean

    // 获取默认短信应用包名
    fun getDefaultSmsPackage(context: Context): String?

    // 请求默认短信应用角色
    fun requestDefaultSmsRole(activity: Activity, launcher: ActivityResultLauncher<Intent>)

    // 打开默认应用设置
    fun openDefaultAppsSettings(activity: Activity)

    // 处理默认短信应用结果
    fun handleDefaultSmsResult(
        context: Context,
        originalDefaultSmsApp: String?,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    )
}
```

### PermissionUtils

权限工具类。

```kotlin
object PermissionUtils {
    // 必需权限
    val REQUIRED_PERMISSIONS: Array<String>

    // 是否有所有权限
    fun hasAllPermissions(context: Context): Boolean

    // 是否有短信权限
    fun hasSmsPermission(context: Context): Boolean

    // 是否有通讯录权限
    fun hasContactsPermission(context: Context): Boolean

    // 是否为默认短信应用
    fun isDefaultSmsApp(context: Context): Boolean

    // 获取默认短信应用包名
    fun getDefaultSmsPackage(context: Context): String?
}
```

## UI 状态

### SmsUiState

```kotlin
sealed class SmsUiState {
    object Loading : SmsUiState()
    data class Success(
        val messages: List<SmsMessage>,
        val totalCount: Int,
        val filteredCount: Int,
        val hasMore: Boolean
    ) : SmsUiState()
    data class Error(val message: String) : SmsUiState()
}
```

### OperationState

```kotlin
sealed class OperationState {
    object Idle : OperationState()
    data class Progress(val current: Int, val total: Int) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
```
