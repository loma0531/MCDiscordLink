package legacy.discordlink.database.models

import java.time.Instant

/**
 * Data model representing a link between Minecraft and Discord accounts
 */
data class LinkInfo(
    val minecraftUuid: String,
    val minecraftName: String,
    val discordId: String,
    val discordName: String,
    val linkedAt: Instant = Instant.now()
) {
    /**
     * Check if this link is for the given Minecraft UUID
     */
    fun isForMinecraft(uuid: String): Boolean = minecraftUuid == uuid
    
    /**
     * Check if this link is for the given Discord ID
     */
    fun isForDiscord(id: String): Boolean = discordId == id
    
    /**
     * Get a formatted string representation
     */
    fun toDisplayString(): String {
        return "Minecraft: $minecraftName ($minecraftUuid) <-> Discord: $discordName ($discordId)"
    }
}
