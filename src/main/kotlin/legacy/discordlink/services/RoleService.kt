package legacy.discordlink.services

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role

/**
 * Service for managing Discord roles
 */
class RoleService(
    private val enabled: Boolean,
    private val roleId: String,
    private val logger: java.util.logging.Logger,
    private val debug: Boolean = false
) {
    
    /**
     * Give role to a member after successful linking
     * 
     * @return true if successful or disabled, false if failed
     */
    fun giveLinkedRole(guild: Guild, member: Member): Boolean {
        if (!enabled || roleId.isEmpty()) {
            return true // Feature disabled
        }
        
        return try {
            val role = guild.getRoleById(roleId)
            if (role == null) {
                if (debug) {
                    logger.warning("Role with ID $roleId not found")
                }
                return false
            }
            
            // Check if member already has the role
            if (member.roles.contains(role)) {
                if (debug) {
                    logger.info("Member ${member.effectiveName} already has role ${role.name}")
                }
                return true
            }
            
            // Add role
            guild.addRoleToMember(member, role).queue(
                {
                    if (debug) {
                        logger.info("Added role ${role.name} to ${member.effectiveName}")
                    }
                },
                { error ->
                    if (debug) {
                        logger.warning("Failed to add role to ${member.effectiveName}: ${error.message}")
                    }
                }
            )
            
            true
        } catch (e: Exception) {
            if (debug) {
                logger.warning("Error adding role: ${e.message}")
            }
            false
        }
    }
    
    /**
     * Remove role from a member after unlinking
     * 
     * @return true if successful or disabled, false if failed
     */
    fun removeLinkedRole(guild: Guild, member: Member): Boolean {
        if (!enabled || roleId.isEmpty()) {
            return true // Feature disabled
        }
        
        return try {
            val role = guild.getRoleById(roleId)
            if (role == null) {
                if (debug) {
                    logger.warning("Role with ID $roleId not found")
                }
                return false
            }
            
            // Check if member has the role
            if (!member.roles.contains(role)) {
                if (debug) {
                    logger.info("Member ${member.effectiveName} doesn't have role ${role.name}")
                }
                return true
            }
            
            // Remove role
            guild.removeRoleFromMember(member, role).queue(
                {
                    if (debug) {
                        logger.info("Removed role ${role.name} from ${member.effectiveName}")
                    }
                },
                { error ->
                    if (debug) {
                        logger.warning("Failed to remove role from ${member.effectiveName}: ${error.message}")
                    }
                }
            )
            
            true
        } catch (e: Exception) {
            if (debug) {
                logger.warning("Error removing role: ${e.message}")
            }
            false
        }
    }
    
    /**
     * Check if role exists in guild
     */
    fun roleExists(guild: Guild): Boolean {
        if (!enabled || roleId.isEmpty()) {
            return true
        }
        return guild.getRoleById(roleId) != null
    }
    
    /**
     * Get role name
     */
    fun getRoleName(guild: Guild): String? {
        if (!enabled || roleId.isEmpty()) {
            return null
        }
        return guild.getRoleById(roleId)?.name
    }
}
