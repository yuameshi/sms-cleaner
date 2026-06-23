package top.yuameshi.sms.cleaner.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import top.yuameshi.sms.cleaner.util.DefaultSmsManager
import javax.inject.Inject

/**
 * 检查当前App是否为默认短信App的UseCase
 * 封装DefaultSmsManager.isDefaultSmsApp的调用逻辑
 */
class CheckDefaultSmsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 检查当前App是否为默认短信App
     * @return true如果当前App是默认短信App，false否则
     */
    operator fun invoke(): Boolean {
        return DefaultSmsManager.isDefaultSmsApp(context)
    }
}
