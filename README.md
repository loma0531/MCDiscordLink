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
# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                    MCDiscordLink Configuration                            ║
# ║                                                                           ║
# ║  ไฟล์ตั้งค่าหลักของ Plugin                                                      ║
# ║  แก้ไขค่าต่างๆ ตามต้องการ ใช้ /mclink reload เพื่อโหลดค่าใหม่                           ║
# ╚═══════════════════════════════════════════════════════════════════════════╝

# ═══════════════════════════════════════════════════════════════════════════
# 1. DISCORD BOT SETTINGS
# ═══════════════════════════════════════════════════════════════════════════
discord:
  token: "PUT_YOUR_DISCORD_TOKEN_HERE"

# ═══════════════════════════════════════════════════════════════════════════
# 2. DATABASE SETTINGS
# ═══════════════════════════════════════════════════════════════════════════
database:
  enabled: true
  host: "localhost"
  port: 3306
  database: "mcdiscord"
  username: "root"
  password: "your_password_here"
  useSSL: false
  allowPublicKeyRetrieval: true
  # ตั้งค่า Timezone (ถ้ามีปัญหารหัสหมดอายุ)
  use-timezone: false  # เปิดใช้งาน timezone (false = ใช้เวลาเครื่อง server)
  timezone: "Asia/Bangkok"  # ตั้งค่าเมื่อ use-timezone: true
  # ตัวอย่าง timezone: Asia/Bangkok, Asia/Tokyo, America/New_York, UTC
  # หรือใช้ offset: +07:00, -05:00
  connection-pool:
    maximum-pool-size: 10
    minimum-idle: 2
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000

# ═════════════(when pl══════════════════════════════════════════════════════
# 3k-message:GE (เมื่อผู้เล่นยังไม่ได้ลิงก์บัญชี)
# Placeholders: {code}, {minutes}
# ═════════════════════ลิงก์ของคุณ:══════════════════════════════════════════
kick-message:
  title: "&cคุณต้องลิงก์บัญชี Discord ก่อนถึงจะเล่นบนเซิร์ฟเวอร์นี้ได้!"
  linking-code: "&eรหัสลิงก์ของคุณ: &a&l{code}"
  description: "&7วิธีลิงก์บัญชี:"
  steps:
    - "&71) เข้าร่วม Discord server ของเรา discord.gg/linkserver"
    - "&72) ไปที่ช่อง 'Link Account' ในเซิร์ฟเวอร์"
    - "&73) คลิกปุ่ม &b'Link Minecraft Accounts"
    - "&74) กรอกรหัสของคุณ: &a&l{code}"
    - "&75) เข้าร่วมเซิร์ฟเวอร์ Minecraft อีกครั้ง"
  expiry-warning: "&cรหัสจะหมดอายุใน &e{minutes} นาที&c!"
code-or-message: "&cไม่สามารถสร้างรหัสได้ กรุณาติดต่อผู้ดูแลระบบ"

# ระยะเวลาหมดอายุของรหัส (นาที)
code-expiry-minutes: 30

# ═══════════════════════════════════════════════════════════════════════════
# 4. DISCORD ACCOUNT VERIFICATION (การตรวจสอบบัญชี Discord)
# ═══════════════════════════════════════════════════════════════════════════
discoed-account-verification:
  enabled: true
  min-account-age-days: 7      # บัญชี Discord ต้องอายุอย่างน้อย (วัน)
  min-server-join: 10m         # ต้องอยู่ในเซิร์ฟเวอร์อย่างน้อย (m=นาที, h=ชั่วโมง, d=วัน)

# ═══════════════════════════════════════════════════════════════════════════
# 5. ACCOUNT LIMITS (จำกัดจำนวนบัญชี)
# ═══════════════════════════════════════════════════════════════════════════
limit-setting:
  enabled: true
  max-minecraft-accounts-per-discord-account: 10  # 1 Discord ผูกได้สูงสุดกี่บัญชี Minecraft

# ═══════════════════════════════════════════════════════════════════════════
# 6. DISCORD EMBED SETTINGS (ข้อความในช่อง Link Account)
# Placeholders: {min-account-age-days}, {min-server-join}, {max}
# ═══════════════════════════════════════════════════════════════════════════
discord-embed:
  title: "🔗 ลิงก์บัญชี Minecraft ของคุณ"
  description: |
    **วิธีลิงก์บัญชี:**
    1️⃣ คลิกปุ่มด้านล่าง  
    2️⃣ กรอกรหัส 4 หลักที่ได้รับจาก Minecraft  
    3️⃣ เข้าร่วม Minecraft server อีกครั้ง  
    
    **ข้อกำหนด:**
    • บัญชี Discord ต้องมีอายุอย่างน้อย {min-account-age-days} วัน  
    • ต้องอยู่ใน server อย่างน้อย {min-server-join}
    
    **สำคัญ:**
    • 1 Discord ผูกได้สูงสุด {max} บัญชี  
    • สามารถเพิ่มบัญชีเพิ่มได้โดยกดปุ่มอีกครั้ง
  color: "#00FF00"
  button-text: "ลิงก์ Minecraft"
  button-emoji: "🎮"
  button-color: "PRIMARY"
  add-empty-lines: true
  fields:
    - name: "📋 ขั้นตอนละเอียด"
      value: |
        • เข้า Minecraft → ถูกเตะพร้อมรหัส 4 หลัก  
        • กลับมาที่ Discord → กดปุ่มด้านล่าง  
        • กรอกรหัส 4 หลักใน popup  
        • เข้าร่วม Minecraft อีกครั้ง → เสร็จสิ้น!  
      inline: false
    - name: "ℹ️ ข้อมูลเพิ่มเติม"
      value: "หากมีปัญหา ติดต่อทีมงานที่ช่อง #support"
      inline: false

