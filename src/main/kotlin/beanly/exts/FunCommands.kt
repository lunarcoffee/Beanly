@file:Suppress("unused")

package beanly.exts

import beanly.consts.EMOJI_BILLIARD_BALL
import beanly.consts.EMOJI_GAME_DIE
import beanly.consts.EMOJI_RADIO_BUTTON
import beanly.consts.EMOJI_THINKING
import beanly.exts.utility.DiceRoll
import beanly.exts.utility.toDiceRoll
import beanly.trimToDescription
import framework.CommandGroup
import framework.dsl.command
import framework.dsl.embed
import framework.extensions.await
import framework.extensions.error
import framework.extensions.send
import framework.extensions.success
import framework.transformers.TrGreedy
import framework.transformers.TrInt
import framework.transformers.TrRest
import framework.transformers.TrSplit
import kotlin.random.Random

@CommandGroup("Fun")
class FunCommands {
    fun flip() = command("flip") {
        description = "Flips coins."
        aliases = listOf("coin", "flipcoin")

        extDescription = """
            |`$name [times]`\n
            |If an argument is provided, this command flips `times` coins, displaying all of the
            |flip results. If no argument is provided, this command will flip one coin.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(true, 1))
        execute { ctx, args ->
            val times = args.get<Int>(0)

            if (times !in 1..292) {
                ctx.error("I can't flip that number of coins!")
                return@execute
            }

            val flips = (1..times).map { if (Random.nextBoolean()) "heads" else "tails" }
            val heads = flips.count { it == "heads" }
            val tails = flips.count { it == "tails" }

            // Only show the resultant flip if only one coin was flipped, and count each if more
            // than one coin was flipped.
            val result = if (times == 1) {
                flips[0]
            } else {
                "$heads heads and $tails tails"
            }

            ctx.send(
                embed {
                    title = "$EMOJI_RADIO_BUTTON  You flipped $result!"
                    description = flips.toString()
                }
            )
        }
    }

    fun roll() = command("roll") {
        description = "Rolls dice with RPG style roll specifiers."
        aliases = listOf("dice", "rolldice")

        extDescription = """
            |`$name [roll specs...]`\n
            |Rolls dice according to roll specifiers. Some examples are:\n
            | - `d6`: rolls a six-sided die\n
            | - `2d8`: rolls two eight-sided dice\n
            | - `d20+1`: rolls a twenty-sided die and adds one to the result\n
            | - `3d4-2`: rolls three four-sided dice and subtracts two from the result\n
            |If no specifiers are provided, a single `d6` is used.
        """.trimToDescription()

        expectedArgs = listOf(TrGreedy(String::toDiceRoll, listOf(DiceRoll(1, 6, 0))))
        execute { ctx, args ->
            val diceRolls = args.get<List<DiceRoll>>(0)

            // Check for constraints with helpful feedback.
            for (roll in diceRolls) {
                val errorMsg = when {
                    roll.times !in 1..100 -> "I can't roll a die that many times!"
                    roll.sides !in 1..1000 -> "I can't roll a die with that many sides!"
                    roll.mod !in -10000..10000 -> "That modifier is too big or small!"
                    else -> ""
                }

                if (errorMsg.isNotEmpty()) {
                    ctx.error(errorMsg)
                    return@execute
                }
            }

            // Generate a list of lists that hold each result for each roll.
            val results = diceRolls
                .map { roll -> List(roll.times) { Random.nextInt(1, roll.sides + 1) } }

            // Sum all the results of each individual roll and add all the modifiers.
            val total = results.flatten().sum() + diceRolls.sumBy { it.mod }

            // If the user rolls more than one die, make the embed title "You rolled a total of..."
            // instead of "You rolled a..." if only one die was rolled. Makes it a bit more human.
            val totalOfOrEmpty = if (diceRolls.size > 1) "total of " else ""

            ctx.send(
                embed {
                    title = "$EMOJI_GAME_DIE  You rolled a $totalOfOrEmpty$total!"

                    description = results.zip(diceRolls).joinToString("\n") { (res, roll) ->
                        val modifierSign = if (roll.mod <= 0) "" else "+"
                        val modifier = if (roll.mod != 0) roll.mod.toString() else ""

                        val modifierAndSign = modifierSign + modifier
                        "**${roll.times}d${roll.sides}$modifierAndSign**: $res $modifierAndSign"
                    }
                }
            )
        }
    }

    fun pick() = command("pick") {
        description = "Picks a value from the options you give."
        aliases = listOf("select", "choose")

        extDescription = """
            |`$name options...`\n
            |Picks a value from `options`, which is a list of choices separated by `|` surrounded
            |by spaces (so you can use the pipe in an option for things like `Wolfram|Alpha`).
        """.trimToDescription()

        expectedArgs = listOf(TrSplit(" | "))
        execute { ctx, args ->
            val options = args.get<List<String>>(0)

            if (options.size < 2) {
                ctx.error("I need at least 2 options to choose from!")
                return@execute
            }

            ctx.send(
                embed {
                    title = "$EMOJI_THINKING  I choose **${options.random()}**!"
                    description = options.toString()
                }
            )
        }
    }

    fun eightBall() = command("8ball") {
        val responses = listOf(
            "It is certain.",
            "It is decidedly so.",
            "Without a doubt.",
            "Yes - definitely.",
            "You may rely on it.",
            "As I see it, yes.",
            "Most likely.",
            "Outlook good.",
            "Yes.",
            "Signs point to yes.",
            "Reply hazy, try again.",
            "Ask again later.",
            "Better not tell you now.",
            "Cannot predict now.",
            "Concentrate and ask again.",
            "Don't count on it.",
            "My reply is no.",
            "My sources say no.",
            "Outlook not so good.",
            "Very doubtful."
        )

        description = "Uncover secrets with the 100% reliable Magic 8 Ball!"
        aliases = listOf("magiceightball")

        extDescription = """
            |`$name question`
            |Ask the Magic 8 Ball a question and it will undoubtedly tell you the truth (unless
            |it's tired and wants to sleep and not answer your question, in which case you should
            |simply ask again, politely).
        """.trimToDescription()

        expectedArgs = listOf(TrRest())
        execute { ctx, args ->
            val question = args.get<String>(0)

            if (question.isBlank()) {
                ctx.error("You have to ask me a question!")
                return@execute
            }

            ctx.send(
                embed {
                    title = "$EMOJI_BILLIARD_BALL  The 8-ball says:"
                    description = responses.random()
                }
            )
        }
    }

    fun steal() = command("steal") {
        description = "Steals emotes from message history in the current channel."
        aliases = listOf("stealemotes")

        extDescription = """
            |`$name [limit]`\n
            |Steals custom emotes from the current channel's history. If `limit` is specified, this
            |command will attempt to steal all emotes from the past `limit` messages. If not, the
            |default is the past 100 messages.
        """.trimToDescription()

        expectedArgs = listOf(TrInt(true, 100))
        execute { ctx, args ->
            val historyToSearch = args.get<Int>(0)

            if (historyToSearch !in 1..1000) {
                ctx.error("I can't steal from that many messages in history!")
                return@execute
            }

            val pmChannel = ctx.event.author.openPrivateChannel().await()
            pmChannel.success("Your emotes are being processed!")

            ctx.event
                .channel
                .iterableHistory
                .take(historyToSearch)
                .flatMap { it.emotes }
                .distinct()
                .map { "**${it.name}**: <${it.imageUrl}>" }
                .chunked(20)
                .forEach { pmChannel.send("*::*\n${it.joinToString("\n")}") }

            ctx.success("Your stolen emotes have been sent to you!")
        }
    }

    fun emote() = command("emote") {
        description = "Sends emotes from servers I'm in by your choice."
        aliases = listOf("sendemote")

        extDescription = """
            |`$name [emote names...]`\n
            |
        """.trimToDescription()

        expectedArgs = listOf(TrGreedy(String::toString))
        execute { ctx, args ->
            val emoteNames = args.get<List<String>>(0)
            val emotes = emoteNames
                .map { ctx.jda.getEmotesByName(it, true).firstOrNull()?.asMention }

            if (emotes.any { it == null }) {
                ctx.error("I don't have access to one or more of those emotes!")
                return@execute
            }

            // Makes the experience a bit more human.
            val pluralOrNotEmotes = if (emotes.size == 1) {
                "is your emote"
            } else {
                "are your emotes"
            }

            ctx.success("Here $pluralOrNotEmotes: ${emotes.joinToString(" ")}")
        }
    }
}
