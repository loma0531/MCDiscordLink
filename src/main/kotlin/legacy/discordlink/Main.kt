package legacy.discordlink

import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    lateinit var cfg: ConfigManager
    lateinit var db: DatabaseManager
    lateinit var discord: DiscordBot

    override fun onEnable() {
        // Ensure data folder and config
        if (!dataFolder.exists()) dataFolder.mkdirs()

        cfg = ConfigManager(this)
        db = DatabaseManager(this, cfg)

        if (cfg.isDatabaseEnabled()) {
            if (!db.initialize()) {
                logger.severe("Database initialization failed — disabling plugin")
                server.pluginManager.disablePlugin(this)
                return
            }
        } else {
            logger.severe("Database is disabled in config — plugin requires database")
            server.pluginManager.disablePlugin(this)
            return
        }

        // Discord bot with new flow
        discord = DiscordBot(logger, db, cfg, cfg.getDiscordToken())
        discord.start()

        // Register join listener that generates codes and kicks unlinked players
        server.pluginManager.registerEvents(JoinListener(this, db), this)

        logger.info("MCDiscordLink enabled - Use /setup-link-discord in Discord")
    }

    override fun onDisable() {
        try {
            discord.stop()
        } catch (e: Exception) {
            if (cfg.getDebug()) logger.warning("Error stopping discord bot: ${e.message}")
        }
        db.close()
        logger.info("MCDiscordLink disabled")
    }
}
