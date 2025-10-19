package legacy.discordlink.database.repositories

import legacy.discordlink.database.models.LinkInfo

/**
 * Repository interface for managing Discord-Minecraft account links
 * 
 * This interface defines all operations related to account linking,
 * allowing for different implementations (MySQL, MongoDB, In-Memory, etc.)
 */
interface LinkRepository {
    
    /**
     * Check if a Minecraft account is linked to Discord
     * 
     * @param minecraftUuid The Minecraft player UUID
     * @return true if linked, false otherwise
     */
    fun isLinked(minecraftUuid: String): Boolean
    
    /**
     * Find link information by Minecraft UUID
     * 
     * @param minecraftUuid The Minecraft player UUID
     * @return LinkInfo if found, null otherwise
     */
    fun findByMinecraftUuid(minecraftUuid: String): LinkInfo?
    
    /**
     * Find all Minecraft accounts linked to a Discord ID
     * 
     * @param discordId The Discord user ID
     * @return List of LinkInfo, empty if none found
     */
    fun findByDiscordId(discordId: String): List<LinkInfo>
    
    /**
     * Count how many Minecraft accounts are linked to a Discord ID
     * 
     * @param discordId The Discord user ID
     * @return Number of linked accounts
     */
    fun countByDiscordId(discordId: String): Int
    
    /**
     * Create a new link between Minecraft and Discord accounts
     * 
     * @param linkInfo The link information to create
     * @return true if successful, false otherwise
     */
    fun create(linkInfo: LinkInfo): Boolean
    
    /**
     * Update an existing link
     * 
     * @param linkInfo The updated link information
     * @return true if successful, false otherwise
     */
    fun update(linkInfo: LinkInfo): Boolean
    
    /**
     * Delete a link by Minecraft UUID
     * 
     * @param minecraftUuid The Minecraft player UUID
     * @return true if deleted, false if not found
     */
    fun delete(minecraftUuid: String): Boolean
    
    /**
     * Delete all links for a Discord ID
     * 
     * @param discordId The Discord user ID
     * @return Number of links deleted
     */
    fun deleteByDiscordId(discordId: String): Int
    
    /**
     * Get all links in the database
     * 
     * @return List of all LinkInfo
     */
    fun findAll(): List<LinkInfo>
}
