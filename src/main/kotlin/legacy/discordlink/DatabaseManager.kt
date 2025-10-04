package legacy.discordlink

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.plugin.java.JavaPlugin
import java.sql.Connection
import java.sql.SQLException

class DatabaseManager(private val plugin: JavaPlugin, private val cfg: ConfigManager) {
    private var ds: HikariDataSource? = null
    private var connected = false

    fun initialize(): Boolean {
        val host = cfg.getDatabaseHost()
        val port = cfg.getDatabasePort()
        val db = cfg.getDatabaseName()
        val user = cfg.getDatabaseUsername()
        val pass = cfg.getDatabasePassword()
        val useSSL = cfg.getUseSSL()
        val allowPKR = cfg.getAllowPKR()

        val jdbcUrl = "jdbc:mysql://$host:$port/$db?useSSL=$useSSL&allowPublicKeyRetrieval=$allowPKR&serverTimezone=UTC"

        try {
            val hc = HikariConfig().apply {
                this.jdbcUrl = jdbcUrl
                this.username = user
                this.password = pass
                this.driverClassName = "com.mysql.cj.jdbc.Driver"
                maximumPoolSize = cfg.getMaxPool()
                minimumIdle = cfg.getMinIdle()
                connectionTimeout = cfg.getConnTimeout()
                idleTimeout = cfg.getIdleTimeout()
                maxLifetime = cfg.getMaxLifetime()

                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            }
            
            ds = HikariDataSource(hc)

            // Test connection
            ds!!.connection.use { conn ->
                if (!conn.isValid(5)) throw SQLException("Invalid connection")
            }

            connected = true
            createTables()
            plugin.logger.info("Database connected successfully")
            return true
        } catch (e: Exception) {
            plugin.logger.severe("Database connection failed: ${e.message}")
            connected = false
            ds?.close()
            ds = null
            return false
        }
    }

    private fun createTables() {
        ds!!.connection.use { c ->
            val s = c.createStatement()
            
            // Main links table - allows multiple minecraft accounts per discord
            val linksTable = """
                CREATE TABLE IF NOT EXISTS discord_links (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    minecraft_uuid VARCHAR(36) NOT NULL UNIQUE,
                    minecraft_name VARCHAR(16) NOT NULL,
                    discord_id VARCHAR(20) NOT NULL,
                    discord_name VARCHAR(32) NOT NULL,
                    linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_discord_id (discord_id),
                    INDEX idx_minecraft_uuid (minecraft_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """.trimIndent()
            s.executeUpdate(linksTable)
            
            // Temporary codes table
            val codesTable = """
                CREATE TABLE IF NOT EXISTS temp_codes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(4) NOT NULL UNIQUE,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    minecraft_name VARCHAR(16) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    INDEX idx_code (code),
                    INDEX idx_expires (expires_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """.trimIndent()
            s.executeUpdate(codesTable)
        }
    }

    fun getConnection(): Connection {
        if (ds == null || !connected) throw SQLException("DB not connected")
        return ds!!.connection
    }

    fun isPlayerLinked(uuid: String): Boolean {
        try {
            getConnection().use { c ->
                val ps = c.prepareStatement("SELECT COUNT(*) FROM discord_links WHERE minecraft_uuid = ?")
                ps.setString(1, uuid)
                val rs = ps.executeQuery()
                if (rs.next()) return rs.getInt(1) > 0
            }
        } catch (e: Exception) {
            if (cfg.getDebug()) plugin.logger.warning("isPlayerLinked error: ${e.message}")
        }
        return false
    }

    fun generateAndStoreCode(uuid: String, name: String, expiryMinutes: Int): String {
        try {
            getConnection().use { c ->
                // Clean expired codes first
                val cleanPs = c.prepareStatement("DELETE FROM temp_codes WHERE expires_at < NOW()")
                cleanPs.executeUpdate()
                
                // Generate unique 4-digit code
                var code: String
                var attempts = 0
                do {
                    code = (1000..9999).random().toString()
                    val checkPs = c.prepareStatement("SELECT COUNT(*) FROM temp_codes WHERE code = ?")
                    checkPs.setString(1, code)
                    val rs = checkPs.executeQuery()
                    rs.next()
                    val exists = rs.getInt(1) > 0
                    attempts++
                } while (exists && attempts < 100)
                
                if (attempts >= 100) {
                    plugin.logger.severe("Failed to generate unique code after 100 attempts")
                    return ""
                }
                
                // Store code with expiry
                val insertPs = c.prepareStatement(
                    "INSERT INTO temp_codes (code, minecraft_uuid, minecraft_name, expires_at) VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL ? MINUTE)) ON DUPLICATE KEY UPDATE code = VALUES(code), expires_at = VALUES(expires_at)"
                )
                insertPs.setString(1, code)
                insertPs.setString(2, uuid)
                insertPs.setString(3, name)
                insertPs.setInt(4, expiryMinutes)
                insertPs.executeUpdate()
                
                return code
            }
        } catch (e: Exception) {
            if (cfg.getDebug()) plugin.logger.warning("generateAndStoreCode error: ${e.message}")
            return ""
        }
    }

    fun verifyCodeAndGetPlayer(code: String): Pair<String, String>? {
        try {
            getConnection().use { c ->
                val ps = c.prepareStatement(
                    "SELECT minecraft_uuid, minecraft_name FROM temp_codes WHERE code = ? AND expires_at > NOW()"
                )
                ps.setString(1, code)
                val rs = ps.executeQuery()
                
                if (rs.next()) {
                    val uuid = rs.getString("minecraft_uuid")
                    val name = rs.getString("minecraft_name")
                    
                    // Delete the used code
                    val deletePs = c.prepareStatement("DELETE FROM temp_codes WHERE code = ?")
                    deletePs.setString(1, code)
                    deletePs.executeUpdate()
                    
                    return Pair(uuid, name)
                }
            }
        } catch (e: Exception) {
            if (cfg.getDebug()) plugin.logger.warning("verifyCodeAndGetPlayer error: ${e.message}")
        }
        return null
    }

    fun getAccountCountForDiscord(discordId: String): Int {
        try {
            getConnection().use { c ->
                val ps = c.prepareStatement("SELECT COUNT(*) FROM discord_links WHERE discord_id = ?")
                ps.setString(1, discordId)
                val rs = ps.executeQuery()
                if (rs.next()) return rs.getInt(1)
            }
        } catch (e: Exception) {
            if (cfg.getDebug()) plugin.logger.warning("getAccountCountForDiscord error: ${e.message}")
        }
        return 0
    }

    fun linkAccount(uuid: String, name: String, discordId: String, discordName: String): Boolean {
        try {
            getConnection().use { c ->
                val ps = c.prepareStatement(
                    "INSERT INTO discord_links (minecraft_uuid, minecraft_name, discord_id, discord_name) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id), discord_name = VALUES(discord_name), updated_at = CURRENT_TIMESTAMP"
                )
                ps.setString(1, uuid)
                ps.setString(2, name)
                ps.setString(3, discordId)
                ps.setString(4, discordName)
                ps.executeUpdate()
                return true
            }
        } catch (e: Exception) {
            if (cfg.getDebug()) plugin.logger.warning("linkAccount error: ${e.message}")
            return false
        }
    }

    fun close() {
        try { ds?.close() } catch (e: Exception) { /* Silent close */ }
    }
}
