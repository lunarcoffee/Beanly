package framework.core.transformers

import framework.core.transformers.utility.Found
import framework.core.transformers.utility.NotFound
import framework.core.transformers.utility.UserSearchResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrUser(
    override val optional: Boolean = false,
    override var default: UserSearchResult? = null
) : Transformer<UserSearchResult?> {

    override fun transform(
        event: MessageReceivedEvent,
        args: MutableList<String>
    ): UserSearchResult? {

        if (optional && args.isEmpty()) {
            return default
        }
        val input = args.removeAt(0)

        return Found(
            when {
                input.length == 18 -> event.jda.getUserById(input)
                input.matches(USER_TAG) -> event.jda.getUserByTag(input)
                input.matches(USER_MENTION) -> event.jda.getUserById(input.drop(2).take(18))
                else -> event.jda.getUsersByName(input, true).firstOrNull()
            } ?: return NotFound
        )
    }

    companion object {
        private val USER_TAG = """.+#\d{4}$""".toRegex()
        private val USER_MENTION = """<@\d{18}>""".toRegex()
    }
}