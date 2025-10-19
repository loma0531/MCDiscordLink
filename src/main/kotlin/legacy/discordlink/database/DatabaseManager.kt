package legacy.discordlink.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import legacy.discordlink.config.ConfigManager
import legacy.discordlink.database.repositories.*
import org.bukkit.plugin.java.JavaPlugin
import java.sql.SQLException

/**
 * Database manager using Repository Pattern
 * 
 * Manages database connection and provides access to repositories
 */
class DatabaseManager(
    private val plugin: JavaPlugin,
    private val config: ConfigManager
) {
    private var dataSource: HikariDataSource? = null
    private var connected = false
    
    // Repositories
    lateinit var linkRepository: LinkRepository
        private set
    lateinit var codeRepository: CodeRepository
        private set
    
    /**
     * Initialize database connection and repositories
     */
    fun initialize(): Boolean {
        try {
            // Create connection pool
            dataSource = createDataSource()
            
            // Test connection
            dataSource!!.connection.use { conn ->
                if (!conn.isValid(5)) {
                    throw SQLException("Invalid connection")
                }
            }
            
            connected = true
            
            // Create tables
            createTables()
            
            // Initialize repositories
            initializeRepositories()
            
            plugin.logger.info("Database connected successfully")
            return true
            
        } catch (e: Exception) {
            plugin.logger.severe("Database connection failed: ${e.message}")
            connected = false
            dataSource?.close()
            dataSource = null
            return false
        }
    }
    
    /**
     * Create HikariCP data source
     */
    private fun createDataSource(): HikariDataSource {
        val useTimezone = config.getDatabaseUseTimezone()
        val timezone = if (useTimezone) config.getDatabaseTimezone() else "SERVER"
        
        val jdbcUrl = if (useTimezone) {
            "jdbc:mysql://${config.getDatabaseHost()}:${config.getDatabasePort()}/${config.getDatabaseName()}" +
                    "?useSSL=${config.getUseSSL()}" +
                    "&allowPublicKeyRetrieval=${config.getAllowPKR()}" +
                    "&serverTimezone=${config.getDatabaseTimezone()}"
        } else {
            "jdbc:mysql://${config.getDatabaseHost()}:${config.getDatabasePort()}/${config.getDatabaseName()}" +
                    "?useSSL=${config.getUseSSL()}" +
                    "&allowPublicKeyRetrieval=${config.getAllowPKR()}"
        }
        
        plugin.logger.info("Database timezone: $timezone ${if (useTimezone) "(custom)" else "(server default)"}")
        
        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = config.getDatabaseUsername()
            this.password = config.getDatabasePassword()
            this.driverClassName = "com.mysql.cj.jdbc.Driver"
            maximumPoolSize = config.getMaxPool()
            minimumIdle = config.getMinIdle()
            connectionTimeout = config.getConnTimeout()
            idleTimeout = config.getIdleTimeout()
            maxLifetime = config.getMaxLifetime()
            
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        }
        
        return HikariDataSource(hikariConfig)
    }
    
    /**
     * Create database tables
     */
    private fun createTables() {
        dataSource!!.connection.use { conn ->
            conn.createStatement().use { stmt ->
                // Links table
                stmt.executeUpdate("""
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
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.trimIndent())
                
                // Codes table
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS temp_codes (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(4) NOT NULL UNIQUE,
                        minecraft_uuid VARCHAR(36) NOT NULL,
                        minecraft_name VARCHAR(16) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        expires_at TIMESTAMP NOT NULL,
                        INDEX idx_code (code),
                        INDEX idx_expires (expires_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """.trimIndent())
            }
        }
    }
    
    /**
     * Initialize repository instances
     */
    private fun initializeRepositories() {
        val ds = dataSource ?: throw IllegalStateException("DataSource not initialized")
        
        linkRepository = MySQLLinkRepository(
            dataSource = ds,
            logger = plugin.logger,
            debug = config.getDebug()
        )
        
        codeRepository = MySQLCodeRepository(
            dataSource = ds,
            logger = plugin.logger,
            debug = config.getDebug()
        )
    }
    
    /**
     * Check if database is connected
     */
    fun isConnected(): Boolean = connected
    
    /**
     * Close database connection
     */
    fun close() {
        try {
            dataSource?.close()
            connected = false
        } catch (e: Exception) {
            // Silent close
        }
    }
}
