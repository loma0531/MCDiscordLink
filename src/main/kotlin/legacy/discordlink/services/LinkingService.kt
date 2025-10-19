package legacy.discordlink.services

import legacy.discordlink.database.models.LinkInfo
import legacy.discordlink.database.models.VerificationCode
import legacy.discordlink.database.repositories.CodeRepository
import legacy.discordlink.database.repositories.LinkRepository

/**
 * Service for handling account linking business logic
 * 
 * This service coordinates between repositories and enforces business rules
 */
class LinkingService(
    private val linkRepository: LinkRepository,
    private val codeRepository: CodeRepository,
    private val maxAccountsPerDiscord: Int,
    private val logger: java.util.logging.Logger? = null,
    private val debug: Boolean = false
) {
    
    /**
     * Check if a Minecraft account is linked
     */
    fun isLinked(minecraftUuid: String): Boolean {
        return linkRepository.isLinked(minecraftUuid)
    }
    
    /**
     * Get link information for a Minecraft account
     */
    fun getLinkInfo(minecraftUuid: String): LinkInfo? {
        return linkRepository.findByMinecraftUuid(minecraftUuid)
    }
    
    /**
     * Get all Minecraft accounts linked to a Discord ID
     */
    fun getLinkedAccounts(discordId: String): List<LinkInfo> {
        return linkRepository.findByDiscordId(discordId)
    }
    
    /**
     * Get count of accounts linked to a Discord ID
     */
    fun getAccountCount(discordId: String): Int {
        return linkRepository.countByDiscordId(discordId)
    }
    
    /**
     * Generate a verification code for a player
     * 
     * @return The generated code, or null if failed
     */
    fun generateVerificationCode(
        minecraftUuid: String,
        minecraftName: String,
        expiryMinutes: Int
    ): String? {
        // Clean expired codes first
        codeRepository.cleanExpired()
        
        // Generate unique code
        var attempts = 0
        var code: String
        
        do {
            code = (1000..9999).random().toString()
            attempts++
        } while (codeRepository.exists(code) && attempts < 100)
        
        if (attempts >= 100) {
            return null // Failed to generate unique code
        }
        
        // Create verification code
        val verificationCode = VerificationCode.create(
            code = code,
            minecraftUuid = minecraftUuid,
            minecraftName = minecraftName,
            expiryMinutes = expiryMinutes
        )
        
        return if (codeRepository.create(verificationCode)) code else null
    }
    
    /**
     * Get pending verification code for a player
     */
    fun getPendingCode(minecraftUuid: String): String? {
        return codeRepository.findPendingCode(minecraftUuid)?.code
    }
    
    /**
     * Link a Minecraft account to Discord using verification code
     * 
     * @return Result with LinkInfo if successful, or error message
     */
    fun linkAccount(
        code: String,
        discordId: String,
        discordName: String
    ): Result<LinkInfo> {
        if (debug) {
            logger?.info("=== Link Account Attempt ===")
            logger?.info("Code: $code")
            logger?.info("Discord ID: $discordId")
            logger?.info("Discord Name: $discordName")
        }
        
        // 1. Verify code exists and is valid
        val verificationCode = codeRepository.findValidCode(code)
        if (verificationCode == null) {
            if (debug) {
                logger?.warning("Step 1 FAILED: Code not found or expired")
            }
            return Result.failure(Exception("Invalid or expired code"))
        }
        
        if (debug) {
            logger?.info("Step 1 OK: Code found for ${verificationCode.minecraftName} (${verificationCode.minecraftUuid})")
        }
        
        // 2. Check if already linked
        val isAlreadyLinked = linkRepository.isLinked(verificationCode.minecraftUuid)
        if (isAlreadyLinked) {
            if (debug) {
                logger?.warning("Step 2 FAILED: Minecraft account already linked")
            }
            return Result.failure(Exception("This Minecraft account is already linked"))
        }
        
        if (debug) {
            logger?.info("Step 2 OK: Minecraft account not linked yet")
        }
        
        // 3. Check account limit
        val currentCount = linkRepository.countByDiscordId(discordId)
        if (currentCount >= maxAccountsPerDiscord) {
            if (debug) {
                logger?.warning("Step 3 FAILED: Account limit reached ($currentCount/$maxAccountsPerDiscord)")
            }
            return Result.failure(Exception("Account limit reached: $currentCount/$maxAccountsPerDiscord"))
        }
        
        if (debug) {
            logger?.info("Step 3 OK: Account limit check passed ($currentCount/$maxAccountsPerDiscord)")
        }
        
        // 4. Create link
        val linkInfo = LinkInfo(
            minecraftUuid = verificationCode.minecraftUuid,
            minecraftName = verificationCode.minecraftName,
            discordId = discordId,
            discordName = discordName
        )
        
        if (debug) {
            logger?.info("Step 4: Creating link in database...")
        }
        
        val created = linkRepository.create(linkInfo)
        return if (created) {
            if (debug) {
                logger?.info("Step 4 OK: Link created successfully")
                logger?.info("Step 5: Deleting used code...")
            }
            
            // Delete used code
            val deleted = codeRepository.delete(code)
            if (debug) {
                logger?.info("Step 5 ${if (deleted) "OK" else "WARNING"}: Code deletion ${if (deleted) "successful" else "failed"}")
                logger?.info("=== Link Account SUCCESS ===")
            }
            
            Result.success(linkInfo)
        } else {
            if (debug) {
                logger?.warning("Step 4 FAILED: Failed to create link in database")
                logger?.warning("=== Link Account FAILED ===")
            }
            Result.failure(Exception("Failed to create link in database"))
        }
    }
    
    /**
     * Unlink a Minecraft account from Discord
     * 
     * @return true if successful
     */
    fun unlinkAccount(minecraftUuid: String): Boolean {
        return linkRepository.delete(minecraftUuid)
    }
    
    /**
     * Check if Discord user can link more accounts
     */
    fun canLinkMore(discordId: String): Boolean {
        val currentCount = linkRepository.countByDiscordId(discordId)
        return currentCount < maxAccountsPerDiscord
    }
    
    /**
     * Get remaining slots for a Discord user
     */
    fun getRemainingSlots(discordId: String): Int {
        val currentCount = linkRepository.countByDiscordId(discordId)
        return maxOf(0, maxAccountsPerDiscord - currentCount)
    }
}
