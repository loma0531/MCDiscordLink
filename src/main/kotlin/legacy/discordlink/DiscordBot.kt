package legacy.discordlink

import dev.kord.core.Kord
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ModalSubmitInteraction
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.rest.builder.component.option

class DiscordBot(
    private val logger: java.util.logging.Logger, 
    private val db: DatabaseManager,
    private val cfg: ConfigManager,
    private val token: String
) {
    private var kord: Kord? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        scope.launch {
            try {
                kord = Kord(token)

                // Register slash command
                kord?.createGlobalChatInputCommand(
                    "setup-link-discord",
                    "Setup Discord-Minecraft linking system (Admin only)"
                )

                // Handle slash command
                kord?.on<ChatInputCommandInteractionCreateEvent> {
                    if (interaction.invokedCommandName == "setup-link-discord") {
                        // Check if command is used in a guild
                        val guildId = interaction.data.guildId.value
                        if (guildId == null) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordGuildOnly()
                            }
                            return@on
                        }
                        
                        // Check if user has admin permissions
                        val member = interaction.user.asMemberOrNull(guildId)
                        val hasPermission = member?.getPermissions()?.contains(Permission.Administrator) == true
                        
                        if (!hasPermission) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordNoPermission()
                            }
                            return@on
                        }

                        // Create embed with button
                        interaction.deferPublicResponse().respond {
                            embed {
                                title = cfg.getDiscordEmbedTitle()
                                description = cfg.getDiscordEmbedDescription()
                                    .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                                    .replace("{expiry-minutes}", cfg.getCodeExpiryMinutes().toString())
                                
                                // Parse color from hex string
                                val colorHex = cfg.getDiscordEmbedColor().removePrefix("#")
                                color = dev.kord.common.Color(colorHex.toInt(16))
                                
                                // Add detailed steps field if enabled
                                if (cfg.getDiscordShowDetailedSteps()) {
                                    field {
                                        name = cfg.getDiscordEmbedFieldTitle()
                                        value = cfg.getDiscordEmbedFieldValue()
                                        inline = false
                                    }
                                }
                            }
                            
                            actionRow {
                                // Parse button style
                                val buttonStyle = when (cfg.getDiscordButtonColor().uppercase()) {
                                    "PRIMARY" -> ButtonStyle.Primary
                                    "SECONDARY" -> ButtonStyle.Secondary
                                    "SUCCESS" -> ButtonStyle.Success
                                    "DANGER" -> ButtonStyle.Danger
                                    else -> ButtonStyle.Primary
                                }
                                
                                interactionButton(buttonStyle, "link_minecraft") {
                                    label = cfg.getDiscordButtonLabel()
                                    emoji = dev.kord.common.entity.DiscordPartialEmoji(name = cfg.getDiscordButtonEmoji())
                                }
                            }
                        }
                    }
                }

                // Handle button click
                kord?.on<ButtonInteractionCreateEvent> {
                    if (interaction.componentId == "link_minecraft") {
                        // Show modal for code input
                        interaction.modal(cfg.getDiscordModalTitle(), "code_input_modal") {
                            actionRow {
                                textInput(TextInputStyle.Short, "minecraft_code", cfg.getDiscordModalInputLabel()) {
                                    placeholder = cfg.getDiscordModalInputPlaceholder()
                                    required = true
                                    allowedLength = 4..4
                                }
                            }
                        }
                    }
                }

                // Handle modal submission
                kord?.on<ModalSubmitInteractionCreateEvent> {
                    if (interaction.modalId == "code_input_modal") {
                        val code = interaction.textInputs["minecraft_code"]?.value?.trim()
                        
                        if (code.isNullOrEmpty() || code.length != 4) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordInvalidCode()
                            }
                            return@on
                        }

                        // Verify code and get player info
                        val playerInfo = db.verifyCodeAndGetPlayer(code)
                        if (playerInfo == null) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordCodeExpired()
                            }
                            return@on
                        }

                        val (uuid, minecraftName) = playerInfo
                        val discordId = interaction.user.id.toString()
                        val discordName = interaction.user.username

                        // Check account limit
                        val currentCount = db.getAccountCountForDiscord(discordId)
                        if (currentCount >= cfg.getMaxAccountsPerDiscord()) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordAccountLimit()
                                    .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                            }
                            return@on
                        }

                        // Link the account
                        val success = db.linkAccount(uuid, minecraftName, discordId, discordName)
                        if (success) {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordSuccess()
                                    .replace("{minecraft-name}", minecraftName)
                                    .replace("{discord-name}", discordName)
                                    .replace("{current-count}", (currentCount + 1).toString())
                                    .replace("{max-accounts}", cfg.getMaxAccountsPerDiscord().toString())
                            }
                            if (cfg.getLogSuccessfulLinks()) {
                                logger.info("Successfully linked $minecraftName ($uuid) with Discord $discordName ($discordId)")
                            }
                        } else {
                            interaction.deferEphemeralResponse().respond {
                                content = cfg.getDiscordError()
                            }
                        }
                    }
                }

                logger.info("Discord bot started successfully")
                kord?.login()
            } catch (e: Exception) {
                logger.severe("Failed to start Discord bot: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        scope.launch {
            try {
                kord?.logout()
            } catch (e: Exception) {
                logger.warning("Error stopping Kord: ${e.message}")
            }
        }
    }
}
