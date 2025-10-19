package legacy.discordlink.discord.commands

import legacy.discordlink.config.MessageManager
import legacy.discordlink.services.LinkingService
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import org.bukkit.Bukkit
import java.awt.Color

/**
 * Handler for /status command
 * 
 * Checks link status for Discord users or Minecraft players
 */
class McCheckCommand(
    private val linkingService: LinkingService,
    private val messages: MessageManager
) : ListenerAdapter() {
    
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "status") return
        
        // Check if command is used in a guild
        if (event.guild == null) {
            event.reply(messages.getMcCheckErrorNoUser()).setEphemeral(true).queue()
            return
        }
        
        // Check if user has admin permissions
        val member = event.member
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply(messages.getMcCheckErrorNoUser()).setEphemeral(true).queue()
            return
        }
        
        val userInput = event.getOption("user")?.asString
        if (userInput.isNullOrBlank()) {
            event.reply(messages.getMcCheckErrorNoUser()).setEphemeral(true).queue()
            return
        }
        
        // Defer reply since operations might take time
        event.deferReply(true).queue()
        
        // Check if input is Discord ID or Minecraft username
        val isDiscordId = userInput.matches(Regex("\\d{17,19}")) || userInput.startsWith("<@")
        
        val embed = if (isDiscordId) {
            handleDiscordUserCheck(userInput, event)
        } else {
            handleMinecraftPlayerCheck(userInput)
        }
        
        event.hook.editOriginalEmbeds(embed.build()).queue()
    }
    
    /**
     * Handle Discord user check
     */
    private fun handleDiscordUserCheck(userInput: String, event: SlashCommandInteractionEvent): EmbedBuilder {
        val discordId = when {
            userInput.startsWith("<@") -> userInput.removePrefix("<@").removeSuffix(">").removePrefix("!")
            else -> userInput
        }
        
        val linkedAccounts = linkingService.getLinkedAccounts(discordId)
        val embed = EmbedBuilder()
            .setTitle(messages.getMcCheckTitle())
            .setTimestamp(java.time.Instant.now())
        
        if (linkedAccounts.isEmpty()) {
            embed.setColor(Color.RED)
            embed.addField(messages.getMcCheckDiscordIdLabel(), discordId, true)
            embed.addField(messages.getMcCheckStatusLabel(), messages.getMcCheckStatusNotLinked(), false)
        } else {
            embed.setColor(Color.GREEN)
            embed.addField(messages.getMcCheckDiscordIdLabel(), discordId, true)
            embed.addField(messages.getMcCheckStatusLabel(), messages.getMcCheckStatusLinked(), true)
            
            val accountsList = linkedAccounts.mapIndexed { index, linkInfo ->
                "**${index + 1}.** ${linkInfo.minecraftName} `(${linkInfo.minecraftUuid.substring(0, 8)}...)`"
            }.joinToString("\n")
            
            embed.addField(
                messages.getMcCheckMinecraftAccountsLabel().replace("{count}", linkedAccounts.size.toString()),
                accountsList,
                false
            )
            
            if (linkedAccounts.isNotEmpty()) {
                embed.addField(
                    messages.getMcCheckFirstLinkDateLabel(),
                    linkedAccounts[0].linkedAt.toString(),
                    true
                )
            }
        }
        
        return embed
    }
    
    /**
     * Handle Minecraft player check
     */
    private fun handleMinecraftPlayerCheck(playerName: String): EmbedBuilder {
        val player = Bukkit.getPlayerExact(playerName) ?: Bukkit.getOfflinePlayer(playerName)
        val embed = EmbedBuilder()
            .setTitle(messages.getMcCheckTitle())
            .setTimestamp(java.time.Instant.now())
        
        if (!player.hasPlayedBefore() && !player.isOnline) {
            embed.setColor(Color.RED)
            embed.addField(
                messages.getMcCheckNotFoundBoth(),
                messages.getMcCheckNotFoundMessage().replace("{user}", playerName),
                false
            )
            return embed
        }
        
        val uuid = player.uniqueId.toString()
        val linkInfo = linkingService.getLinkInfo(uuid)
        
        embed.addField(messages.getMcCheckMinecraftLabel(), player.name ?: playerName, true)
        embed.addField(messages.getMcCheckUuidLabel(), uuid.substring(0, 8) + "...", true)
        
        if (linkInfo != null) {
            embed.setColor(Color.GREEN)
            embed.addField(messages.getMcCheckStatusLabel(), messages.getMcCheckStatusLinked(), true)
            embed.addField(messages.getMcCheckDiscordLabel(), linkInfo.discordName, true)
            embed.addField(messages.getMcCheckDiscordIdLabel(), linkInfo.discordId, true)
            embed.addField(messages.getMcCheckLinkedAtLabel(), linkInfo.linkedAt.toString(), true)
            
            // Show other accounts
            val allAccounts = linkingService.getLinkedAccounts(linkInfo.discordId)
            if (allAccounts.size > 1) {
                val otherAccounts = allAccounts
                    .filter { it.minecraftUuid != uuid }
                    .joinToString(", ") { it.minecraftName }
                embed.addField(messages.getMcCheckOtherAccountsLabel(), otherAccounts, false)
            }
        } else {
            embed.setColor(Color.RED)
            embed.addField(messages.getMcCheckStatusLabel(), messages.getMcCheckStatusNotLinkedMc(), true)
            
            val pendingCode = linkingService.getPendingCode(uuid)
            if (pendingCode != null) {
                embed.addField(messages.getMcCheckPendingCodeLabel(), pendingCode, true)
            }
        }
        
        return embed
    }
    
    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        if (event.name != "status" || event.focusedOption.name != "user") return
        
        val input = event.focusedOption.value.lowercase()
        val suggestions = mutableListOf<Command.Choice>()
        
        val guild = event.guild
        if (guild == null) {
            event.replyChoices(emptyList()).queue()
            return
        }
        
        // If input is empty or very short, show helpful message
        if (input.isEmpty()) {
            suggestions.add(Command.Choice("💡 พิมพ์ชื่อ Discord หรือ Minecraft...", ""))
            event.replyChoices(suggestions).queue()
            return
        }
        
        // Add Discord members with better formatting
        if (input.length >= 1) {
            val discordMembers = guild.members
                .filter { member ->
                    !member.user.isBot && (
                        member.user.name.lowercase().contains(input) ||
                        member.effectiveName.lowercase().contains(input) ||
                        member.id.contains(input)
                    )
                }
                .sortedBy { member ->
                    // Prioritize exact matches
                    when {
                        member.user.name.lowercase() == input -> 0
                        member.effectiveName.lowercase() == input -> 1
                        member.user.name.lowercase().startsWith(input) -> 2
                        member.effectiveName.lowercase().startsWith(input) -> 3
                        else -> 4
                    }
                }
                .take(15)
                .map { member ->
                    val displayName = if (member.effectiveName != member.user.name) {
                        "👤 ${member.effectiveName} (@${member.user.name})"
                    } else {
                        "👤 @${member.user.name}"
                    }
                    Command.Choice(displayName, member.id)
                }
            suggestions.addAll(discordMembers)
        }
        
        // Add Minecraft players with better formatting
        if (suggestions.size < 25) {
            val onlinePlayers = Bukkit.getOnlinePlayers()
                .filter { it.name.lowercase().contains(input) }
                .sortedBy { player ->
                    // Prioritize exact matches
                    when {
                        player.name.lowercase() == input -> 0
                        player.name.lowercase().startsWith(input) -> 1
                        else -> 2
                    }
                }
                .take(25 - suggestions.size)
                .map { player ->
                    val linkInfo = linkingService.getLinkInfo(player.uniqueId.toString())
                    val status = if (linkInfo != null) "✅" else "❌"
                    Command.Choice("🎮 $status ${player.name}", player.name)
                }
            suggestions.addAll(onlinePlayers)
        }
        
        // If no suggestions found
        if (suggestions.isEmpty()) {
            suggestions.add(Command.Choice("❌ ไม่พบผู้ใช้ที่ตรงกัน", ""))
        }
        
        event.replyChoices(suggestions.take(25)).queue()
    }
}
