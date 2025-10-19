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
# ========================================
# BASIC SETTINGS
# ========================================
discord:
  token: "YOUR_DISCORD_BOT_TOKEN"

database:
  enabled: true
  host: "localhost"
  port: 3306
  database: "mcdiscord"
  username: "root"
  password: "your_password"

plugin:
  debug: false                    # Show detailed error messages
  max-accounts-per-discord: 10    # Max Minecraft accounts per Discord
  code-expiry-minutes: 30         # How long codes last

# Account verification requirements (heuristic-based)
verification:
  require-email-verified: true     # Require likely email verification
  require-phone-verified: true     # Require likely phone verification

# ========================================
# MINECRAFT KICK MESSAGE (with colors!)
# ========================================
kick-message:
  title: "&cYou must link your Discord account!"
  code-prefix: "&eYour code: "
  code-color: "&a&l"              # Green and bold
  steps-title: "&7How to link:"
  steps:
    - "&71) Go to Discord"
    - "&72) Find admin to run &e/setup-link-discord"
    - "&73) Click &b'Link Account' &7button"
    - "&74) Enter code: &e{code}"
    - "&75) Rejoin server"
  expiry-warning: "&cExpires in &e{minutes} minutes!"
  add-empty-lines: true           # Add spacing between sections

# ========================================
# DISCORD EMBED CUSTOMIZATION
# ========================================
discord-embed:
  title: "🔗 Link Your Minecraft Account"
  color: "#00FF00"                # Hex color code
  description: |
    **How to link:**
    1️⃣ Click button below
    2️⃣ Enter 4-digit code
    3️⃣ Rejoin Minecraft
  
  button-text: "Link Account"
  button-emoji: "🎮"
  button-color: "PRIMARY"         # PRIMARY, SECONDARY, SUCCESS, DANGER

# ========================================
# ADVANCED SETTINGS
# ========================================
advanced:
  kick-delay-ticks: 5             # Delay before kick (prevents plugin conflicts)
  ops-bypass-linking: true        # Let operators skip linking
  log-successful-links: true      # Log links to console
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
- `{minutes}` - Expiry time
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
- ✅ **Account Verification** - Require likely verified email/phone on Discord (heuristic-based, configurable)
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

## ⚠️ IMPORTANT: Plugin Reload Warning

**This plugin CANNOT be safely reloaded using PlugMan or similar tools.**

### Why?
The plugin uses JDA (Java Discord API) which creates persistent connections that cannot be properly unloaded during reloads, causing class loading conflicts.

### Solution:
- **Restart your server** instead of using reload commands
- If you must reload: `/plugman unload MCDiscordLink` → wait 5 seconds → `/plugman load MCDiscordLink`
- **Server restart is always recommended** for best stability

## 🔐 Account Verification System

### How It Works
The plugin can require Discord users to have "verified" accounts before linking. However, Discord's API doesn't expose exact verification status to bots.

### Heuristic Approach
Instead, we use intelligent indicators:

**Email Verification Indicators:**
- Account 14+ days old with avatar/roles
- User has Discord badges/flags

**Phone Verification Indicators:**
- Account 30+ days old with activity
- User has server permissions

### Configuration
```yaml
verification:
  require-email-verified: true   # Enable email verification checks
  require-phone-verified: true   # Enable phone verification checks
```

Set to `false` to disable verification requirements.

**Note:** This is heuristic-based and filters out most new/fake accounts while allowing legitimate users.