package legacy.discordlink.database.repositories

import legacy.discordlink.database.models.VerificationCode

/**
 * Repository interface for managing verification codes
 * 
 * This interface defines all operations related to temporary verification codes
 * used for linking Minecraft and Discord accounts
 */
interface CodeRepository {
    
    /**
     * Create a new verification code
     * 
     * @param code The verification code to create
     * @return true if successful, false otherwise
     */
    fun create(code: VerificationCode): Boolean
    
    /**
     * Find a valid (non-expired) verification code
     * 
     * @param code The code string to search for
     * @return VerificationCode if found and valid, null otherwise
     */
    fun findValidCode(code: String): VerificationCode?
    
    /**
     * Find any pending verification code for a Minecraft player
     * 
     * @param minecraftUuid The Minecraft player UUID
     * @return VerificationCode if found, null otherwise
     */
    fun findPendingCode(minecraftUuid: String): VerificationCode?
    
    /**
     * Delete a verification code
     * 
     * @param code The code string to delete
     * @return true if deleted, false if not found
     */
    fun delete(code: String): Boolean
    
    /**
     * Delete all codes for a specific Minecraft player
     * 
     * @param minecraftUuid The Minecraft player UUID
     * @return Number of codes deleted
     */
    fun deleteByMinecraftUuid(minecraftUuid: String): Int
    
    /**
     * Clean up all expired verification codes
     * 
     * @return Number of codes deleted
     */
    fun cleanExpired(): Int
    
    /**
     * Check if a code exists (regardless of expiry)
     * 
     * @param code The code string to check
     * @return true if exists, false otherwise
     */
    fun exists(code: String): Boolean
}
