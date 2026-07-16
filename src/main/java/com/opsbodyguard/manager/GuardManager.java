package com.opsbodyguard.manager;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.guard.GuardMode;
import com.opsbodyguard.guard.PaymentType;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuardManager {

    private final OPsBodyGuard plugin;
    private final Map<UUID, List<BodyGuard>> playerGuards;
    private final Map<UUID, BodyGuard> guardsByEntity;
    private final File dataFile;

    public GuardManager(OPsBodyGuard plugin) {
        this.plugin = plugin;
        this.playerGuards = new HashMap<>();
        this.guardsByEntity = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "guards.yml");
    }

    public BodyGuard spawnGuard(Player owner, PaymentType paymentType, int days) {
        BodyGuard bodyGuard = new BodyGuard(owner.getUniqueId(), paymentType, days);

        Location spawnLoc = owner.getLocation().add(1, 0, 1);
        IronGolem golem = spawnGolemEntity(spawnLoc, owner.getUniqueId(), owner.getName());

        bodyGuard.setGuardUUID(golem.getUniqueId());
        bodyGuard.setSpawnLocation(spawnLoc);

        // Add to maps
        playerGuards.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(bodyGuard);
        guardsByEntity.put(golem.getUniqueId(), bodyGuard);

        return bodyGuard;
    }

    // Helper method to spawn the iron golem entity
    private IronGolem spawnGolemEntity(Location location, UUID ownerUUID, String ownerName) {
        return location.getWorld().spawn(location, IronGolem.class, entity -> {
            // Set custom name
            entity.setCustomName(MessageUtil.color("&b" + ownerName + "'s Guard"));
            entity.setCustomNameVisible(true);

            // Set health
            double health = plugin.getConfig().getDouble("guard.health", 100);
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
            entity.setHealth(health);

            // Make it not naturally aggressive
            entity.setPlayerCreated(true);

            // Set metadata to identify this as a bodyguard
            entity.setMetadata("bodyguard", new FixedMetadataValue(plugin, ownerUUID.toString()));
            entity.setMetadata("bodyguard_owner", new FixedMetadataValue(plugin, ownerUUID.toString()));

            // Prevent it from despawning
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
        });
    }

    // Respawn all guards for a player (called on player join)
    public void respawnPlayerGuards(Player player) {
        List<BodyGuard> guards = playerGuards.get(player.getUniqueId());
        if (guards == null) return;

        for (BodyGuard guard : guards) {
            if (guard.isSpawned()) continue; // Already has entity

            Location loc = guard.getSpawnLocation();
            if (loc == null) loc = player.getLocation().add(1, 0, 1);

            IronGolem golem = spawnGolemEntity(loc, player.getUniqueId(), player.getName());
            guard.setGuardUUID(golem.getUniqueId());
            guardsByEntity.put(golem.getUniqueId(), guard);
        }

        if (guards.size() > 0) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.guard-arrived"));
        }
    }

    // Despawn all guards for a player (called on player quit)
    public void despawnPlayerGuards(UUID playerUUID) {
        List<BodyGuard> guards = playerGuards.get(playerUUID);
        if (guards == null) return;

        for (BodyGuard guard : guards) {
            IronGolem entity = guard.getEntity();
            if (entity != null && !entity.isDead()) {
                // Save current location before despawning
                guard.setSpawnLocation(entity.getLocation());
                entity.remove();
            }
            guardsByEntity.remove(guard.getGuardUUID());
            guard.setGuardUUID(null); // Mark as not spawned
        }
    }

    public void removeGuard(BodyGuard bodyGuard) {
        // Remove entity
        IronGolem entity = bodyGuard.getEntity();
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }

        // Remove from maps
        UUID ownerUUID = bodyGuard.getOwnerUUID();
        List<BodyGuard> guards = playerGuards.get(ownerUUID);
        if (guards != null) {
            guards.remove(bodyGuard);
            if (guards.isEmpty()) {
                playerGuards.remove(ownerUUID);
            }
        }
        guardsByEntity.remove(bodyGuard.getGuardUUID());
    }

    public List<BodyGuard> getPlayerGuards(UUID playerUUID) {
        return playerGuards.getOrDefault(playerUUID, new ArrayList<>());
    }

    public BodyGuard getGuardByEntity(UUID entityUUID) {
        return guardsByEntity.get(entityUUID);
    }

    public int getPlayerGuardCount(UUID playerUUID) {
        return getPlayerGuards(playerUUID).size();
    }

    public boolean canHaveMoreGuards(UUID playerUUID) {
        int maxGuards = plugin.getConfig().getInt("limits.max-guards-per-player", 1);
        if (maxGuards == -1) return true;
        return getPlayerGuardCount(playerUUID) < maxGuards;
    }

    /**
     * Process MC day payments/expiry for all guards.
     * Called when a new Minecraft day starts.
     */
    public void processMCDayPayments() {
        for (List<BodyGuard> guards : new ArrayList<>(playerGuards.values())) {
            for (BodyGuard guard : new ArrayList<>(guards)) {
                if (guard.isPerDayPayment()) {
                    // Per-day payment - charge every MC day
                    processPerDayPayment(guard);
                } else {
                    // Custom days payment - decrement days remaining
                    processCustomDaysExpiry(guard);
                }
            }
        }
    }

    private void processPerDayPayment(BodyGuard guard) {
        Player owner = guard.getOwner();
        double price = plugin.getConfig().getDouble("price.per-day", 50000);
        String priceFormatted = MessageUtil.formatMoneyClean(price);

        if (owner != null && owner.isOnline()) {
            if (plugin.getEconomyManager().withdraw(owner, price)) {
                guard.setLastPaymentTime(System.currentTimeMillis());
                // Notify owner of payment
                MessageUtil.send(owner, plugin.getConfig().getString("messages.daily-payment", "&eDaily guard payment: &f$%price%")
                        .replace("%price%", priceFormatted));
            } else {
                // Can't pay - remove guard
                MessageUtil.send(owner, plugin.getConfig().getString("messages.payment-failed"));
                removeGuard(guard);
            }
        } else {
            // Owner offline - try to withdraw anyway
            if (!plugin.getEconomyManager().withdraw(guard.getOwnerUUID(), price)) {
                // Can't pay - remove guard (notify when they come online via other means)
                removeGuard(guard);
            } else {
                guard.setLastPaymentTime(System.currentTimeMillis());
            }
        }
    }

    private void processCustomDaysExpiry(BodyGuard guard) {
        guard.decrementDays();
        int remaining = guard.getDaysRemaining();
        Player owner = guard.getOwner();

        if (remaining <= 0) {
            // Guard expired
            if (owner != null && owner.isOnline()) {
                MessageUtil.send(owner, plugin.getConfig().getString("messages.guard-expired", "&cYour guard's contract has expired!"));
            }
            removeGuard(guard);
        } else if (remaining <= 3 && owner != null && owner.isOnline()) {
            // Warn owner when 3 or fewer days remaining
            MessageUtil.send(owner, plugin.getConfig().getString("messages.days-warning", "&eYour guard has &f%days% &edays remaining!")
                    .replace("%days%", String.valueOf(remaining)));
        }
    }

    // Legacy method for compatibility - now uses MC day tracking
    public void processPayments() {
        // Kept for backwards compatibility but no longer used
    }

    public void updateGuardBehaviors() {
        for (BodyGuard guard : guardsByEntity.values()) {
            IronGolem entity = guard.getEntity();
            Player owner = guard.getOwner();

            if (entity == null || entity.isDead()) {
                continue;
            }

            if (owner == null || !owner.isOnline()) {
                continue;
            }

            // Check if guard has active target - prioritize attacking over following
            if (entity.getTarget() != null && !entity.getTarget().isDead()) {
                double maxRange = getMaxRangeForMode(guard.getMode());
                double targetDistance = entity.getLocation().distance(entity.getTarget().getLocation());

                if (targetDistance <= maxRange) {
                    // Target in range, let guard continue chasing
                    handleGuardMode(guard, entity, owner);
                    continue;
                } else {
                    // Target out of range, clear target and resume follow
                    entity.setTarget(null);
                }
            }

            // Follow owner logic
            double followDistance = plugin.getConfig().getDouble("guard.follow-distance", 10); // Changed default to 10
            double teleportDistance = plugin.getConfig().getDouble("guard.teleport-distance", 15);
            double distance = entity.getLocation().distance(owner.getLocation());

            // Teleport if too far
            if (distance > teleportDistance) {
                Location teleportLoc = owner.getLocation().add(1, 0, 1);
                entity.teleport(teleportLoc);
            }
            // Move towards owner if outside follow distance
            else if (distance > followDistance) {
                entity.getPathfinder().moveTo(owner.getLocation(), 1.0);
            }

            // Handle behavior based on mode
            handleGuardMode(guard, entity, owner);
        }
    }

    private double getMaxRangeForMode(GuardMode mode) {
        return switch (mode) {
            case CUSTOM_ATTACK, NEUTRAL -> 50.0;
            case AGGRESSIVE, HOSTILE -> 10.0;
            case PASSIVE -> 0.0;
        };
    }

    private void handleGuardMode(BodyGuard guard, IronGolem entity, Player owner) {
        // If Order toggle is ON and gstop was used, don't attack
        if (guard.isOrderActive() && guard.isOrderStopped()) {
            entity.setTarget(null);
            return;
        }

        switch (guard.getMode()) {
            case AGGRESSIVE -> handleAggressiveMode(entity, owner);
            case HOSTILE -> handleHostileMode(entity, owner);
            case CUSTOM_ATTACK -> handleCustomAttackMode(guard, entity);
            case PASSIVE -> entity.setTarget(null);
            case NEUTRAL -> {} // Handled by hit event
        }
    }

    private void handleAggressiveMode(IronGolem entity, Player owner) {
        if (entity.getTarget() != null && !entity.getTarget().isDead()) return;

        // Find nearest entity (not owner)
        double range = 10.0;
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity nearby : entity.getNearbyEntities(range, range, range)) {
            if (nearby instanceof LivingEntity living && !(nearby instanceof ArmorStand)) {
                if (nearby.equals(owner)) continue;
                if (nearby instanceof IronGolem golem && golem.hasMetadata("bodyguard")) continue;

                double dist = entity.getLocation().distance(nearby.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = nearby;
                }
            }
        }

        if (nearest instanceof LivingEntity target) {
            entity.setTarget(target);
        }
    }

    private void handleHostileMode(IronGolem entity, Player owner) {
        if (entity.getTarget() != null && !entity.getTarget().isDead()) return;

        // Find nearest hostile mob
        double range = 10.0;
        Monster nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity nearby : entity.getNearbyEntities(range, range, range)) {
            if (nearby instanceof Monster monster) {
                double dist = entity.getLocation().distance(nearby.getLocation());
                if (dist < nearestDist) {
                    nearestDist = dist;
                    nearest = monster;
                }
            }
        }

        if (nearest != null) {
            entity.setTarget(nearest);
        }
    }

    private void handleCustomAttackMode(BodyGuard guard, IronGolem entity) {
        String targetName = guard.getCustomTargetName();
        if (targetName == null || targetName.isEmpty()) return;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && target.isOnline()) {
            double range = 50.0; // Changed from 15 to 50
            if (entity.getLocation().distance(target.getLocation()) <= range) {
                entity.setTarget(target);
            }
        }
    }

    public void saveGuards() {
        FileConfiguration config = new YamlConfiguration();

        int index = 0;
        for (Map.Entry<UUID, List<BodyGuard>> entry : playerGuards.entrySet()) {
            for (BodyGuard guard : entry.getValue()) {
                String path = "guards." + index;
                // guardUUID can be null if entity is despawned
                if (guard.getGuardUUID() != null) {
                    config.set(path + ".guardUUID", guard.getGuardUUID().toString());
                }
                config.set(path + ".ownerUUID", guard.getOwnerUUID().toString());
                config.set(path + ".mode", guard.getMode().name());
                config.set(path + ".paymentType", guard.getPaymentType().name());
                config.set(path + ".daysRemaining", guard.getDaysRemaining());
                config.set(path + ".lastPaymentTime", guard.getLastPaymentTime());
                config.set(path + ".customTargetName", guard.getCustomTargetName());

                if (guard.getSpawnLocation() != null) {
                    Location loc = guard.getSpawnLocation();
                    config.set(path + ".location.world", loc.getWorld().getName());
                    config.set(path + ".location.x", loc.getX());
                    config.set(path + ".location.y", loc.getY());
                    config.set(path + ".location.z", loc.getZ());
                }
                index++;
            }
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            MessageUtil.logError("Could not save guards data: " + e.getMessage());
        }
    }

    public void loadGuards() {
        if (!dataFile.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(dataFile);

        if (!config.contains("guards")) return;

        int loadedCount = 0;
        for (String key : config.getConfigurationSection("guards").getKeys(false)) {
            String path = "guards." + key;

            try {
                UUID ownerUUID = UUID.fromString(config.getString(path + ".ownerUUID"));
                GuardMode mode = GuardMode.valueOf(config.getString(path + ".mode"));
                PaymentType paymentType = PaymentType.valueOf(config.getString(path + ".paymentType"));
                int daysRemaining = config.getInt(path + ".daysRemaining");
                long lastPaymentTime = config.getLong(path + ".lastPaymentTime");
                String customTargetName = config.getString(path + ".customTargetName");

                // Load location
                Location loc = null;
                if (config.contains(path + ".location.world")) {
                    String worldName = config.getString(path + ".location.world");
                    var world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        loc = new Location(world,
                                config.getDouble(path + ".location.x"),
                                config.getDouble(path + ".location.y"),
                                config.getDouble(path + ".location.z"));
                    }
                }

                // Create BodyGuard with null entity UUID (not spawned yet)
                BodyGuard bodyGuard = new BodyGuard(null, ownerUUID, mode, paymentType,
                        daysRemaining, lastPaymentTime, customTargetName);
                bodyGuard.setSpawnLocation(loc);

                // Add to playerGuards map only (not guardsByEntity since no entity yet)
                playerGuards.computeIfAbsent(ownerUUID, k -> new ArrayList<>()).add(bodyGuard);
                loadedCount++;

            } catch (Exception e) {
                MessageUtil.logWarning("Could not load guard " + key + ": " + e.getMessage());
            }
        }

        MessageUtil.logInfo("Loaded " + loadedCount + " guard data entries. Guards will spawn when owners join.");
    }

    public void handleGuardDeath(IronGolem entity) {
        BodyGuard guard = getGuardByEntity(entity.getUniqueId());
        if (guard == null) return;

        Player owner = guard.getOwner();
        if (owner != null && owner.isOnline()) {
            MessageUtil.send(owner, plugin.getConfig().getString("messages.guard-died"));
        }

        // Remove from maps
        UUID ownerUUID = guard.getOwnerUUID();
        List<BodyGuard> guards = playerGuards.get(ownerUUID);
        if (guards != null) {
            guards.remove(guard);
            if (guards.isEmpty()) {
                playerGuards.remove(ownerUUID);
            }
        }
        guardsByEntity.remove(guard.getGuardUUID());
    }
}
