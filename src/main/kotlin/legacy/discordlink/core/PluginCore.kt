package legacy.discordlink.core

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.config.MessageManager
import legacy.discordlink.database.DatabaseManager
import legacy.discordlink.discord.DiscordBotManager
import legacy.discordlink.minecraft.commands.MCLinkCommand
import legacy.discordlink.minecraft.listeners.JoinListener
import legacy.discordlink.services.LinkingService
import legacy.discordlink.services.RoleService
import legacy.discordlink.services.VerificationService
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main plugin core class
 * Handles plugin lifecycle and component initialization
 */
class PluginCore(private val plugin: JavaPlugin) {
    
    // Core components
    lateinit var config: ConfigManager
        private set
    lateinit var messages: MessageManager
        private set
    lateinit var database: DatabaseManager
        private set
    lateinit var discord: DiscordBotManager
        private set
    
    // Services
    lateinit var linkingService: LinkingService
        private set
    lateinit var verificationService: VerificationService
        private set
    lateinit var roleService: RoleService
        private set
    
    /**
     * Initialize all plugin components
     * @return true if initialization successful, false otherwise
     */
    fun initialize(): Boolean {
        try {
            // Step 1: Load configurations
            if (!initializeConfigs()) {
                return false
            }
            
            // Step 2: Initialize database
            if (!initializeDatabase()) {
                return false
            }
            
            // Step 3: Initialize services
            initializeServices()
            
            // Step 4: Start Discord bot
            if (!initializeDiscord()) {
                return false
            }
            
            // Step 4: Register listeners and commands
            registerListeners()
            registerCommands()
            
            plugin.logger.info(messages.getLogPluginEnabled())
            return true
            
        } catch (e: LinkageError) {
            plugin.logger.severe(messages.getLogDiscordBotClassConflict())
            plugin.logger.severe(messages.getLogDiscordBotClassConflictHelp())
            return false
        } catch (e: Exception) {
            plugin.logger.severe("Failed to enable plugin: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Initialize configuration managers
     */
    private fun initializeConfigs(): Boolean {
        try {
            config = ConfigManager(plugin)
            messages = MessageManager(plugin)
            return true
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load configurations: ${e.message}")
            return false
        }
    }
    
    /**
     * Initialize database connection
     */
    private fun initializeDatabase(): Boolean {
        if (!config.isDatabaseEnabled()) {
            plugin.logger.severe(messages.getLogDatabaseDisabled())
            return false
        }
        
        database = DatabaseManager(plugin, config)
        if (!database.initialize()) {
            plugin.logger.severe(messages.getLogDatabaseInitFailed())
            return false
        }
        
        return true
    }
    
    /**
     * Initialize services
     */
    private fun initializeServices() {
        linkingService = LinkingService(
            linkRepository = database.linkRepository,
            codeRepository = database.codeRepository,
            maxAccountsPerDiscord = config.getMaxAccountsPerDiscord(),
            logger = plugin.logger,
            debug = config.getDebug()
        )
        
        verificationService = VerificationService(
            enabled = config.getVerificationEnabled(),
            minAccountAgeDays = config.getMinAccountAgeDays(),
            minServerJoinMinutes = config.getMinServerJoinMinutes()
        )
        
        roleService = RoleService(
            enabled = config.getGiveRoleAfterLink(),
            roleId = config.getLinkedRoleId(),
            logger = plugin.logger,
            debug = config.getDebug()
        )
    }
    
    /**
     * Initialize Discord bot
     */
    private fun initializeDiscord(): Boolean {
        try {
            discord = DiscordBotManager(
                logger = plugin.logger,
                linkingService = linkingService,
                verificationService = verificationService,
                roleService = roleService,
                config = config,
                messages = messages,
                token = config.getDiscordToken()
            )
            discord.start()
            return true
        } catch (e: LinkageError) {
            plugin.logger.severe(messages.getLogDiscordBotJdaConflict())
            plugin.logger.severe(messages.getLogDiscordBotNoReload())
            throw e
        } catch (e: Exception) {
            plugin.logger.severe(messages.getLogDiscordBotStartFailed().replace("{error}", e.message ?: "Unknown"))
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Register event listeners
     */
    private fun registerListeners() {
        plugin.server.pluginManager.registerEvents(
            JoinListener(plugin, linkingService, config),
            plugin
        )
    }
    
    /**
     * Register commands
     */
    private fun registerCommands() {
        try {
            // Register /mclink command
            val mclinkCommand = plugin.getCommand("mclink")
            if (mclinkCommand != null) {
                val commandExecutor = MCLinkCommand(plugin, this, linkingService, messages)
                mclinkCommand.setExecutor(commandExecutor)
                mclinkCommand.tabCompleter = commandExecutor
                plugin.logger.info(messages.getLogCommandMclinkRegistered())
            } else {
                plugin.logger.severe(messages.getLogCommandMclinkFailed())
            }
        } catch (e: Exception) {
            plugin.logger.severe(messages.getLogCommandRegistrationError().replace("{error}", e.message ?: "Unknown"))
            e.printStackTrace()
        }
    }
    
    /**
     * Reload plugin configuration and Discord bot
     */
    fun reload() {
        try {
            // Reload configurations
            config.reloadConfig()
            messages.reloadMessages()
            
            // Reinitialize services with new config
            initializeServices()
            
            // Restart Discord bot
            if (::discord.isInitialized) {
                discord.stop()
                Thread.sleep(500)
            }
            
            discord = DiscordBotManager(
                logger = plugin.logger,
                linkingService = linkingService,
                verificationService = verificationService,
                roleService = roleService,
                config = config,
                messages = messages,
                token = config.getDiscordToken()
            )
            discord.start()
            
            plugin.logger.info(messages.getLogPluginReloadSuccess())
        } catch (e: Exception) {
            plugin.logger.severe(messages.getLogPluginReloadFailed().replace("{error}", e.message ?: "Unknown"))
            throw e
        }
    }
    
    /**
     * Shutdown all plugin components
     */
    fun shutdown() {
        try {
            // Stop Discord bot
            if (::discord.isInitialized) {
                discord.stop()
                Thread.sleep(1000)
            }
        } catch (e: Exception) {
            if (::config.isInitialized && config.getDebug()) {
                plugin.logger.warning(messages.getLogDiscordBotStopError().replace("{error}", e.message ?: "Unknown"))
            }
        }
        
        try {
            // Close database
            if (::database.isInitialized) {
                database.close()
            }
        } catch (e: Exception) {
            if (::config.isInitialized && config.getDebug()) {
                plugin.logger.warning("Error closing database: ${e.message}")
            }
        }
        
        plugin.logger.info(messages.getLogPluginDisabled())
    }
}
