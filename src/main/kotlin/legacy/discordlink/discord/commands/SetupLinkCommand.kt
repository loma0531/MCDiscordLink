package legacy.discordlink.discord.commands

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.config.MessageManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color

/**
 * Handler for /setup-link-discord command
 * 
 * Creates the linking interface with buttons
 */
class SetupLinkCommand(
    private val config: ConfigManager,
    private val messages: MessageManager
) : ListenerAdapter() {
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "setup-link-discord") return
        
        // Check if command is used in a guild
        if (event.guild == null) {
            event.reply(config.getDiscordGuildOnly()).setEphemeral(true).queue()
            return
        }
        
        // Check if user has admin permissions
        val member = event.member
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply(config.getDiscordNoPermission()).setEphemeral(true).queue()
            return
        }
        
        // Create embed
        val embed = createLinkingEmbed()
        
        // Create buttons
        val linkButton = createLinkButton()
        val checkButton = createCheckButton()
        
        // Send message
        event.replyEmbeds(embed.build())
            .addActionRow(linkButton, checkButton)
            .queue()
    }
    
    /**
     * Create the main linking embed
     */
    private fun createLinkingEmbed(): EmbedBuilder {
        val embed = EmbedBuilder()
            .setTitle(config.getDiscordEmbedTitle())
            .setDescription(buildDescription())
            .setColor(Color.decode(config.getDiscordEmbedColor()))
        
        // Add detailed steps field if enabled
        if (config.getDiscordShowDetailedSteps()) {
            embed.addField(
                config.getDiscordEmbedFieldTitle(),
                config.getDiscordEmbedFieldValue(),
                false
            )
        }
        
        return embed
    }
    
    /**
     * Build embed description with placeholders
     */
    private fun buildDescription(): String {
        var description = config.getDiscordEmbedDescription()
            .replace("{max}", config.getMaxAccountsPerDiscord().toString())
            .replace("{min-account-age-days}", config.getMinAccountAgeDays().toString())
            .replace("{min-server-join}", config.getMinServerJoin())
        
        return description
    }
    
    /**
     * Create link button
     */
    private fun createLinkButton(): Button {
        val buttonStyle = when (config.getDiscordButtonColor().uppercase()) {
            "SECONDARY" -> Button.secondary("link_minecraft", config.getDiscordButtonLabel())
            "SUCCESS" -> Button.success("link_minecraft", config.getDiscordButtonLabel())
            "DANGER" -> Button.danger("link_minecraft", config.getDiscordButtonLabel())
            else -> Button.primary("link_minecraft", config.getDiscordButtonLabel())
        }
        
        return buttonStyle.withEmoji(Emoji.fromUnicode(config.getDiscordButtonEmoji()))
    }
    
    /**
     * Create check accounts button
     */
    private fun createCheckButton(): Button {
        return Button.secondary("check_accounts", messages.getCheckAccountsButton())
            .withEmoji(Emoji.fromUnicode("🔍"))
    }
}
