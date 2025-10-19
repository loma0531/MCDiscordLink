package legacy.discordlink.discord

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.config.MessageManager
import legacy.discordlink.discord.commands.McCheckCommand
import legacy.discordlink.discord.commands.SetupLinkCommand
import legacy.discordlink.discord.handlers.ButtonHandler
import legacy.discordlink.discord.handlers.ModalHandler
import legacy.discordlink.services.LinkingService
import legacy.discordlink.services.RoleService
import legacy.discordlink.services.VerificationService
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.commands.build.Commands

/**
 * Discord Bot Manager
 * 
 * Manages Discord bot lifecycle and registers commands/handlers
 */
class DiscordBotManager(
    private val logger: java.util.logging.Logger,
    private val linkingService: LinkingService,
    private val verificationService: VerificationService,
    private val roleService: RoleService,
    private val config: ConfigManager,
    private val messages: MessageManager,
    private val token: String
) {
    private var jda: JDA? = null
    
    // Commands
    private lateinit var setupLinkCommand: SetupLinkCommand
    private lateinit var mcCheckCommand: McCheckCommand
    
    // Handlers
    private lateinit var buttonHandler: ButtonHandler
    private lateinit var modalHandler: ModalHandler
    
    /**
     * Start the Discord bot
     */
    fun start() {
        try {
            // Initialize commands and handlers
            initializeComponents()
            
            // Build JDA
            jda = JDABuilder.createDefault(token)
                .addEventListeners(setupLinkCommand)
                .addEventListeners(mcCheckCommand)
                .addEventListeners(buttonHandler)
                .addEventListeners(modalHandler)
                .build()
            
            jda?.awaitReady()
            
            // Register slash commands
            registerCommands()
            
            logger.info(messages.getLogDiscordBotStarted())
            
        } catch (e: LinkageError) {
            logger.severe(messages.getLogDiscordBotJdaConflict())
            logger.severe(messages.getLogDiscordBotNoReload())
            throw e
        } catch (e: Exception) {
            logger.severe(messages.getLogDiscordBotStartFailed().replace("{error}", e.message ?: "Unknown"))
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Initialize commands and handlers
     */
    private fun initializeComponents() {
        setupLinkCommand = SetupLinkCommand(config, messages)
        mcCheckCommand = McCheckCommand(linkingService, messages)
        buttonHandler = ButtonHandler(linkingService, config, messages)
        modalHandler = ModalHandler(
            linkingService = linkingService,
            verificationService = verificationService,
            roleService = roleService,
            config = config,
            messages = messages,
            logger = logger
        )
    }
    
    /**
     * Register slash commands with Discord
     */
    private fun registerCommands() {
        jda?.updateCommands()?.addCommands(
            Commands.slash("setup-link-discord", "Setup Discord-Minecraft linking system (Admin only)"),
            Commands.slash("status", "ตรวจสอบสถานะการเชื่อมโยงบัญชี Discord/Minecraft (Admin only)")
                .addOption(
                    net.dv8tion.jda.api.interactions.commands.OptionType.STRING,
                    "user",
                    "ชื่อผู้ใช้ Discord (@user, ID) หรือชื่อผู้เล่น Minecraft",
                    true,
                    true
                )
        )?.queue()
    }
    
    /**
     * Stop the Discord bot
     */
    fun stop() {
        try {
            jda?.let { bot ->
                bot.shutdown()
                if (!bot.awaitShutdown(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    bot.shutdownNow()
                }
            }
            jda = null
        } catch (e: Exception) {
            logger.warning(messages.getLogDiscordBotStopError().replace("{error}", e.message ?: "Unknown"))
        }
    }
    
    /**
     * Get JDA instance
     */
    fun getJDA(): JDA? = jda
}
