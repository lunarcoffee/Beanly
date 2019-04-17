package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class TrSplit(
    private val separator: String = " ",
    override val default: List<String> = emptyList(),
    override val name: String = "split"
) : Transformer<List<String>> {

    // Split is always technically optional.
    override val optional = true

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): List<String> {
        return args.joinToString(" ").split(separator).also {
            args.clear()
        }
    }

    override fun toString() = name
}
