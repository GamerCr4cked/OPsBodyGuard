# ===============================================
#        OPsBodyGuard Plugin v3 - Full Guide
# ===============================================

## What is OPsBodyGuard?

A premium Iron Golem bodyguard system for Minecraft servers!
Players can purchase personal bodyguards that follow them around,
protect them from enemies, and can be controlled with different attack modes.

Requires: Paper/Spigot 1.21+, Vault + Economy plugin (like EssentialsX)

---

## Commands

/buyguard - Opens the bodyguard shop GUI
/greload  - Reloads the plugin config (admin only)

---

## Permissions

opsbodyguard.buy    - Allow using /buyguard (default: true)
opsbodyguard.reload - Allow using /greload (default: op)

---

## How It Works

1. Player runs /buyguard
2. Choose payment type:
   - Per-Day Payment: Pay $50k every Minecraft day (auto-deducted)
   - Custom Days: Pay upfront for 5-30 days with discounts!

3. Guard spawns as an Iron Golem next to the player
4. Right-click your guard to open Control GUI
5. Sneak + Right-click to open All Guards Control (if you have multiple)

---

## Payment System

### Per-Day Payment
- Price: $50,000 per Minecraft day (configurable)
- Auto-deducts from player balance when a new MC day starts
- If player can't pay, guard is removed
- Works even when player is offline!

### Custom Days Payment (Bulk Purchase)
Pay upfront for multiple days with discounts!

| Days | Discount | Total Price (at $50k/day) |
|------|----------|---------------------------|
| 5    | 5%       | $237,500                  |
| 10   | 15%      | $425,000                  |
| 15   | 20%      | $600,000                  |
| 20   | 25%      | $750,000                  |
| 25   | 27.5%    | $906,250                  |
| 30   | 30%      | $1,050,000                |

Discount scales linearly from min-discount (5%) to max-discount (30%)

---

## Guard Modes

### Aggressive (Diamond Sword icon)
- Attacks ANY entity within 10 blocks
- Players, mobs, animals - everything!

### Neutral (Lead icon)
- Only attacks entities that YOU hit first
- 50 block chase range
- Hit an enemy and guard will chase them down!

### Hostile (Rotten Flesh icon)
- Only attacks hostile mobs (zombies, skeletons, etc.)
- 10 block range
- Great for PvE protection

### Passive (Wheat icon)
- Won't attack anything
- Just follows you around

### Custom Attack (Player Head icon)
- Target a specific player by name
- 50 block chase range
- Guard will hunt that player relentlessly!

---

## Order Toggle (Chat Commands)

The Order Toggle is a special feature that works WITH any mode!
When enabled, you can control your guard with chat commands.

How to use:
1. Open Guard Control GUI
2. Click "Order Toggle" to turn it ON
3. Now you can type in chat:
   - gstop  - Guard stops attacking immediately
   - gattack - Guard resumes attacking based on its mode

Works with any capitalization: GSTOP, GStop, gSTOP all work!

Example: Guard is in Aggressive mode attacking everything.
Type "gstop" - guard stops. Type "gattack" - guard resumes.

---

## Guard Behavior

- Guards follow within 10 blocks of owner
- If owner is 15+ blocks away, guard teleports to them
- Guards despawn when owner logs out (saves server resources)
- Guards respawn when owner logs back in
- Guard data persists through server restarts!
- Maximum 5 guards per player (configurable)
- Guard health: 100 HP (configurable)

---

## GUI Overview

### Main Shop (/buyguard)
- Click the Iron Golem egg to start purchase

### Payment Type Selection
- Per-Day Payment (left) - Pay daily
- Custom Days (right) - Pay upfront with discount

### Custom Days Selection
- 5 Days, 10 Days, 20 Days, 30 Days shortcuts
- Custom Days option - type any number (5-30)

### Guard Control (Right-click guard)
- Row of mode buttons (Aggressive, Neutral, Hostile, Passive, Custom Attack)
- Order Toggle button
- Cancel Guard button (with confirmation)

### All Guards Control (Sneak + Right-click guard)
- Same as above but applies to ALL your guards at once!

---

## Configuration (config.yml)

### Prices
```yaml
price:
  per-day: 50000  # Price per MC day
```

### Limits
```yaml
limits:
  max-guards-per-player: 5  # Use -1 for unlimited
```

### Guard Stats
```yaml
guard:
  health: 100           # Guard HP (Iron Golem default is 100)
  teleport-distance: 15 # Teleport if owner is this far
  follow-distance: 10   # Stay within this distance of owner
```

### Day Packages (GUI shortcuts)
```yaml
day-packages:
  5:
    enabled: true
  10:
    enabled: true
  20:
    enabled: true
  30:
    enabled: true
```

### Custom Days & Discounts
```yaml
custom-days:
  min-days: 5      # Minimum days player can buy
  max-days: 30     # Maximum days player can buy
  discount:
    min-discount: 5   # Discount % at min-days
    max-discount: 30  # Discount % at max-days
```

### Money Formatting
```yaml
formatting:
  use-abbreviations: true  # true: 50k, false: 50000.00
```

### All Messages are Customizable!
Use & for color codes. Available placeholders:
- %min%, %max% - Day limits
- %price% - Money amount
- %days% - Days remaining
- %mode% - Guard mode name
- %player% - Player name
- %status% - ON/OFF status

### GUI Customization
Change titles and item icons in config!

---

## Installation

1. Put OPsBodyGuard-v3.jar in your plugins folder
2. Make sure you have Vault + an economy plugin installed
3. Restart server
4. Edit config.yml to your liking
5. Use /greload to apply changes

---

## Tips for Server Owners

1. Balance the economy:
   - $50k/day is good for mid-game servers
   - Adjust based on your server's economy scale

2. Limit guards:
   - 5 guards max prevents lag and OP situations
   - Set to 1 for a more exclusive feel

3. Discounts encourage bulk purchases:
   - More money upfront = better for economy sinks
   - 30% discount at 30 days is significant!

4. Guards are powerful:
   - 100 HP Iron Golems hit hard
   - Consider lowering health for balance

---

## Credits

Plugin by: ITzGamerCracked_
Version: 3.0.0
API: Paper 1.21+
Dependencies: Vault + Economy Plugin (EssentialsX, CMI, TheNewEconomy, etc.)

---

## Support

Having issues? Check:
1. Vault is installed and working
2. Economy plugin is set up (EssentialsX, CMI, etc.)
3. Config syntax is correct (use /greload after changes)
4. Check console for error messages

---

Enjoy your bodyguards! Stay protected out there!
