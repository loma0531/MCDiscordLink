package legacy.discordlink.config

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
    
    fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile)
    }

    fun getDiscordToken(): String = config.getString("discord.token", "")!!.trim()

    fun isDatabaseEnabled(): Boolean = config.getBoolean("database.enabled", true)
    fun getDatabaseHost(): String = config.getString("database.host", "localhost")!!
    fun getDatabasePort(): Int = config.getInt("database.port", 3306)
    fun getDatabaseName(): String = config.getString("database.database", "mcdiscord")!!
    fun getDatabaseUsername(): String = config.getString("database.username", "root")!!
    fun getDatabasePassword(): String = config.getString("database.password", "")!!
    fun getDatabaseUseTimezone(): Boolean = config.getBoolean("database.use-timezone", false)
    fun getDatabaseTimezone(): String = config.getString("database.timezone", "Asia/Bangkok")!!
    fun getMaxPool(): Int = config.getInt("database.connection-pool.maximum-pool-size", 10)
    fun getMinIdle(): Int = config.getInt("database.connection-pool.minimum-idle", 2)
    fun getConnTimeout(): Long = config.getLong("database.connection-pool.connection-timeout", 30000)
    fun getIdleTimeout(): Long = config.getLong("database.connection-pool.idle-timeout", 600000)
    fun getMaxLifetime(): Long = config.getLong("database.connection-pool.max-lifetime", 1800000)
    fun getUseSSL(): Boolean = config.getBoolean("database.useSSL", false)
    fun getAllowPKR(): Boolean = config.getBoolean("database.allowPublicKeyRetrieval", true)
    
    fun getMaxAccountsPerDiscord(): Int = config.getInt("limit-setting.max-minecraft-accounts-per-discord-account", 10)
    fun getCodeExpiryMinutes(): Int = config.getInt("code-expiry-minutes", 30)
    fun getDebug(): Boolean = config.getBoolean("advanced.debug", false)
    
    // Kick message configurations
    fun getKickTitle(): String = config.getString("kick-message.title", "&cYou must link your Discord account to play!")!!
    fun getKickLinkingCode(): String = config.getString("kick-message.linking-code", "&eYour linking code: &a&l{code}")!!
    fun getKickDescription(): String = config.getString("kick-message.description", "&7How to link your account:")!!
    fun getKickSteps(): List<String> = config.getStringList("kick-message.steps")
    fun getKickExpiry(): String = config.getString("kick-message.expiry-warning", "&cExpires in &e{minutes} minutes&c!")!!
    fun getKickError(): String = config.getString("kick-message.error-message", "&cFailed to generate code. Contact admin.")!!
    
    // Discord embed configurations
    fun getDiscordEmbedTitle(): String = config.getString("discord-embed.title", "🔗 Link Minecraft Account")!!
    fun getDiscordEmbedColor(): String = config.getString("discord-embed.color", "#00FF00")!!
    fun getDiscordEmbedDescription(): String = config.getString("discord-embed.description", "Click button to link")!!
    fun getDiscordButtonLabel(): String = config.getString("discord-embed.button-text", "Link Account")!!
    fun getDiscordButtonEmoji(): String = config.getString("discord-embed.button-emoji", "🎮")!!
    fun getDiscordButtonColor(): String = config.getString("discord-embed.button-color", "PRIMARY")!!
    fun getDiscordAddEmptyLines(): Boolean = config.getBoolean("discord-embed.add-empty-lines", true)
    
    // Discord modal configurations
    fun getDiscordModalTitle(): String = config.getString("discord-modal.title", "Enter Code")!!
    fun getDiscordModalInputLabel(): String = config.getString("discord-modal.input-label", "4-Digit Code")!!
    fun getDiscordModalInputPlaceholder(): String = config.getString("discord-modal.input-placeholder", "Enter code")!!
    
    // Discord response configurations
    fun getDiscordInvalidCode(): String = config.getString("discord-responses.invalid-code", "❌ Invalid code")!!
    fun getDiscordCodeExpired(): String = config.getString("discord-responses.code-expired", "❌ Code expired")!!
    fun getDiscordAccountLimit(): String = config.getString("discord-responses.account-limit", "❌ Account limit reached")!!
    fun getDiscordAlreadyLinked(): String = config.getString("discord-responses.already-linked", "❌ Already linked")!!
    fun getDiscordSuccess(): String = config.getString("discord-responses.success-message", "✅ Success!")!!
    fun getDiscordError(): String = config.getString("discord-responses.link-error", "❌ Error occurred")!!
    fun getDiscordNoPermission(): String = config.getString("discord-responses.no-permission", "❌ No permission")!!
    fun getDiscordGuildOnly(): String = config.getString("discord-responses.guild-only", "❌ Server only")!!
    
    // Logging settings
    fun getLoggingEnabled(): Boolean = config.getBoolean("logging.enabled", true)
    fun getLogChannelId(): String = config.getString("logging.log-channel-id", "")!!
    fun getLogSuccessfulLinks(): Boolean = getLoggingEnabled() // Simplified - use main logging setting
    
    // Role settings
    fun getGiveRoleAfterLink(): Boolean = config.getBoolean("give-role-after-link.enabled", true)
    fun getLinkedRoleId(): String = config.getString("give-role-after-link.give-role-id", "")!!
    
    // Legacy methods for backward compatibility
    fun getDiscordShowDetailedSteps(): Boolean = getDiscordEmbedFields().isNotEmpty()
    fun getDiscordEmbedFieldTitle(): String = {
        val fields = getDiscordEmbedFields()
        if (fields.isNotEmpty()) {
            fields[0]["name"]?.toString() ?: "📋 Steps"
        } else "📋 Steps"
    }()
    fun getDiscordEmbedFieldValue(): String = {
        val fields = getDiscordEmbedFields()
        if (fields.isNotEmpty()) {
            fields[0]["value"]?.toString() ?: "Follow instructions"
        } else "Follow instructions"
    }()
    
    // Kick message legacy methods
    fun getKickAddEmptyLines(): Boolean = true // Always add empty lines for better readability
    fun getKickCodePrefix(): String = "&eYour linking code: "
    fun getKickCodeColor(): String = "&a&l"
    fun getKickStepsTitle(): String = getKickDescription()
    
    // Advanced settings
    fun getKickDelayTicks(): Long = config.getLong("advanced.kick-delay-ticks", 5L)
    fun getOpsBypassLinking(): Boolean = config.getBoolean("advanced.ops-bypass-linking", true)
    
    // Verification settings
    fun getVerificationEnabled(): Boolean = config.getBoolean("discoed-account-verification.enabled", true)
    fun getMinAccountAgeDays(): Int = config.getInt("discoed-account-verification.min-account-age-days", 7)
    fun getMinServerJoin(): String = config.getString("discoed-account-verification.min-server-join", "10m")!!
    
    // Parse time string (10m, 10h, 10d, etc.) to minutes
    fun getMinServerJoinMinutes(): Int {
        val timeStr = getMinServerJoin()
        return parseTimeToMinutes(timeStr)
    }
    
    private fun parseTimeToMinutes(timeStr: String): Int {
        val regex = Regex("(\\d+)([mhdwoy])")
        val match = regex.find(timeStr.lowercase()) ?: return 10
        
        val amount = match.groupValues[1].toIntOrNull() ?: 10
        val unit = match.groupValues[2]
        
        return when (unit) {
            "m" -> amount
            "h" -> amount * 60
            "d" -> amount * 60 * 24
            "w" -> amount * 60 * 24 * 7
            "mo" -> amount * 60 * 24 * 30
            "y" -> amount * 60 * 24 * 365
            else -> amount
        }
    }
    

    
    // New config structure support
    fun getLimitSettingEnabled(): Boolean = config.getBoolean("limit-setting.enabled", true)
    
    // Discord embed field support
    fun getDiscordEmbedFields(): List<Map<*, *>> {
        return config.getMapList("discord-embed.fields")
    }
    
    // Placeholder replacement helper
    fun replacePlaceholders(text: String, placeholders: Map<String, String>): String {
        var result = text
        placeholders.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
    
    // Utility function to convert color codes to Components
    fun parseColoredText(text: String): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text)
    }
    

}
