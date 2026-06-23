package top.yuameshi.sms.cleaner.util

import android.os.Build

/**
 * 设备检测工具类
 * 用于识别小米设备、MIUI系统和澎湃OS（HyperOS）
 *
 * 参考文档: https://dev.mi.com/docs/appsmarket/technical_docs/system&device_identification/
 */
object DeviceUtils {

    /**
     * 检测当前设备是否为小米设备
     * 通过 Build.MANUFACTURER 判断
     */
    fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    /**
     * 检测当前系统是否为MIUI
     * 通过系统属性 ro.miui.ui.version.name 判断
     */
    fun isMiui(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            val versionName = getMethod.invoke(null, "ro.miui.ui.version.name", "") as String
            versionName.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检测当前系统是否为澎湃OS（HyperOS）
     * 通过系统属性 ro.mi.os.version.name 判断
     */
    fun isHyperOS(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java, String::class.java)
            val versionName = getMethod.invoke(null, "ro.mi.os.version.name", "") as String
            versionName.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 检测当前系统是否为MIUI或澎湃OS
     * 用于判断SIM卡筛选功能的兼容性
     */
    fun isMiuiOrHyperOS(): Boolean {
        return isMiui() || isHyperOS()
    }
}
