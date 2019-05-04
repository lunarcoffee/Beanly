package framework.api.dsl

import framework.core.Bot
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

class BotDsl(configPath: String) : Bot(configPath) {
    var activity: Activity?
        get() = jda.presence.activity
        set(value) {
            jda.presence.activity = value
        }

    var status: OnlineStatus
        get() = jda.presence.status
        set(value) {
            jda.presence.setStatus(value)
        }
}

fun startBot(configPath: String, init: BotDsl.() -> Unit): Bot {
    return BotDsl(configPath).apply {
        init()
        loadCommands()
    }
}