# ═══════════════════════════════════════════════════════════════════════════
# 7. DISCORD MODAL (Popup สำหรับกรอกรหัส)
# ═══════════════════════════════════════════════════════════════════════════
discord-modal:
  title: "Enter Your Minecraft Code"
  input-label: "4-Digit Code"
  input-placeholder: "Enter the code from Minecraft server"

# ═══════════════════════════════════════════════════════════════════════════
# 8. DISCORD RESPONSE MESSAGES (ข้อความตอบกลับใน Discord)
# Placeholders: {max}, {minecraft}, {discord}, {count}
# ═══════════════════════════════════════════════════════════════════════════
discord-responses:
  invalid-code: "❌ กรุณากรอกรหัส 4 หลักที่ถูกต้อง!"
  code-expired: "❌ รหัสหมดอายุหรือไม่ถูกต้อง กรุณาเข้าเซิร์ฟเวอร์ Minecraft อีกครั้งเพื่อรับรหัสใหม่"
  account-limit: "❌ คุณผูกบัญชี Minecraft ครบจำนวนสูงสุดแล้ว ({max} บัญชี)!"
  already-linked: "❌ บัญชี Minecraft นี้ถูกเชื่อมโยงกับ Discord อื่นแล้ว!"
  link-error: "❌ เกิดข้อผิดพลาดในการเชื่อมโยง กรุณาลองใหม่อีกครั้ง"
  no-permission: "❌ คุณต้องมีสิทธิ์ Administrator เพื่อใช้คำสั่งนี้!"
  guild-only: "❌ คำสั่งนี้ใช้ได้เฉพาะในเซิร์ฟเวอร์ Discord เท่านั้น!"
  success-message: |
    ✅ **เชื่อมโยงบัญชีสำเร็จ!**
    
    🎮 **Minecraft:** {minecraft}
    👤 **Discord:** {discord}
    📊 **บัญชีที่เชื่อมโยง:** {count}/{max}
    
    ตอนนี้คุณสามารถเข้าเซิร์ฟเวอร์ Minecraft ได้แล้ว!

# ═══════════════════════════════════════════════════════════════════════════
# 9. LOGGING SETTINGS (บันทึก Log ใน Discord)
# ═══════════════════════════════════════════════════════════════════════════
logging:
  enabled: true
  log-channel-id: ""  # ใส่ Channel ID ที่ต้องการส่ง log (เว้นว่างเพื่อปิด)

# ═══════════════════════════════════════════════════════════════════════════
# 10. ROLE MANAGEMENT (ให้ยศอัตโนมัติหลังลิงก์)
# ═══════════════════════════════════════════════════════════════════════════
give-role-after-link:
  enabled: true
  give-role-id: ""  # ใส่ Role ID ที่ต้องการให้ (เว้นว่างเพื่อปิด)

# ═══════════════════════════════════════════════════════════════════════════
# 11. ADVANCED SETTINGS (ตั้งค่าขั้นสูง)
# ═══════════════════════════════════════════════════════════════════════════
advanced:
  kick-delay-ticks: 20           # ดีเลย์ก่อนเตะผู้เล่น (ticks, 20 ticks = 1 วินาที)
  ops-bypass-linking: true      # ให้ OP ข้ามการลิงก์ได้หรือไม่
  debug: false                  # เปิด Debug mode (แสดงข้อความเพิ่มเติมใน console)

```
### Placeholders
```yaml
# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                    MCDiscordLink Messages                                 ║
# ║                                                                           ║
# ║  ไฟล์นี้เก็บข้อความทั้งหมดของ Plugin                                               ║
# ║  แก้ไขได้ตามต้องการ ใช้ /mclink reload เพื่อโหลดค่าใหม่                               ║ 
# ║                                                                           ║
# ║  📝 Placeholders ที่ใช้ได้:                                                    ║
# ║  {player}       = ชื่อผู้เล่น Minecraft                                         ║
# ║  {uuid}         = Minecraft UUID                                          ║
# ║  {minecraft}    = ชื่อ Minecraft (เหมือน {player})                            ║
# ║  {discord}      = ชื่อ Discord                                              ║
# ║  {discord-id}   = Discord ID                                              ║
# ║  {code}         = รหัสยืนยัน 4 หลัก                                           ║
# ║  {count}        = จำนวนบัญชีปัจจุบัน                                           ║
# ║  {max}          = จำนวนบัญชีสูงสุด                                            ║
# ║  {number}       = หมายเลขลำดับ                                              ║
# ║  {min}          = ค่าขั้นต่ำ (วัน/นาที)                                          ║
# ║  {current}      = ค่าปัจจุบัน (วัน/นาที)                                         ║
# ║  {date}         = วันที่และเวลา                                               ║
# ║  {seconds}      = วินาที                                                    ║
# ║  {error}        = ข้อความ error                                            ║
# ║  {command}      = ชื่อคำสั่ง                                                  ║
# ║  {sender}       = ผู้ส่งคำสั่ง                                                 ║
# ║  {args}         = arguments ของคำสั่ง                                       ║
# ║  {role}         = ชื่อยศ Discord                                            ║
# ║  {role-id}      = Role ID                                                 ║
# ║  {channel-id}   = Channel ID                                              ║
# ║  {user}         = ชื่อผู้ใช้ทั่วไป                                                ║
# ╚═══════════════════════════════════════════════════════════════════════════╝
