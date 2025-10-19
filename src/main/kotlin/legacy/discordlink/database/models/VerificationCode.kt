package legacy.discordlink.database.models

import java.time.Instant

/**
 * Data model representing a temporary verification code
 */
data class VerificationCode(
    val code: String,
    val minecraftUuid: String,
    val minecraftName: String,
    val createdAt: Instant = Instant.now(),
    val expiresAt: Instant
) {
    /**
     * Check if this code has expired
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
    
    /**
     * Check if this code is still valid
     */
    fun isValid(): Boolean = !isExpired()
    
    /**
     * Get remaining time in minutes
     */
    fun getRemainingMinutes(): Long {
        val now = Instant.now()
        if (now.isAfter(expiresAt)) return 0
        return java.time.Duration.between(now, expiresAt).toMinutes()
    }
    
    companion object {
        /**
         * Create a new verification code with expiry time
         */
        fun create(
            code: String,
            minecraftUuid: String,
            minecraftName: String,
            expiryMinutes: Int
        ): VerificationCode {
            val now = Instant.now()
            return VerificationCode(
                code = code,
                minecraftUuid = minecraftUuid,
                minecraftName = minecraftName,
                createdAt = now,
                expiresAt = now.plusSeconds(expiryMinutes * 60L)
            )
        }
    }
}
