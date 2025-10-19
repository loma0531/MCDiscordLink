package legacy.discordlink.minecraft.listeners

import legacy.discordlink.config.ConfigManager
import legacy.discordlink.services.LinkingService
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

/**
 * Listener for player join events
 * Handles verification code generation and kicking unlinked players
 */
class JoinListener(
    private val plugin: JavaPlugin,
    private val linkingService: LinkingService,
    private val config: ConfigManager
) : Listener {
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // Allow operators to bypass linking if enabled
        if (player.isOp && config.getOpsBypassLinking()) {
            return
        }

        // Use scheduler to prevent conflicts with other plugins
        object : BukkitRunnable() {
            override fun run() {
                handlePlayerJoin(player)
            }
        }.runTaskLater(plugin, config.getKickDelayTicks())
    }
    
    /**
     * Handle player join logic
     */
    private fun handlePlayerJoin(player: Player) {
        // Check if player is still online
        if (!player.isOnline) return
        
        // Check if player is linked
        if (linkingService.isLinked(player.uniqueId.toString())) {
            return // Player is linked, allow join
        }
        
        // Generate verification code
        val code = linkingService.generateVerificationCode(
            minecraftUuid = player.uniqueId.toString(),
            minecraftName = player.name,
            expiryMinutes = config.getCodeExpiryMinutes()
        )
        
        if (code != null) {
            player.kick(buildKickMessage(code))
        } else {
            player.kick(config.parseColoredText(config.getKickError()))
        }
    }
    
    /**
     * Build kick message with verification code
     */
    private fun buildKickMessage(code: String): Component {
        val msgBuilder = Component.text()
        
        // Title
        msgBuilder.append(config.parseColoredText(config.getKickTitle()))
        
        if (config.getKickAddEmptyLines()) {
            msgBuilder.append(Component.newline()).append(Component.newline())
        }
        
        // Code display
        msgBuilder.append(config.parseColoredText(config.getKickCodePrefix()))
            .append(config.parseColoredText(config.getKickCodeColor() + code))
        
        if (config.getKickAddEmptyLines()) {
            msgBuilder.append(Component.newline()).append(Component.newline())
        }
        
        // Steps title
        msgBuilder.append(config.parseColoredText(config.getKickStepsTitle()))
            .append(Component.newline())
        
        // Add configurable steps
        val steps = config.getKickSteps()
        for (step in steps) {
            val processedStep = step.replace("{code}", code)
            msgBuilder.append(config.parseColoredText(processedStep))
                .append(Component.newline())
        }
        
        if (config.getKickAddEmptyLines()) {
            msgBuilder.append(Component.newline())
        }
        
        // Expiry warning
        val expiryText = config.getKickExpiry()
            .replace("{minutes}", config.getCodeExpiryMinutes().toString())
        msgBuilder.append(config.parseColoredText(expiryText))
        
        return msgBuilder.build()
    }
}
