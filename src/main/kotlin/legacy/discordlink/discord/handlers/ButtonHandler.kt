package legacy.discordlink.discord.handlers

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.config.MessageManager
import legacy.discordlink.services.LinkingService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.awt.Color

/**
 * Handler for button interactions
 * 
 * Handles link button and check accounts button
 */
class ButtonHandler(
    private val linkingService: LinkingService,
    private val config: ConfigManager,
    private val messages: MessageManager
) : ListenerAdapter() {
    
    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            "link_minecraft" -> handleLinkButton(event)
            "check_accounts" -> handleCheckAccountsButton(event)
        }
    }
    
    /**
     * Handle link Minecraft button
     */
    private fun handleLinkButton(event: ButtonInteractionEvent) {
        val codeInput = TextInput.create(
            "minecraft_code",
            config.getDiscordModalInputLabel(),
            TextInputStyle.SHORT
        )
            .setPlaceholder(config.getDiscordModalInputPlaceholder())
            .setRequiredRange(4, 4)
            .build()
        
        val modal = Modal.create("code_input_modal", config.getDiscordModalTitle())
            .addActionRow(codeInput)
            .build()
        
        event.replyModal(modal).queue()
    }
    
    /**
     * Handle check accounts button
     */
    private fun handleCheckAccountsButton(event: ButtonInteractionEvent) {
        val discordId = event.user.id
        val linkedAccounts = linkingService.getLinkedAccounts(discordId)
        
        if (linkedAccounts.isEmpty()) {
            event.reply(
                "${messages.getCheckAccountsNoAccountsTitle()}\n\n${messages.getCheckAccountsNoAccountsMessage()}"
            )
                .setEphemeral(true)
                .queue()
            return
        }
        
        val embed = EmbedBuilder()
            .setTitle(messages.getCheckAccountsTitle())
            .setDescription(messages.getCheckAccountsDescription())
            .setColor(Color.GREEN)
        
        linkedAccounts.forEachIndexed { index, linkInfo ->
            embed.addField(
                messages.getCheckAccountsAccountNumber().replace("{number}", (index + 1).toString()),
                "🎮 **${linkInfo.minecraftName}**",
                true
            )
        }
        
        embed.setFooter(
            messages.getCheckAccountsFooter()
                .replace("{count}", linkedAccounts.size.toString())
                .replace("{max}", config.getMaxAccountsPerDiscord().toString())
        )
        
        event.replyEmbeds(embed.build())
            .setEphemeral(true)
            .queue()
    }
}
