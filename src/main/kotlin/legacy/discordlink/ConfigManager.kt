package legacy.discordlink

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {
    private val configFile = File(plugin.dataFolder, "config.yml")
    private var config: FileConfiguration

    init {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        config = YamlConfiguration.loadConfiguration(configFile)
    }

    fun getDiscordToken(): String = config.getString("discord.token", "")!!.trim()

    fun isDatabaseEnabled(): Boolean = config.getBoolean("database.enabled", true)
    fun getDatabaseHost(): String = config.getString("database.host", "localhost")!!
    fun getDatabasePort(): Int = config.getInt("database.port", 3306)
    fun getDatabaseName(): String = config.getString("database.database", "mcdiscord")!!
    fun getDatabaseUsername(): String = config.getString("database.username", "root")!!
    fun getDatabasePassword(): String = config.getString("database.password", "")!!
    fun getMaxPool(): Int = config.getInt("database.connection-pool.maximum-pool-size", 10)
    fun getMinIdle(): Int = config.getInt("database.connection-pool.minimum-idle", 2)
    fun getConnTimeout(): Long = config.getLong("database.connection-pool.connection-timeout", 30000)
    fun getIdleTimeout(): Long = config.getLong("database.connection-pool.idle-timeout", 600000)
    fun getMaxLifetime(): Long = config.getLong("database.connection-pool.max-lifetime", 1800000)
    fun getUseSSL(): Boolean = config.getBoolean("database.useSSL", false)
    fun getAllowPKR(): Boolean = config.getBoolean("database.allowPublicKeyRetrieval", true)
    
    fun getMaxAccountsPerDiscord(): Int = config.getInt("plugin.max-accounts-per-discord", 10)
    fun getCodeExpiryMinutes(): Int = config.getInt("plugin.code-expiry-minutes", 30)
    fun getDebug(): Boolean = config.getBoolean("plugin.debug", false)
    
    // Kick message configurations
    fun getKickTitle(): String = config.getString("kick-message.title", "&cYou must link your Discord account to play!")!!
    fun getKickCodePrefix(): String = config.getString("kick-message.code-prefix", "&eYour code: ")!!
    fun getKickCodeColor(): String = config.getString("kick-message.code-color", "&a&l")!!
    fun getKickStepsTitle(): String = config.getString("kick-message.steps-title", "&7Steps:")!!
    fun getKickSteps(): List<String> = config.getStringList("kick-message.steps")
    fun getKickExpiry(): String = config.getString("kick-message.expiry-warning", "&cExpires in &e{minutes} minutes&c!")!!
    fun getKickError(): String = config.getString("kick-message.error-message", "&cFailed to generate code. Contact admin.")!!
    fun getKickAddEmptyLines(): Boolean = config.getBoolean("kick-message.add-empty-lines", true)
    
    // Discord embed configurations
    fun getDiscordEmbedTitle(): String = config.getString("discord-embed.title", "🔗 Link Minecraft Account")!!
    fun getDiscordEmbedColor(): String = config.getString("discord-embed.color", "#00FF00")!!
    fun getDiscordEmbedDescription(): String = config.getString("discord-embed.description", "Click button to link")!!
    fun getDiscordShowDetailedSteps(): Boolean = config.getBoolean("discord-embed.show-detailed-steps", true)
    fun getDiscordEmbedFieldTitle(): String = config.getString("discord-embed.field-title", "📋 Steps")!!
    fun getDiscordEmbedFieldValue(): String = config.getString("discord-embed.field-description", "Follow instructions")!!
    fun getDiscordButtonLabel(): String = config.getString("discord-embed.button-text", "Link Account")!!
    fun getDiscordButtonEmoji(): String = config.getString("discord-embed.button-emoji", "🎮")!!
    fun getDiscordButtonColor(): String = config.getString("discord-embed.button-color", "PRIMARY")!!
    
    // Discord modal configurations
    fun getDiscordModalTitle(): String = config.getString("discord-modal.title", "Enter Code")!!
    fun getDiscordModalInputLabel(): String = config.getString("discord-modal.input-label", "4-Digit Code")!!
    fun getDiscordModalInputPlaceholder(): String = config.getString("discord-modal.input-placeholder", "Enter code")!!
    
    // Discord response configurations
    fun getDiscordInvalidCode(): String = config.getString("discord-responses.invalid-code", "❌ Invalid code")!!
    fun getDiscordCodeExpired(): String = config.getString("discord-responses.code-expired", "❌ Code expired")!!
    fun getDiscordAccountLimit(): String = config.getString("discord-responses.account-limit", "❌ Account limit reached")!!
    fun getDiscordSuccess(): String = config.getString("discord-responses.success-message", "✅ Success!")!!
    fun getDiscordError(): String = config.getString("discord-responses.link-error", "❌ Error occurred")!!
    fun getDiscordNoPermission(): String = config.getString("discord-responses.no-permission", "❌ No permission")!!
    fun getDiscordGuildOnly(): String = config.getString("discord-responses.guild-only", "❌ Server only")!!
    
    // Advanced settings
    fun getKickDelayTicks(): Long = config.getLong("advanced.kick-delay-ticks", 5L)
    fun getOpsBypassLinking(): Boolean = config.getBoolean("advanced.ops-bypass-linking", true)
    fun getLogSuccessfulLinks(): Boolean = config.getBoolean("advanced.log-successful-links", true)
    
    // Utility function to convert color codes to Components
    fun parseColoredText(text: String): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text)
    }
}
