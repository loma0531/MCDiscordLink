package legacy.discordlink.services

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

/**
 * Service for verifying Discord user requirements
 */
class VerificationService(
    private val enabled: Boolean,
    private val minAccountAgeDays: Int,
    private val minServerJoinMinutes: Int
) {
    
    /**
     * Check if user meets all verification requirements
     * 
     * @return null if verified, error message if not
     */
    fun checkRequirements(user: User, member: Member?): String? {
        if (!enabled) {
            return null // Verification disabled
        }
        
        // Check account age
        val accountAgeError = checkAccountAge(user)
        if (accountAgeError != null) {
            return accountAgeError
        }
        
        // Check server join time
        if (member != null) {
            val serverJoinError = checkServerJoinTime(member)
            if (serverJoinError != null) {
                return serverJoinError
            }
        }
        
        return null // All checks passed
    }
    
    /**
     * Check if Discord account is old enough
     */
    private fun checkAccountAge(user: User): String? {
        val accountAge = System.currentTimeMillis() - user.timeCreated.toEpochSecond() * 1000
        val accountAgeDays = accountAge / (24 * 60 * 60 * 1000)
        
        return if (accountAgeDays < minAccountAgeDays) {
            "Account must be at least $minAccountAgeDays days old (current: $accountAgeDays days)"
        } else null
    }
    
    /**
     * Check if user has been in server long enough
     */
    private fun checkServerJoinTime(member: Member): String? {
        val joinTime = member.timeJoined.toEpochSecond() * 1000
        val serverTime = System.currentTimeMillis() - joinTime
        val serverMinutes = serverTime / (60 * 1000)
        
        return if (serverMinutes < minServerJoinMinutes) {
            "Must be in server for at least $minServerJoinMinutes minutes (current: $serverMinutes minutes)"
        } else null
    }
    
    /**
     * Get account age in days
     */
    fun getAccountAgeDays(user: User): Long {
        val accountAge = System.currentTimeMillis() - user.timeCreated.toEpochSecond() * 1000
        return accountAge / (24 * 60 * 60 * 1000)
    }
    
    /**
     * Get server join time in minutes
     */
    fun getServerJoinMinutes(member: Member): Long {
        val joinTime = member.timeJoined.toEpochSecond() * 1000
        val serverTime = System.currentTimeMillis() - joinTime
        return serverTime / (60 * 1000)
    }
}
