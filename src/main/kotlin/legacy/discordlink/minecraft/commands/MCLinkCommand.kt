package legacy.discordlink.minecraft.commands

import legacy.discordlink.config.MessageManager
import legacy.discordlink.core.PluginCore
import legacy.discordlink.services.LinkingService
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Command handler for /mclink
 * 
 * Provides admin commands for managing the plugin and player commands for unlinking
 */
class MCLinkCommand(
    private val plugin: JavaPlugin,
    private val core: PluginCore,
    private val linkingService: LinkingService,
    private val messages: MessageManager
) : CommandExecutor, TabCompleter {
    
    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        // Check if it's unlink command (players can use this)
        if (args.isNotEmpty() && args[0].lowercase() == "unlink") {
            return handleUnlink(sender)
        }
        
        // Check permission for admin commands
        if (!sender.hasPermission("mcdiscordlink.admin") && !sender.isOp) {
            sender.sendMessage(messages.getCmdNoPermission())
            sender.sendMessage(messages.getCmdRequiredPermission())
            return true
        }
        
        // Show usage if no args
        if (args.isEmpty()) {
            showUsage(sender)
            return true
        }
        
        // Handle subcommands
        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "check" -> handleCheck(sender, args)
            "unlink" -> handleUnlink(sender)
            else -> {
                sender.sendMessage(messages.getCmdUnknownCommand().replace("{command}", args[0]))
                sender.sendMessage(messages.getCmdUsageMain())
            }
        }
        
        return true
    }
    
    /**
     * Show command usage
     */
    private fun showUsage(sender: CommandSender) {
        sender.sendMessage(messages.getCmdUsageMain())
        sender.sendMessage(messages.getCmdAvailableCommands())
        sender.sendMessage(messages.getCmdReload())
        sender.sendMessage(messages.getCmdCheck())
        sender.sendMessage(messages.getCmdUnlink())
    }
    
    /**
     * Handle reload subcommand
     */
    private fun handleReload(sender: CommandSender) {
        try {
            core.reload()
            sender.sendMessage(messages.getCmdReloadSuccess())
        } catch (e: Exception) {
            sender.sendMessage(messages.getCmdReloadFailed().replace("{error}", e.message ?: "Unknown"))
            plugin.logger.warning("Reload failed: ${e.message}")
        }
    }
    
    /**
     * Handle check subcommand
     */
    private fun handleCheck(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(messages.getCmdCheckUsage())
            return
        }
        
        val playerName = args[1]
        val player = Bukkit.getPlayerExact(playerName) ?: Bukkit.getOfflinePlayer(playerName)
        
        if (!player.hasPlayedBefore() && !player.isOnline) {
            sender.sendMessage(messages.getCmdCheckPlayerNotFound().replace("{player}", playerName))
            return
        }
        
        val uuid = player.uniqueId.toString()
        val linkInfo = linkingService.getLinkInfo(uuid)
        
        if (linkInfo != null) {
            sender.sendMessage(messages.getCmdCheckLinked().replace("{player}", playerName))
            sender.sendMessage(messages.getCmdCheckDiscordId().replace("{discord-id}", linkInfo.discordId))
            sender.sendMessage(messages.getCmdCheckDiscordName().replace("{discord-name}", linkInfo.discordName))
            sender.sendMessage(messages.getCmdCheckLinkedAt().replace("{date}", linkInfo.linkedAt.toString()))
        } else {
            sender.sendMessage(messages.getCmdCheckNotLinked().replace("{player}", playerName))
            
            // Check if they have a pending code
            val pendingCode = linkingService.getPendingCode(uuid)
            if (pendingCode != null) {
                sender.sendMessage(messages.getCmdCheckPendingCode().replace("{code}", pendingCode))
            }
        }
    }
    
    /**
     * Handle unlink subcommand
     */
    private fun handleUnlink(sender: CommandSender): Boolean {
        if (sender !is Player) {
            sender.sendMessage(messages.getCmdUnlinkPlayersOnly())
            return true
        }
        
        val uuid = sender.uniqueId.toString()
        
        if (!linkingService.isLinked(uuid)) {
            sender.sendMessage(messages.getCmdUnlinkNotLinked())
            return true
        }
        
        // Unlink the account
        val success = linkingService.unlinkAccount(uuid)
        if (success) {
            sender.sendMessage(messages.getCmdUnlinkSuccess())
            sender.sendMessage(messages.getCmdUnlinkWarning())
            
            // Countdown messages
            for (i in 5 downTo 1) {
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    if (sender.isOnline) {
                        sender.sendMessage(messages.getCmdUnlinkCountdown().replace("{seconds}", i.toString()))
                    }
                }, (100L - (i * 20L)))
            }
            
            // Schedule kick after 5 seconds
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                if (sender.isOnline) {
                    sender.kickPlayer(messages.getCmdUnlinkKickMessage())
                }
            }, 100L)
        } else {
            sender.sendMessage(messages.getCmdUnlinkFailed())
        }
        
        return true
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> {
                val commands = mutableListOf<String>()
                if (sender.hasPermission("mcdiscordlink.admin") || sender.isOp) {
                    commands.addAll(listOf("reload", "check"))
                }
                commands.add("unlink")
                commands.filter { it.startsWith(args[0].lowercase()) }
            }
            2 -> {
                if (args[0].lowercase() == "check" && (sender.hasPermission("mcdiscordlink.admin") || sender.isOp)) {
                    Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.lowercase().startsWith(args[1].lowercase()) }
                } else emptyList()
            }
            else -> emptyList()
        }
    }
}
