package top.yuameshi.sms.cleaner.domain.usecase

import top.yuameshi.sms.cleaner.data.model.SimCardInfo
import top.yuameshi.sms.cleaner.data.repository.SmsRepository
import javax.inject.Inject

/**
 * 加载SIM卡信息并判断是否使用短名称
 *
 * 短名称唯一性判断逻辑：如果所有SIM卡的短名称都不重复，则使用短名称；否则使用长名称。
 */
class LoadSimCardsUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    /**
     * 加载SIM卡列表并判断短名称是否唯一
     *
     * @return SimCardsResult 包含SIM卡列表和短名称唯一性标志
     */
    operator fun invoke(): SimCardsResult {
        val cards = smsRepository.getSimCards()
        val shortNames = cards.map { it.getShortName() }
        val useShortSimName = shortNames.distinct().size == shortNames.size
        return SimCardsResult(
            simCards = cards,
            useShortSimName = useShortSimName
        )
    }
}

/**
 * SIM卡加载结果
 *
 * @property simCards SIM卡列表
 * @property useShortSimName 短名称是否唯一（true=用短名称，false=用长名称）
 */
data class SimCardsResult(
    val simCards: List<SimCardInfo>,
    val useShortSimName: Boolean
)
