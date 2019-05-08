package beanly.exts.commands.utility.osu

import framework.core.transformers.utility.SplitTime
import java.time.LocalDateTime
import kotlin.math.roundToInt

class OsuUserInfo(
    val userId: String,
    val username: String,
    val globalRank: String,
    val countryRank: String,
    val ssh: String,
    val ss: String,
    val sh: String,
    val s: String,
    val a: String,
    private val ppDecimal: String,
    private val joinTimeRaw: String,
    private val playTimeSeconds: String
) {
    val pp get() = ppDecimal.toDouble().roundToInt()
    val joinTime get() = LocalDateTime.parse(joinTimeRaw.replace(" ", "T"))!!
    val playTime get() = SplitTime(playTimeSeconds.toLong() * 1_000)
}
