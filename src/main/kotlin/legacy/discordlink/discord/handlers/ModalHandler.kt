package legacy.discordlink.discord.handlers

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.config.MessageManager
import legacy.discordlink.services.LinkingService
import legacy.discordlink.services.RoleService
import legacy.discordlink.services.VerificationService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.awt.Color

/**
 * Handler for modal interactions
 * 
 * Handles code input modal for account linking
 */
class ModalHandler(
    private val linkingService: LinkingService,
    private val verificationService: VerificationService,
    private val roleService: RoleService,
    private val config: ConfigManager,
    private val messages: MessageManager,
    private val logger: java.util.logging.Logger
) : ListenerAdapter() {
    
    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId != "code_input_modal") return
        
        val code = event.getValue("minecraft_code")?.asString?.trim()
        
        // Debug log
        if (config.getDebug()) {
            logger.info("Code input received: '$code' from user ${event.user.name} (${event.user.id})")
        }
        
        // Validate code format
        if (code.isNullOrEmpty() || code.length != 4) {
            event.reply(config.getDiscordInvalidCode()).setEphemeral(true).queue()
            return
        }
        
        // Validate code is numeric
        if (!code.all { it.isDigit() }) {
            event.reply(config.getDiscordInvalidCode()).setEphemeral(true).queue()
            return
        }
        
        // Check verification requirements
        val member = event.member
        val verificationError = verificationService.checkRequirements(event.user, member)
        if (verificationError != null) {
            event.reply(formatVerificationError(verificationError)).setEphemeral(true).queue()
            return
        }
        
        // Check if user can link more accounts
        val discordId = event.user.id
        if (!linkingService.canLinkMore(discordId)) {
            event.reply(
                config.getDiscordAccountLimit()
                    .replace("{max}", config.getMaxAccountsPerDiscord().toString())
            ).setEphemeral(true).queue()
            return
        }
        
        // Attempt to link account
        val result = linkingService.linkAccount(
            code = code,
            discordId = discordId,
            discordName = event.user.name
        )
        
        if (result.isSuccess) {
            handleSuccessfulLink(event, result.getOrNull()!!)
        } else {
            // Debug log for failure
            if (config.getDebug()) {
                logger.warning("Link failed for code '$code': ${result.exceptionOrNull()?.message}")
            }
            handleFailedLink(event, result.exceptionOrNull()?.message)
        }
    }
    
    /**
     * Handle successful account linking
     */
    private fun handleSuccessfulLink(event: ModalInteractionEvent, linkInfo: legacy.discordlink.database.models.LinkInfo) {
        val currentCount = linkingService.getAccountCount(linkInfo.discordId)
        
        // Send success message
        event.reply(
            config.getDiscordSuccess()
                .replace("{minecraft}", linkInfo.minecraftName)
                .replace("{discord}", linkInfo.discordName)
                .replace("{count}", currentCount.toString())
                .replace("{max}", config.getMaxAccountsPerDiscord().toString())
        ).setEphemeral(true).queue()
        
        // Give role if enabled
        val guild = event.guild
        val member = event.member
        if (guild != null && member != null) {
            roleService.giveLinkedRole(guild, member)
        }
        
        // Send log to channel if enabled
        if (config.getLoggingEnabled() && config.getLogChannelId().isNotEmpty()) {
            sendLogMessage(event, linkInfo, currentCount)
        }
        
        // Console log
        if (config.getLoggingEnabled()) {
            logger.info(
                messages.getLogLinkSuccessConsole()
                    .replace("{minecraft}", linkInfo.minecraftName)
                    .replace("{uuid}", linkInfo.minecraftUuid)
                    .replace("{discord}", linkInfo.discordName)
                    .replace("{discord-id}", linkInfo.discordId)
            )
        }
    }
    
    /**
     * Handle failed account linking
     */
    private fun handleFailedLink(event: ModalInteractionEvent, errorMessage: String?) {
        val message = when {
            errorMessage?.contains("expired", ignoreCase = true) == true -> config.getDiscordCodeExpired()
            errorMessage?.contains("limit", ignoreCase = true) == true -> {
                config.getDiscordAccountLimit().replace("{max}", config.getMaxAccountsPerDiscord().toString())
            }
            errorMessage?.contains("already linked", ignoreCase = true) == true -> config.getDiscordAlreadyLinked()
            errorMessage?.contains("Invalid", ignoreCase = true) == true -> config.getDiscordCodeExpired()
            else -> {
                if (config.getDebug()) {
                    logger.warning("Unhandled link error: $errorMessage")
                }
                config.getDiscordError()
            }
        }
        
        event.reply(message).setEphemeral(true).queue()
    }
    
    /**
     * Send log message to Discord channel
     */
    private fun sendLogMessage(event: ModalInteractionEvent, linkInfo: legacy.discordlink.database.models.LinkInfo, currentCount: Int) {
        try {
            val logChannel = event.jda.getTextChannelById(config.getLogChannelId())
            if (logChannel != null) {
                val logEmbed = EmbedBuilder()
                    .setTitle(messages.getLogLinkSuccessEmbedTitle())
                    .addField(messages.getLogLinkSuccessEmbedMinecraft(), linkInfo.minecraftName, true)
                    .addField(messages.getLogLinkSuccessEmbedDiscord(), "${event.user.name} (${event.user.id})", true)
                    .addField(
                        messages.getLogLinkSuccessEmbedTotal(),
                        "$currentCount/${config.getMaxAccountsPerDiscord()}",
                        true
                    )
                    .setColor(Color.GREEN)
                    .setTimestamp(java.time.Instant.now())
                    .build()
                
                logChannel.sendMessageEmbeds(logEmbed).queue()
            }
        } catch (e: Exception) {
            if (config.getDebug()) {
                logger.warning(messages.getLogMessageError().replace("{error}", e.message ?: "Unknown"))
            }
        }
    }
    
    /**
     * Format verification error message
     */
    private fun formatVerificationError(error: String): String {
        return when {
            error.contains("account", ignoreCase = true) -> {
                val parts = error.split("(current:")
                if (parts.size == 2) {
                    val current = parts[1].trim().removeSuffix(")")
                    messages.getVerificationAccountAgeError()
                        .replace("{min}", config.getMinAccountAgeDays().toString())
                        .replace("{current}", current)
                } else error
            }
            error.contains("server", ignoreCase = true) -> {
                val parts = error.split("(current:")
                if (parts.size == 2) {
                    val current = parts[1].trim().removeSuffix(")")
                    messages.getVerificationServerJoinError()
                        .replace("{min}", config.getMinServerJoinMinutes().toString())
                        .replace("{current}", current)
                } else error
            }
            else -> error
        }
    }
}
