package beanly.exts.commands.utility.osu.user

import beanly.consts.BEANLY_CONFIG
import beanly.consts.Emoji
import beanly.consts.TIME_FORMATTER
import beanly.exts.commands.utility.osu.OsuHasMode
import com.google.gson.GsonBuilder
import framework.api.dsl.embed
import framework.api.extensions.error
import framework.api.extensions.send
import framework.core.CommandContext
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OsuUser(private val usernameOrId: String, override val mode: Int) : OsuHasMode() {
    suspend fun sendDetails(ctx: CommandContext) {
        val info = getUserInfo(ctx) ?: return
        ctx.send(
            embed {
                info.run {
                    val link = "https://osu.ppy.sh/users/$userId/$modeUrl"

                    title = "${modeEmoji()}  Info on player **$username**:"
                    description = """
                        |**User ID**: $userId
                        |**Global rank**: #$globalRank
                        |**Country rank**: #$countryRank in $country
                        |**PP**: $pp
                        |**SS+/SS/S+/S/A**: $ssh/$ss/$sh/$s/$a
                        |**Join time**: ${joinTime.format(TIME_FORMATTER).drop(4)}
                        |**Play time**: $playTime
                        |**Link**: $link
                    """.trimMargin()

                    thumbnail { url = "https://a.ppy.sh/$userId" }
                }
            }
        )
    }

    private suspend fun getUserInfo(ctx: CommandContext): OsuUserInfo? {
        return try {
            withContext(Dispatchers.Default) {
                OSU_USER_GSON.fromJson(
                    httpPost {
                        url("https://osu.ppy.sh/api/get_user")
                        param {
                            "k" to BEANLY_CONFIG.osuToken
                            "u" to usernameOrId
                            "m" to mode
                        }
                    }.body()!!.charStream().readText().drop(1).dropLast(1),
                    OsuUserInfo::class.java
                )
            }
        } catch (e: IllegalStateException) {
            ctx.error("I can't find a player with that name!")
            return null
        }
    }

    private fun modeEmoji(): Emoji {
        return when (mode) {
            0 -> Emoji.COMPUTER_MOUSE
            1 -> Emoji.DRUM
            2 -> Emoji.PINEAPPLE
            3 -> Emoji.MUSICAL_KEYBOARD
            else -> throw IllegalStateException()
        }
    }

    companion object {
        private val OSU_USER_GSON = GsonBuilder()
            .setFieldNamingStrategy(OsuUserStrategy())
            .create()
    }
}
