package legacy.discordlink

import legacy.discordlink.core.PluginCore
import org.bukkit.plugin.java.JavaPlugin

/**
 * Main plugin class
 * 
 * Delegates all functionality to PluginCore for better organization
 */
class Main : JavaPlugin() {
    
    private lateinit var core: PluginCore
    
    override fun onEnable() {
        try {
            // Initialize plugin core
            core = PluginCore(this)
            
            if (!core.initialize()) {
                logger.severe("Failed to initialize plugin")
                server.pluginManager.disablePlugin(this)
                return
            }
            
        } catch (e: Exception) {
            logger.severe("Fatal error during plugin initialization: ${e.message}")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }
    
    override fun onDisable() {
        if (::core.isInitialized) {
            core.shutdown()
        }
    }
    
    /**
     * Reload plugin configuration and components
     * Called by /mclink reload command
     */
    fun reloadPlugin() {
        if (::core.isInitialized) {
            core.reload()
        } else {
            throw IllegalStateException("Plugin core not initialized")
        }
    }
    
    /**
     * Get plugin core instance
     * Useful for accessing services from other components
     */
    fun getCore(): PluginCore {
        if (!::core.isInitialized) {
            throw IllegalStateException("Plugin core not initialized")
        }
        return core
    }
}
