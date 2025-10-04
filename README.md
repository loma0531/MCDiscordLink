# MCDiscordLink - Minecraft Discord Account Linking Plugin

## Overview
This plugin enforces Discord account linking for Minecraft players. Players must link their Minecraft account with Discord before they can join the server.

## Main Flow

### 1. Player Joins Server
- Plugin checks if player's UUID is linked in database
- If not linked → generates unique 4-digit code and kicks player with instructions
- Code is stored in database with expiration time

### 2. Discord Linking Process
- Admin uses `/setup-link-discord` slash command in Discord
- Bot creates embed with "Link Minecraft Account" button
- Player clicks button → modal appears asking for 4-digit code
- Bot verifies code and links accounts if valid

### 3. Account Management
- Each Discord account can link up to 10 Minecraft accounts (configurable)
- Codes expire after 30 minutes (configurable)
- Once linked, player can join server normally

## Configuration

### config.yml - Easy Setup Guide

```yaml
discord:
  token: "PUT_YOUR_DISCORD_TOKEN_HERE"

database:
  enabled: true
  host: "localhost"
  port: 3306
  database: "mcdiscord"
  username: "root"
  password: "your_password_here"
  connection-pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
  useSSL: false
  allowPublicKeyRetrieval: true

plugin:
  debug: false
  max-accounts-per-discord: 10
  code-expiry-minutes: 30

# ========================================
# MINECRAFT KICK MESSAGE SETTINGS
# ========================================
kick-message:
  # Colors: &c=red, &e=yellow, &a=green, &b=aqua, &d=light_purple, &f=white, &7=gray, &8=dark_gray, &9=blue
  title: "&cYou must link your Discord account to play on this server!"
  
  # Code display
  code-prefix: "&eYour linking code: "
  code-color: "&a&l"  # Green and bold
  
  # Instructions
  steps-title: "&7How to link your account:"
  steps:
    - "&71) Go to our Discord server discord.gg/linkserver"
    - "&72) Go to channel 'Link Account' in server"
    - "&73) Click the &b'Link Minecraft Account' &7button"
    - "&74) Enter your code: &a&l{code}"
    - "&75) Rejoin this server"
  
  # Footer
  expiry-warning: "&cCode expires in &e{minutes} minutes&c!"
  error-message: "&cFailed to generate code. Contact administrator."
  
  # Spacing (empty lines between sections)
  add-empty-lines: true

# ========================================
# DISCORD EMBED SETTINGS
# ========================================
discord-embed:
  # Main embed
  title: "🔗 Link Your Minecraft Account"
  color: "#00FF00"  # Green color (hex format)
  
  description: |
    **How to link your account:**
    
    1️⃣ Click the button below
    2️⃣ Enter the 4-digit code from Minecraft
    3️⃣ Rejoin the Minecraft server
    
    **Important:**
    • Maximum {max-accounts} Minecraft accounts per Discord
    • Code expires in {expiry-minutes} minutes
    • You can add more accounts by clicking again
  
  # Additional field
  show-detailed-steps: false
  field-title: "📋 Detailed Steps"
  field-description: |
    • Join Minecraft server → Get kicked with 4-digit code
    • Come back to Discord → Click button below
    • Enter the 4-digit code in the popup
    • Rejoin Minecraft server → Success!
  
  # Button customization
  button-text: "Link Minecraft Account"
  button-emoji: "🎮"
  button-color: "PRIMARY"  # PRIMARY, SECONDARY, SUCCESS, DANGER

# ========================================
# DISCORD MODAL (POPUP) SETTINGS
# ========================================
discord-modal:
  title: "Enter Your Minecraft Code"
  input-label: "4-Digit Code"
  input-placeholder: "Enter the code from Minecraft server"

# ========================================
# DISCORD RESPONSE MESSAGES
# ========================================
discord-responses:
  # Error messages
  invalid-code: "❌ Please enter a valid 4-digit code!"
  code-expired: "❌ Code is invalid or expired. Join Minecraft server again for a new code."
  account-limit: "❌ You have reached the maximum limit of {max-accounts} Minecraft accounts!"
  link-error: "❌ An error occurred while linking. Please try again."
  no-permission: "❌ You need Administrator permission to use this command!"
  guild-only: "❌ This command can only be used in a Discord server!"
  
  # Success message
  success-message: |
    ✅ **Successfully Linked!**
    
    🎮 **Minecraft:** {minecraft-name}
    👤 **Discord:** {discord-name}
    📊 **Linked Accounts:** {current-count}/{max-accounts}
    
    You can now join the Minecraft server!

# ========================================
# ADVANCED SETTINGS
# ========================================
advanced:
  # Kick delay to prevent plugin conflicts (in ticks, 20 ticks = 1 second)
  kick-delay-ticks: 5
  
  # Allow operators to bypass linking
  ops-bypass-linking: true
  
  # Log successful links to console
  log-successful-links: true
```

### 🎨 Color Codes for Minecraft Messages
- `&c` = Red
- `&e` = Yellow  
- `&a` = Green
- `&b` = Aqua
- `&d` = Light Purple
- `&f` = White
- `&7` = Gray
- `&8` = Dark Gray
- `&9` = Blue
- `&l` = Bold
- `&o` = Italic
- `&n` = Underline

### 🔧 Placeholders
- `{code}` - The 4-digit code
- `{expiry-minutes}` - Expiry time
- `{max-accounts}` - Account limit
- `{minecraft-name}` - Player name
- `{discord-name}` - Discord name
- `{current-count}` - Current linked accounts

## Database Tables

### discord_links
- Stores permanent account links
- Allows multiple Minecraft accounts per Discord ID

### temp_codes
- Stores temporary 4-digit codes
- Automatically cleaned up when expired

## Commands

### Discord Commands
- `/setup-link-discord` - Creates linking interface (Admin only)

### Minecraft
- No commands needed - all linking happens through Discord

## Setup Instructions

1. Create Discord bot and get token
2. Configure `config.yml` with bot token and database settings
3. Start server to create database tables
4. In Discord, use `/setup-link-discord` to create linking interface
5. Players will be kicked with codes when they try to join

## Features

- ✅ **Easy Configuration** - Simple, well-organized config.yml
- ✅ **Color Support** - Full Minecraft color code support (&c, &e, &a, etc.)
- ✅ **Customizable Messages** - Both kick messages and Discord embeds
- ✅ **Multiple Accounts** - Up to 10 Minecraft accounts per Discord (configurable)
- ✅ **Admin Controls** - Admin-only setup command with permissions
- ✅ **Smart Delays** - Prevents plugin conflicts with configurable kick delay
- ✅ **Operator Bypass** - Ops can skip linking (configurable)
- ✅ **Secure Codes** - 4-digit codes with expiration
- ✅ **Database Cleanup** - Automatic cleanup of expired codes
- ✅ **Logging Control** - Optional success logging
- ✅ **Multi-language Ready** - Easy translation through config

## Requirements

- Paper/Spigot 1.21+
- MySQL database
- Discord bot with appropriate permissions
