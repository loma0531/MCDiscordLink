package legacy.discordlink.database.repositories

import com.zaxxer.hikari.HikariDataSource
import legacy.discordlink.database.models.LinkInfo
import java.sql.ResultSet
import java.sql.Timestamp

/**
 * MySQL implementation of LinkRepository
 * 
 * Handles all database operations for Discord-Minecraft account links
 * using MySQL/MariaDB database
 */
class MySQLLinkRepository(
    private val dataSource: HikariDataSource,
    private val logger: java.util.logging.Logger,
    private val debug: Boolean = false
) : LinkRepository {
    
    override fun isLinked(minecraftUuid: String): Boolean {
        return executeQuery(
            sql = "SELECT COUNT(*) FROM discord_links WHERE minecraft_uuid = ?",
            params = listOf(minecraftUuid)
        ) { rs ->
            rs.next() && rs.getInt(1) > 0
        } ?: false
    }
    
    override fun findByMinecraftUuid(minecraftUuid: String): LinkInfo? {
        return executeQuery(
            sql = """
                SELECT minecraft_uuid, minecraft_name, discord_id, discord_name, linked_at 
                FROM discord_links 
                WHERE minecraft_uuid = ?
            """.trimIndent(),
            params = listOf(minecraftUuid)
        ) { rs ->
            if (rs.next()) mapToLinkInfo(rs) else null
        }
    }
    
    override fun findByDiscordId(discordId: String): List<LinkInfo> {
        return executeQuery(
            sql = """
                SELECT minecraft_uuid, minecraft_name, discord_id, discord_name, linked_at 
                FROM discord_links 
                WHERE discord_id = ?
                ORDER BY linked_at DESC
            """.trimIndent(),
            params = listOf(discordId)
        ) { rs ->
            buildList {
                while (rs.next()) {
                    add(mapToLinkInfo(rs))
                }
            }
        } ?: emptyList()
    }
    
    override fun countByDiscordId(discordId: String): Int {
        return executeQuery(
            sql = "SELECT COUNT(*) FROM discord_links WHERE discord_id = ?",
            params = listOf(discordId)
        ) { rs ->
            if (rs.next()) rs.getInt(1) else 0
        } ?: 0
    }
    
    override fun create(linkInfo: LinkInfo): Boolean {
        return executeUpdate(
            sql = """
                INSERT INTO discord_links 
                (minecraft_uuid, minecraft_name, discord_id, discord_name, linked_at) 
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE 
                    discord_id = VALUES(discord_id), 
                    discord_name = VALUES(discord_name),
                    linked_at = VALUES(linked_at)
            """.trimIndent(),
            params = listOf(
                linkInfo.minecraftUuid,
                linkInfo.minecraftName,
                linkInfo.discordId,
                linkInfo.discordName,
                Timestamp.from(linkInfo.linkedAt)
            )
        ) > 0
    }
    
    override fun update(linkInfo: LinkInfo): Boolean {
        return executeUpdate(
            sql = """
                UPDATE discord_links 
                SET minecraft_name = ?, discord_id = ?, discord_name = ?, linked_at = ?
                WHERE minecraft_uuid = ?
            """.trimIndent(),
            params = listOf(
                linkInfo.minecraftName,
                linkInfo.discordId,
                linkInfo.discordName,
                Timestamp.from(linkInfo.linkedAt),
                linkInfo.minecraftUuid
            )
        ) > 0
    }
    
    override fun delete(minecraftUuid: String): Boolean {
        return executeUpdate(
            sql = "DELETE FROM discord_links WHERE minecraft_uuid = ?",
            params = listOf(minecraftUuid)
        ) > 0
    }
    
    override fun deleteByDiscordId(discordId: String): Int {
        return executeUpdate(
            sql = "DELETE FROM discord_links WHERE discord_id = ?",
            params = listOf(discordId)
        )
    }
    
    override fun findAll(): List<LinkInfo> {
        return executeQuery(
            sql = """
                SELECT minecraft_uuid, minecraft_name, discord_id, discord_name, linked_at 
                FROM discord_links 
                ORDER BY linked_at DESC
            """.trimIndent(),
            params = emptyList()
        ) { rs ->
            buildList {
                while (rs.next()) {
                    add(mapToLinkInfo(rs))
                }
            }
        } ?: emptyList()
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Map ResultSet to LinkInfo object
     */
    private fun mapToLinkInfo(rs: ResultSet): LinkInfo {
        return LinkInfo(
            minecraftUuid = rs.getString("minecraft_uuid"),
            minecraftName = rs.getString("minecraft_name"),
            discordId = rs.getString("discord_id"),
            discordName = rs.getString("discord_name"),
            linkedAt = rs.getTimestamp("linked_at").toInstant()
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
                logger.warning("Query error in LinkRepository: ${e.message}")
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
                logger.warning("Update error in LinkRepository: ${e.message}")
                e.printStackTrace()
            }
            0
        }
    }
}
