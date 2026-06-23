package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import top.yuameshi.sms.cleaner.util.PermissionUtils
import javax.inject.Inject

/**
 * 检查是否已授予所有必需权限的UseCase
 * 封装PermissionUtils.hasAllPermissions的调用逻辑
 */
class CheckPermissionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 检查是否已授予所有必需权限
     * @return true如果所有权限已授予，false否则
     */
    operator fun invoke(): Boolean {
        return PermissionUtils.hasAllPermissions(context)
    }
}
