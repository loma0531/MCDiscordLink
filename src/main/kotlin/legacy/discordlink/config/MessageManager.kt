package legacy.discordlink.config

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MessageManager(private val plugin: JavaPlugin) {
    private val messagesFile = File(plugin.dataFolder, "messages.yml")
    private var messages: FileConfiguration

    init {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdirs()
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }
    
    fun reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile)
    }
    
    private fun getMessage(path: String, default: String): String {
        return messages.getString(path, default) ?: default
    }
    
    // Discord Bot Messages
    fun getCheckAccountsButton(): String = getMessage("discord-bot.check-accounts-button", "เช็คบัญชีของฉัน")
    
    // Status command messages (formerly mc-check)
    fun getMcCheckTitle(): String = getMessage("discord-bot.status.title", "🔍 ตรวจสอบการเชื่อมโยงบัญชี")
    fun getMcCheckErrorNoUser(): String = getMessage("discord-bot.status.error-no-user", "❌ กรุณาระบุชื่อผู้เล่น")
    fun getMcCheckDiscordUserLabel(): String = getMessage("discord-bot.status.discord-user-label", "👤 Discord User")
    fun getMcCheckDiscordIdLabel(): String = getMessage("discord-bot.status.discord-id-label", "🆔 Discord ID")
    fun getMcCheckStatusLabel(): String = getMessage("discord-bot.status.status-label", "สถานะ")
    fun getMcCheckStatusNotLinked(): String = getMessage("discord-bot.status.status-not-linked", "❌ ไม่มีบัญชี Minecraft ที่เชื่อมโยง")
    fun getMcCheckStatusLinked(): String = getMessage("discord-bot.status.status-linked", "✅ เชื่อมโยงแล้ว")
    fun getMcCheckStatusLinkedCount(): String = getMessage("discord-bot.status.status-linked-count", "✅ เชื่อมโยงแล้ว ({count})")
    fun getMcCheckStatusNotLinkedShort(): String = getMessage("discord-bot.status.status-not-linked-short", "❌ ไม่เชื่อมโยง")
    fun getMcCheckMinecraftAccountsLabel(): String = getMessage("discord-bot.status.minecraft-accounts-label", "🎮 บัญชี Minecraft ที่เชื่อมโยง ({count})")
    fun getMcCheckFirstLinkDateLabel(): String = getMessage("discord-bot.status.first-link-date-label", "📅 เชื่อมโยงครั้งแรกเมื่อ")
    fun getMcCheckMinecraftLabel(): String = getMessage("discord-bot.status.minecraft-label", "🎮 Minecraft")
    fun getMcCheckUuidLabel(): String = getMessage("discord-bot.status.uuid-label", "🆔 UUID")
    fun getMcCheckDiscordLabel(): String = getMessage("discord-bot.status.discord-label", "👤 Discord")
    fun getMcCheckLinkedAtLabel(): String = getMessage("discord-bot.status.linked-at-label", "📅 เชื่อมโยงเมื่อ")
    fun getMcCheckOtherAccountsLabel(): String = getMessage("discord-bot.status.other-accounts-label", "🔗 บัญชีอื่นที่เชื่อมโยง")
    fun getMcCheckStatusLinkedNoDetails(): String = getMessage("discord-bot.status.status-linked-no-details", "⚠️ เชื่อมโยงแล้ว (ไม่พบรายละเอียด)")
    fun getMcCheckStatusNotLinkedMc(): String = getMessage("discord-bot.status.status-not-linked-mc", "❌ ยังไม่ได้เชื่อมโยง")
    fun getMcCheckPendingCodeLabel(): String = getMessage("discord-bot.status.pending-code-label", "⏳ รหัสรอการยืนยัน")
    fun getMcCheckNotFoundMcPlayer(): String = getMessage("discord-bot.status.not-found-mc-player", "⚠️ ไม่พบผู้เล่น Minecraft")
    fun getMcCheckFoundSimilarDiscord(): String = getMessage("discord-bot.status.found-similar-discord", "🔍 พบ Discord users ที่คล้ายกัน:")
    fun getMcCheckNotFoundBoth(): String = getMessage("discord-bot.status.not-found-both", "❌ ไม่พบ")
    fun getMcCheckNotFoundMessage(): String = getMessage("discord-bot.status.not-found-message", "ไม่พบผู้เล่น Minecraft หรือ Discord user: {user}")
    
    // Check accounts button messages
    fun getCheckAccountsNoAccountsTitle(): String = getMessage("discord-bot.check-accounts.no-accounts-title", "❌ **ไม่พบบัญชีที่ผูกไว้**")
    fun getCheckAccountsNoAccountsMessage(): String = getMessage("discord-bot.check-accounts.no-accounts-message", "คุณยังไม่ได้ผูกบัญชี Minecraft กับ Discord นี้")
    fun getCheckAccountsTitle(): String = getMessage("discord-bot.check-accounts.title", "🔗 บัญชี Minecraft ที่ผูกไว้")
    fun getCheckAccountsDescription(): String = getMessage("discord-bot.check-accounts.description", "บัญชี Discord ของคุณผูกกับบัญชี Minecraft ดังนี้:")
    fun getCheckAccountsAccountNumber(): String = getMessage("discord-bot.check-accounts.account-number", "บัญชีที่ {number}")
    fun getCheckAccountsFooter(): String = getMessage("discord-bot.check-accounts.footer", "รวม {count} บัญชี | สูงสุด {max} บัญชี")
    
    // Verification messages
    fun getVerificationAccountAgeError(): String = getMessage("discord-bot.verification.account-age-error", "❌ บัญชี Discord ของคุณต้องอายุอย่างน้อย {min} วัน")
    fun getVerificationServerJoinError(): String = getMessage("discord-bot.verification.server-join-error", "❌ คุณต้องอยู่ในเซิร์ฟเวอร์อย่างน้อย {min} นาที")
    
    // Minecraft command messages
    fun getCmdNoPermission(): String = getMessage("minecraft-commands.no-permission", "§cYou don't have permission to use this command.")
    fun getCmdRequiredPermission(): String = getMessage("minecraft-commands.required-permission", "§7Required permission: mcdiscordlink.admin")
    fun getCmdUsageMain(): String = getMessage("minecraft-commands.usage-main", "§eUsage: /mclink <reload|check|unlink> [player]")
    fun getCmdAvailableCommands(): String = getMessage("minecraft-commands.available-commands", "§7Available commands:")
    fun getCmdReload(): String = getMessage("minecraft-commands.cmd-reload", "§7  /mclink reload - Reload plugin")
    fun getCmdCheck(): String = getMessage("minecraft-commands.cmd-check", "§7  /mclink check <player> - Check player link status")
    fun getCmdUnlink(): String = getMessage("minecraft-commands.cmd-unlink", "§7  /mclink unlink - Unlink your account (players only)")
    fun getCmdReloadSuccess(): String = getMessage("minecraft-commands.reload-success", "§a✅ MCDiscordLink reloaded successfully!")
    fun getCmdReloadFailed(): String = getMessage("minecraft-commands.reload-failed", "§c❌ Failed to reload: {error}")
    fun getCmdCheckUsage(): String = getMessage("minecraft-commands.check-usage", "§eUsage: /mclink check <player>")
    fun getCmdCheckPlayerNotFound(): String = getMessage("minecraft-commands.check-player-not-found", "§cPlayer not found: {player}")
    fun getCmdCheckLinked(): String = getMessage("minecraft-commands.check-linked", "§a✅ {player} is linked:")
    fun getCmdCheckDiscordId(): String = getMessage("minecraft-commands.check-discord-id", "§7  Discord ID: {discord-id}")
    fun getCmdCheckDiscordName(): String = getMessage("minecraft-commands.check-discord-name", "§7  Discord Name: {discord-name}")
    fun getCmdCheckLinkedAt(): String = getMessage("minecraft-commands.check-linked-at", "§7  Linked At: {date}")
    fun getCmdCheckLinkedNoDetails(): String = getMessage("minecraft-commands.check-linked-no-details", "§a✅ {player} is linked (details unavailable)")
    fun getCmdCheckNotLinked(): String = getMessage("minecraft-commands.check-not-linked", "§c❌ {player} is not linked")
    fun getCmdCheckPendingCode(): String = getMessage("minecraft-commands.check-pending-code", "§e⏳ Pending code: {code}")
    fun getCmdUnlinkPlayersOnly(): String = getMessage("minecraft-commands.unlink-players-only", "§cThis command can only be used by players!")
    fun getCmdUnlinkNotLinked(): String = getMessage("minecraft-commands.unlink-not-linked", "§c❌ Your account is not linked to Discord!")
    fun getCmdUnlinkSuccess(): String = getMessage("minecraft-commands.unlink-success", "§a✅ การเชื่อมโยง Discord ของคุณถูกยกเลิกแล้ว!")
    fun getCmdUnlinkWarning(): String = getMessage("minecraft-commands.unlink-warning", "§e⚠️ คุณจะถูกเตะออกจากเซิร์ฟเวอร์ใน 5 วินาที...")
    fun getCmdUnlinkCountdown(): String = getMessage("minecraft-commands.unlink-countdown", "§c{seconds}...")
    fun getCmdUnlinkKickMessage(): String = getMessage("minecraft-commands.unlink-kick-message", "§cการเชื่อมโยง Discord ของคุณถูกยกเลิกแล้ว")
    fun getCmdUnlinkFailed(): String = getMessage("minecraft-commands.unlink-failed", "§c❌ ไม่สามารถยกเลิกการเชื่อมโยงได้")
    fun getCmdUnknownCommand(): String = getMessage("minecraft-commands.unknown-command", "§cUnknown subcommand: {command}")
    
    // System log messages
    fun getLogPluginEnabled(): String = getMessage("system-logs.plugin-enabled", "MCDiscordLink enabled - Use /setup-link-discord in Discord")
    fun getLogPluginDisabled(): String = getMessage("system-logs.plugin-disabled", "MCDiscordLink disabled")
    fun getLogPluginReloadSuccess(): String = getMessage("system-logs.plugin-reload-success", "MCDiscordLink reloaded successfully")
    fun getLogPluginReloadFailed(): String = getMessage("system-logs.plugin-reload-failed", "Failed to reload plugin: {error}")
    fun getLogDatabaseConnected(): String = getMessage("system-logs.database-connected", "Database connected successfully")
    fun getLogDatabaseConnectionFailed(): String = getMessage("system-logs.database-connection-failed", "Database connection failed: {error}")
    fun getLogDatabaseDisabled(): String = getMessage("system-logs.database-disabled", "Database is disabled in config — plugin requires database")
    fun getLogDatabaseInitFailed(): String = getMessage("system-logs.database-init-failed", "Database initialization failed — disabling plugin")
    fun getLogDiscordBotStarted(): String = getMessage("system-logs.discord-bot-started", "Discord bot started successfully")
    fun getLogDiscordBotJdaConflict(): String = getMessage("system-logs.discord-bot-jda-conflict", "JDA class loading conflict! Server restart required.")
    fun getLogDiscordBotNoReload(): String = getMessage("system-logs.discord-bot-no-reload", "Cannot reload plugins that use JDA. Please restart the server.")
    fun getLogDiscordBotStartFailed(): String = getMessage("system-logs.discord-bot-start-failed", "Failed to start Discord bot: {error}")
    fun getLogDiscordBotStopError(): String = getMessage("system-logs.discord-bot-stop-error", "Error stopping JDA: {error}")
    fun getLogDiscordBotClassConflict(): String = getMessage("system-logs.discord-bot-class-conflict", "Class loading conflict detected! Please restart the server instead of using reload.")
    fun getLogDiscordBotClassConflictHelp(): String = getMessage("system-logs.discord-bot-class-conflict-help", "This usually happens when reloading plugins with JDA. Use '/stop' then start the server.")
    fun getLogCommandMclinkRegistered(): String = getMessage("system-logs.command-mclink-registered", "✅ Command /mclink registered successfully")
    fun getLogCommandMclinkFailed(): String = getMessage("system-logs.command-mclink-failed", "❌ Failed to register /mclink command - command not found in plugin.yml")
    fun getLogCommandRegistrationError(): String = getMessage("system-logs.command-registration-error", "❌ Exception while registering commands: {error}")
    fun getLogLinkSuccessConsole(): String = getMessage("system-logs.link-success-console", "Successfully linked {minecraft} ({uuid}) with Discord {discord} ({discord-id})")
    fun getLogLinkSuccessEmbedTitle(): String = getMessage("system-logs.link-success-embed-title", "🔗 Account Linked")
    fun getLogLinkSuccessEmbedMinecraft(): String = getMessage("system-logs.link-success-embed-minecraft", "Minecraft")
    fun getLogLinkSuccessEmbedDiscord(): String = getMessage("system-logs.link-success-embed-discord", "Discord")
    fun getLogLinkSuccessEmbedTotal(): String = getMessage("system-logs.link-success-embed-total", "Total Accounts")
    fun getLogRoleAddedSuccess(): String = getMessage("system-logs.role-added-success", "Added role {role} to {discord}")
    fun getLogRoleAddFailed(): String = getMessage("system-logs.role-add-failed", "Failed to add role to {discord}: {error}")
    fun getLogRoleNotFound(): String = getMessage("system-logs.role-not-found", "Role with ID {role-id} not found")
    fun getLogRoleAddError(): String = getMessage("system-logs.role-add-error", "Error adding role to {discord}: {error}")
    fun getLogChannelNotFound(): String = getMessage("system-logs.log-channel-not-found", "Log channel with ID {channel-id} not found")
    fun getLogMessageError(): String = getMessage("system-logs.log-message-error", "Error sending log message: {error}")
    fun getLogMclinkCommandExecuted(): String = getMessage("system-logs.mclink-command-executed", "MCLink command executed by {sender} with args: {args}")
    fun getLogReloadCommandFailed(): String = getMessage("system-logs.reload-command-failed", "Reload failed: {error}")
    
    // Debug messages
    fun getDebugPlayerLinkedError(): String = getMessage("system-logs.debug-player-linked-error", "isPlayerLinked error: {error}")
    fun getDebugGenerateCodeError(): String = getMessage("system-logs.debug-generate-code-error", "generateAndStoreCode error: {error}")
    fun getDebugVerifyCodeError(): String = getMessage("system-logs.debug-verify-code-error", "verifyCodeAndGetPlayer error: {error}")
    fun getDebugAccountCountError(): String = getMessage("system-logs.debug-account-count-error", "getAccountCountForDiscord error: {error}")
    fun getDebugLinkAccountError(): String = getMessage("system-logs.debug-link-account-error", "linkAccount error: {error}")
    fun getDebugGetLinkInfoError(): String = getMessage("system-logs.debug-get-link-info-error", "getLinkInfo error: {error}")
    fun getDebugPendingCodeError(): String = getMessage("system-logs.debug-pending-code-error", "getPendingCode error: {error}")
    fun getDebugUnlinkAccountError(): String = getMessage("system-logs.debug-unlink-account-error", "unlinkAccount error: {error}")
    fun getDebugGetAccountsError(): String = getMessage("system-logs.debug-get-accounts-error", "getAccountsForDiscord error: {error}")
    fun getDebugCodeGenerationFailed(): String = getMessage("system-logs.debug-code-generation-failed", "Failed to generate unique code after 100 attempts")
    
    // Utility function for placeholder replacement
    fun replacePlaceholders(text: String, placeholders: Map<String, String>): String {
        var result = text
        placeholders.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}