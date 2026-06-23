package top.yuameshi.sms.cleaner.data.model

data class SimCardInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String,
    val slotIndex: Int
) {
    fun getFormattedName(): String {
        val parts = mutableListOf<String>()
        
        // Priority: displayName > carrierName > "SIM ${slotIndex + 1}"
        when {
            displayName.isNotBlank() -> parts.add(displayName)
            carrierName.isNotBlank() -> parts.add(carrierName)
            else -> parts.add("SIM ${slotIndex + 1}")
        }
        
        // Append phone number if available (masked for privacy)
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
