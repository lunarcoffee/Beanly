package framework.transformers

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.math.BigInteger

class TrBigInt(
    override val optional: Boolean = false,
    override val default: BigInteger = BigInteger.ZERO,
    override val name: String = "number"
) : Transformer<BigInteger> {

    override fun transform(event: MessageReceivedEvent, args: MutableList<String>): BigInteger {
        return if (optional && args.firstOrNull()?.toBigIntegerOrNull() == null) {
            default
        } else {
            args.removeAt(0).toBigInteger()
        }
    }

    override fun toString() = name
}
