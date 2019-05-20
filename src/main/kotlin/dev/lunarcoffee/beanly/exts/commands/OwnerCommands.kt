@file:Suppress("unused")

package dev.lunarcoffee.beanly.exts.commands

import dev.lunarcoffee.beanly.consts.EMBED_COLOR
import dev.lunarcoffee.beanly.consts.Emoji
import dev.lunarcoffee.beanly.exts.commands.utility.ExecResult
import dev.lunarcoffee.beanly.exts.commands.utility.executeKotlin
import dev.lunarcoffee.beanly.exts.commands.utility.executeShellScript
import dev.lunarcoffee.beanly.trimToDescription
import dev.lunarcoffee.framework.api.dsl.command
import dev.lunarcoffee.framework.api.dsl.embed
import dev.lunarcoffee.framework.api.dsl.messagePaginator
import dev.lunarcoffee.framework.api.extensions.error
import dev.lunarcoffee.framework.api.extensions.send
import dev.lunarcoffee.framework.api.extensions.success
import dev.lunarcoffee.framework.core.annotations.CommandGroup
import dev.lunarcoffee.framework.core.transformers.TrInt
import dev.lunarcoffee.framework.core.transformers.TrRest
import dev.lunarcoffee.framework.core.transformers.TrSplit
import dev.lunarcoffee.framework.core.transformers.TrWord
import kotlinx.coroutines.delay
import java.util.regex.PatternSyntaxException
import kotlin.system.exitProcess

@CommandGroup("Owner")
class OwnerCommands {
    fun ex() = command("ex") {
        description = "Executes arbitrary code. Only my owner can use this."
        aliases = listOf("exec", "execute")

        ownerOnly = true
        noArgParsing = true

        extDescription = """
            |`$name code`\n
            |Executes Kotlin code in an unconstrained environment. This command can only be used by
            |my owner, for obvious security reasons. The only available global is `ctx`, the
            |`CommandContext` object associated with the current command execution. The event and
            |bot objects can be accessed from the command context. Note that star/wildcard imports
            |are not supported because I am lazy.
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val code = args.get<String>(0)
            var language: String

            val codeLines = code
                .removeSurrounding("```")
                .also { language = it.substringBefore("\n") }
                .substringAfter("\n")
                .split("\n")

            val result = when (language) {
                "kotlin" -> executeKotlin(ctx, codeLines)
                "sh" -> executeShellScript(ctx, codeLines.joinToString("\n"))
                else -> {
                    ctx.error("You must specify a valid language in a code block!")
                    return@execute
                }
            }

            if (result == ExecResult.ERROR) {
                // No error message is required because the code execution function has already
                // taken care of that.
                return@execute
            }

            ctx.send(
                messagePaginator(ctx.event.author) {
                    result.run {
                        """
                        |--- $header ---
                        |- stderr:$stderr
                        |- stdout:$stdout
                        |+ Returned `${this.result}` in ~${time}ms."""
                            .trimMargin()
                            .replace(ctx.bot.config.token, "[REDACTED]")
                            .lines()
                            .chunked(16)
                            .forEach { page("```diff\n${it.joinToString("\n")}```") }
                    }
                }
            )
        }
    }

    fun smsg() = command("smsg") {
        description = "Sends a message. Only my owner can use this."
        aliases = listOf("sendmsg")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name message`\n
            |Sends a message to the command user's channel. This is an owner only command as to
            |prevent spam.
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val content = args.get<String>(0)
            ctx.send(content)
        }
    }

    fun semt() = command("semt") {
        description = "Sends one or more emotes. Only my owner can use this."
        aliases = listOf("sendemote")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name names...`\n
            |Sends one or more emotes to the command user's channel. This is an owner only command
            |for... hm. I'm not too sure why this is an owner only command. I guess you'll have to
            |stick with the `emotes` command. Anyway, if an emote is not found, I simply don't send
            |that one (unlike with `emotes`).
        """.trimToDescription()

        expectedArgs = listOf(TrSplit())
        execute { ctx, args ->
            val emoteNames = args.get<List<String>>(0)
            val emotes = emoteNames
                .mapNotNull { ctx.jda.getEmotesByName(it, true).firstOrNull()?.asMention }
                .joinToString(" ")

            if (emotes.isEmpty()) {
                ctx.error("I don't have any of those emotes!")
                return@execute
            }

            ctx.send(emotes)
        }
    }

    fun sebd() = command("sebd") {
        description = "Sends an embed. Only my owner can use this."
        aliases = listOf("sendembed")

        ownerOnly = true
        deleteSender = true

        extDescription = """
            |`$name title description [color]`\n
            |Sends a message embed to the command user's channel. This is an owner only command as
            |to prevent spam. For more advanced usage, it is advised to use the `ex` command.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(), TrWord(), TrInt(true, EMBED_COLOR))
        execute { ctx, args ->
            val titleText = args.get<String>(0)
            val descriptionText = args.get<String>(1)
            val embedColor = args.get<Int>(2)

            ctx.send(
                embed {
                    title = titleText
                    description = descriptionText
                    color = embedColor
                }
            )
        }
    }

    fun regex() = command("regex") {
        description = "Tests a regex against some cases."
        aliases = listOf("testregex", "regularexpression")
        ownerOnly = true

        extDescription = """
            |`$name regex cases...`\n
            |This command attempts to match the given strings in `cases` with the given `regex`.
            |It will report which ones matched and which didn't, and for those that matched, if
            |there were groups, it will report those as well. The regex syntax is from Java. This
            |command can only be used by my owner to prevent ReDOS attacks.
        """.trimToDescription()

        expectedArgs = listOf(TrWord(), TrSplit())
        execute { ctx, args ->
            val regex = try {
                args.get<String>(0).toRegex()
            } catch (e: PatternSyntaxException) {
                ctx.error("That regex isn't valid!")
                return@execute
            }
            val cases = args.get<List<String>>(1)

            ctx.send(
                embed {
                    title = "${Emoji.SCALES}  Testing regex **$regex**:"
                    for (case in cases) {
                        val match = regex.matchEntire(case)?.groupValues ?: "(no match)"
                        description += "\n**$case**: $match"
                    }
                }
            )
        }
    }

    fun shutdown() = command("shutdown") {
        description = "Shuts down the bot. Only my owner can use this."
        ownerOnly = true

        extDescription = """
            |`$name`\n
            |Shuts down the bot process. There is a roughly three second long period of time
            |between command usage and actual process termination. First, I wait two seconds and
            |call `shutdownNow` on my `JDA` instance. Then, I wait another second and terminate
            |myself. Tragic. This is owner only for obvious reasons.
        """.trimToDescription()

        execute { ctx, _ ->
            ctx.success("Goodbye, world...")

            delay(2000)
            ctx.jda.shutdownNow()

            // Give JDA some time to shut down in case I'm in China with a 10 b/s connection. Oh,
            // wait... I wouldn't be able to access Discord without a VPN anyway.
            delay(1000)
            exitProcess(0)
        }
    }
}
