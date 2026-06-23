package top.yuameshi.sms.cleaner.data.model

data class SimCardInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String,
    val slotIndex: Int
) {
    /**
     * 短名称：仅运营商名称（不含手机号）
     */
    fun getShortName(): String {
        return when {
            displayName.isNotBlank() -> displayName
            carrierName.isNotBlank() -> carrierName
            else -> "SIM ${slotIndex + 1}"
        }
    }

    /**
     * 长名称：运营商名称 + 蒙版手机号
     */
    fun getFormattedName(): String {
        val parts = mutableListOf<String>()
        parts.add(getShortName())
        
        if (phoneNumber.isNotBlank()) {
            val masked = if (phoneNumber.length > 4) {
                "*${phoneNumber.takeLast(4)}"
            } else {
                phoneNumber
            }
            parts.add(masked)
        }
        
        return parts.joinToString(" ")
    }
}
