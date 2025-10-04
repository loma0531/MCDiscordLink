package legacy.discordlink

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.EmbedBuilder
import java.awt.Color

class DiscordBot(
    private val logger: java.util.logging.Logger, 
    private val db: DatabaseManager,
    private val cfg: ConfigManager,
    private val token: String
) : ListenerAdapter() {
    private var jda: JDA? = null

    fun start() {
        try {
            jda = JDABuilder.createDefault(token)
                .addEventListeners(this)
                .build()
                
            jda?.awaitReady()
            
            // Register slash command
            jda?.updateCommands()?.addCommands(
                Commands.slash("setup-link-discord", "Setup Discord-Minecraft linking system (Admin only)")
            )?.queue()
            
            logger.info("Discord bot started successfully")
        } catch (e: Exception) {
            logger.severe("Failed to start Discord bot: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "setup-link-discord") {
            // Check if command is used in a guild
            if (event.guild == null) {
                event.reply(cfg.getDiscordGuildOnly()).setEphemeral(true).queue()
                return
            }
            
            // Check if user has admin permissions
            val member = event.member
            if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
                event.reply(cfg.getDiscordNoPermission()).setEphemeral(true).queue()
                return
            }

            // Create embed with button
            val embed = EmbedBuilder()
                .setTitle(cfg.getDiscordEmbedTitle())
                .setDescription(
                    cfg.getDiscordEmbedDescription()
                        .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                        .replace("{expiry-minutes}", cfg.getCodeExpiryMinutes().toString())
                )
                .setColor(Color.decode(cfg.getDiscordEmbedColor()))
            
            // Add detailed steps field if enabled
            if (cfg.getDiscordShowDetailedSteps()) {
                embed.addField(
                    cfg.getDiscordEmbedFieldTitle(),
                    cfg.getDiscordEmbedFieldValue(),
                    false
                )
            }
            
            // Parse button style
            val buttonStyle = when (cfg.getDiscordButtonColor().uppercase()) {
                "SECONDARY" -> Button.secondary("link_minecraft", cfg.getDiscordButtonLabel())
                "SUCCESS" -> Button.success("link_minecraft", cfg.getDiscordButtonLabel())
                "DANGER" -> Button.danger("link_minecraft", cfg.getDiscordButtonLabel())
                else -> Button.primary("link_minecraft", cfg.getDiscordButtonLabel())
            }.withEmoji(Emoji.fromUnicode(cfg.getDiscordButtonEmoji()))
            
            event.replyEmbeds(embed.build())
                .addActionRow(buttonStyle)
                .queue()
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == "link_minecraft") {
            // Show modal for code input
            val codeInput = TextInput.create("minecraft_code", cfg.getDiscordModalInputLabel(), TextInputStyle.SHORT)
                .setPlaceholder(cfg.getDiscordModalInputPlaceholder())
                .setRequiredRange(4, 4)
                .build()
            
            val modal = Modal.create("code_input_modal", cfg.getDiscordModalTitle())
                .addActionRow(codeInput)
                .build()
            
            event.replyModal(modal).queue()
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId == "code_input_modal") {
            val code = event.getValue("minecraft_code")?.asString?.trim()
            
            if (code.isNullOrEmpty() || code.length != 4) {
                event.reply(cfg.getDiscordInvalidCode()).setEphemeral(true).queue()
                return
            }

            // Verify code and get player info
            val playerInfo = db.verifyCodeAndGetPlayer(code)
            if (playerInfo == null) {
                event.reply(cfg.getDiscordCodeExpired()).setEphemeral(true).queue()
                return
            }

            val (uuid, minecraftName) = playerInfo
            val discordId = event.user.id
            val discordName = event.user.name

            // Check account limit
            val currentCount = db.getAccountCountForDiscord(discordId)
            if (currentCount >= cfg.getMaxAccountsPerDiscord()) {
                event.reply(
                    cfg.getDiscordAccountLimit()
                        .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                ).setEphemeral(true).queue()
                return
            }

            // Link the account
            val success = db.linkAccount(uuid, minecraftName, discordId, discordName)
            if (success) {
                event.reply(
                    cfg.getDiscordSuccess()
                        .replace("{minecraft-name}", minecraftName)
                        .replace("{discord-name}", discordName)
                        .replace("{current-count}", (currentCount + 1).toString())
                        .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                ).setEphemeral(true).queue()
                
                if (cfg.getLogSuccessfulLinks()) {
                    logger.info("Successfully linked $minecraftName ($uuid) with Discord $discordName ($discordId)")
                }
            } else {
                event.reply(cfg.getDiscordError()).setEphemeral(true).queue()
            }
        }
    }

    fun stop() {
        try {
            jda?.shutdown()
        } catch (e: Exception) {
            logger.warning("Error stopping JDA: ${e.message}")
        }
    }
}
