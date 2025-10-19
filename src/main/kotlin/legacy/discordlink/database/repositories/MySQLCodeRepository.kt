package legacy.discordlink.database.repositories

import com.zaxxer.hikari.HikariDataSource
import legacy.discordlink.database.models.VerificationCode
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * MySQL implementation of CodeRepository
 * 
 * Handles all database operations for verification codes
 */
class MySQLCodeRepository(
    private val dataSource: HikariDataSource,
    private val logger: java.util.logging.Logger,
    private val debug: Boolean = false
) : CodeRepository {
    
    override fun create(code: VerificationCode): Boolean {
        if (debug) {
            logger.info("Creating code: ${code.code} for ${code.minecraftName} (${code.minecraftUuid}), expires at ${code.expiresAt}")
        }
        
        val result = executeUpdate(
            sql = """
                INSERT INTO temp_codes (code, minecraft_uuid, minecraft_name, created_at, expires_at) 
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                    minecraft_uuid = VALUES(minecraft_uuid),
                    minecraft_name = VALUES(minecraft_name),
                    expires_at = VALUES(expires_at)
            """.trimIndent(),
            params = listOf(
                code.code,
                code.minecraftUuid,
                code.minecraftName,
                Timestamp.from(code.createdAt),
                Timestamp.from(code.expiresAt)
            )
        ) > 0
        
        if (debug) {
            logger.info("Code creation result: $result")
        }
        
        return result
    }
    
    override fun findValidCode(code: String): VerificationCode? {
        if (debug) {
            logger.info("Searching for code: $code")
        }
        
        val result = executeQuery(
            sql = """
                SELECT code, minecraft_uuid, minecraft_name, created_at, expires_at 
                FROM temp_codes 
                WHERE code = ? AND expires_at > NOW()
            """.trimIndent(),
            params = listOf(code)
        ) { rs ->
            if (rs.next()) {
                val verificationCode = mapToVerificationCode(rs)
                if (debug) {
                    logger.info("Found valid code: ${verificationCode.code} for ${verificationCode.minecraftName}, expires at ${verificationCode.expiresAt}")
                }
                verificationCode
            } else {
                if (debug) {
                    logger.warning("Code not found or expired: $code")
                    // Debug: Check if code exists at all
                    executeQuery(
                        sql = "SELECT code, expires_at, NOW() as now FROM temp_codes WHERE code = ?",
                        params = listOf(code)
                    ) { rs2 ->
                        if (rs2.next()) {
                            logger.warning("Code exists but expired. Code: ${rs2.getString("code")}, Expires: ${rs2.getTimestamp("expires_at")}, Now: ${rs2.getTimestamp("now")}")
                        } else {
                            logger.warning("Code does not exist in database")
                        }
                    }
                }
                null
            }
        }
        
        return result
    }
    
    override fun findPendingCode(minecraftUuid: String): VerificationCode? {
        return executeQuery(
            sql = """
                SELECT code, minecraft_uuid, minecraft_name, created_at, expires_at 
                FROM temp_codes 
                WHERE minecraft_uuid = ? AND expires_at > NOW()
                ORDER BY created_at DESC
                LIMIT 1
            """.trimIndent(),
            params = listOf(minecraftUuid)
        ) { rs ->
            if (rs.next()) mapToVerificationCode(rs) else null
        }
    }
    
    override fun delete(code: String): Boolean {
        return executeUpdate(
            sql = "DELETE FROM temp_codes WHERE code = ?",
            params = listOf(code)
        ) > 0
    }
    
    override fun deleteByMinecraftUuid(minecraftUuid: String): Int {
        return executeUpdate(
            sql = "DELETE FROM temp_codes WHERE minecraft_uuid = ?",
            params = listOf(minecraftUuid)
        )
    }
    
    override fun cleanExpired(): Int {
        return executeUpdate(
            sql = "DELETE FROM temp_codes WHERE expires_at < NOW()",
            params = emptyList()
        )
    }
    
    override fun exists(code: String): Boolean {
        return executeQuery(
            sql = "SELECT COUNT(*) FROM temp_codes WHERE code = ?",
            params = listOf(code)
        ) { rs ->
            rs.next() && rs.getInt(1) > 0
        } ?: false
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Map ResultSet to VerificationCode object
     */
    private fun mapToVerificationCode(rs: ResultSet): VerificationCode {
        return VerificationCode(
            code = rs.getString("code"),
            minecraftUuid = rs.getString("minecraft_uuid"),
            minecraftName = rs.getString("minecraft_name"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant()
        )
    }
    
    /**
     * Execute a SELECT query with error handling
     */
    private fun <T> executeQuery(
        sql: String,
        params: List<Any>,
        mapper: (ResultSet) -> T
    ): T? {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.executeQuery().use { rs ->
                        mapper(rs)
                    }
                }
            }
        } catch (e: Exception) {
            if (debug) {
                logger.warning("Query error in CodeRepository: ${e.message}")
                e.printStackTrace()
            }
            null
        }
    }
    
    /**
     * Execute an INSERT/UPDATE/DELETE query with error handling
     */
    private fun executeUpdate(sql: String, params: List<Any>): Int {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement(sql).use { stmt ->
                    params.forEachIndexed { index, param ->
                        stmt.setObject(index + 1, param)
                    }
                    stmt.executeUpdate()
                }
            }
        } catch (e: Exception) {
            if (debug) {
                logger.warning("Update error in CodeRepository: ${e.message}")
                e.printStackTrace()
            }
            0
        }
    }
}
