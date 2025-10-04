package legacy.discordlink

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scheduler.BukkitRunnable

class JoinListener(private val plugin: Main, private val db: DatabaseManager) : Listener {
    
    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(e: PlayerJoinEvent) {
        val p = e.player
        
        // Allow operators to bypass linking if enabled
        if (p.isOp && plugin.cfg.getOpsBypassLinking()) return

        // Use scheduler to prevent conflicts with other plugins
        object : BukkitRunnable() {
            override fun run() {
                // Check if player is still online
                if (!p.isOnline) return
                
                val linked = db.isPlayerLinked(p.uniqueId.toString())
                if (!linked) {
                    // Generate 4-digit code and store in database
                    val code = db.generateAndStoreCode(
                        p.uniqueId.toString(), 
                        p.name, 
                        plugin.cfg.getCodeExpiryMinutes()
                    )
                    
                    if (code.isNotEmpty()) {
                        val msgBuilder = Component.text()
                        
                        // Title
                        msgBuilder.append(plugin.cfg.parseColoredText(plugin.cfg.getKickTitle()))
                        
                        if (plugin.cfg.getKickAddEmptyLines()) {
                            msgBuilder.append(Component.newline()).append(Component.newline())
                        }
                        
                        // Code display
                        msgBuilder.append(plugin.cfg.parseColoredText(plugin.cfg.getKickCodePrefix()))
                            .append(plugin.cfg.parseColoredText(plugin.cfg.getKickCodeColor() + code))
                        
                        if (plugin.cfg.getKickAddEmptyLines()) {
                            msgBuilder.append(Component.newline()).append(Component.newline())
                        }
                        
                        // Steps title
                        msgBuilder.append(plugin.cfg.parseColoredText(plugin.cfg.getKickStepsTitle()))
                            .append(Component.newline())
                        
                        // Add configurable steps
                        val steps = plugin.cfg.getKickSteps()
                        if (steps.isNotEmpty()) {
                            for (step in steps) {
                                val processedStep = step.replace("{code}", code)
                                msgBuilder.append(plugin.cfg.parseColoredText(processedStep))
                                    .append(Component.newline())
                            }
                        }
                        
                        if (plugin.cfg.getKickAddEmptyLines()) {
                            msgBuilder.append(Component.newline())
                        }
                        
                        // Expiry warning
                        val expiryText = plugin.cfg.getKickExpiry().replace("{minutes}", plugin.cfg.getCodeExpiryMinutes().toString())
                        msgBuilder.append(plugin.cfg.parseColoredText(expiryText))
                        
                        p.kick(msgBuilder.build())
                    } else {
                        p.kick(plugin.cfg.parseColoredText(plugin.cfg.getKickError()))
                    }
                }
            }
        }.runTaskLater(plugin, plugin.cfg.getKickDelayTicks())
    }
}
