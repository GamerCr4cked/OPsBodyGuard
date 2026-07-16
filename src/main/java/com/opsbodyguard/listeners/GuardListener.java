package com.opsbodyguard.listeners;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.guard.GuardMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public class GuardListener implements Listener {

    private final OPsBodyGuard plugin;

    public GuardListener(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGuardDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;

        if (!golem.hasMetadata("bodyguard")) return;

        plugin.getGuardManager().handleGuardDeath(golem);

        // Clear drops
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onPlayerInteractGuard(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof IronGolem golem)) return;

        if (!golem.hasMetadata("bodyguard")) return;

        Player player = event.getPlayer();
        BodyGuard guard = plugin.getGuardManager().getGuardByEntity(golem.getUniqueId());

        if (guard == null) return;

        // Only owner can interact
        if (!guard.getOwnerUUID().equals(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        // Sneak + Right-click opens All Guards GUI
        if (player.isSneaking()) {
            plugin.getGUIManager().openAllGuardsControlGUI(player);
        } else {
            // Normal right-click opens single guard control GUI
            plugin.getGUIManager().openGuardControlGUI(player, guard);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity damaged = event.getEntity();

        // Handle NEUTRAL mode - player hits entity to make guard attack
        if (damager instanceof Player player) {
            List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
            for (BodyGuard guard : guards) {
                if (guard.getMode() == GuardMode.NEUTRAL) {
                    IronGolem golem = guard.getEntity();
                    if (golem != null && damaged instanceof LivingEntity target) {
                        // Don't target other bodyguards
                        if (target instanceof IronGolem targetGolem && targetGolem.hasMetadata("bodyguard")) {
                            continue;
                        }
                        golem.setTarget(target);
                    }
                }
            }
        }

        // Handle NEUTRAL mode - someone hits owner, guard defends
        if (damaged instanceof Player owner) {
            List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(owner.getUniqueId());
            for (BodyGuard guard : guards) {
                if (guard.getMode() == GuardMode.NEUTRAL) {
                    IronGolem golem = guard.getEntity();
                    if (golem != null && damager instanceof LivingEntity attacker) {
                        // Don't target other bodyguards
                        if (attacker instanceof IronGolem attackerGolem && attackerGolem.hasMetadata("bodyguard")) {
                            continue;
                        }
                        golem.setTarget(attacker);
                    }
                }
            }
        }

        // Handle NEUTRAL mode - someone hits the guard, guard fights back
        if (damaged instanceof IronGolem damagedGolem && damagedGolem.hasMetadata("bodyguard")) {
            BodyGuard guard = plugin.getGuardManager().getGuardByEntity(damagedGolem.getUniqueId());
            if (guard != null && guard.getMode() == GuardMode.NEUTRAL) {
                if (damager instanceof LivingEntity attacker) {
                    // Don't target owner
                    if (attacker instanceof Player player && guard.getOwnerUUID().equals(player.getUniqueId())) {
                        return;
                    }
                    // Don't target other bodyguards
                    if (attacker instanceof IronGolem attackerGolem && attackerGolem.hasMetadata("bodyguard")) {
                        return;
                    }
                    damagedGolem.setTarget(attacker);
                }
            }
        }

        // Prevent guard from attacking owner
        if (damager instanceof IronGolem golem && golem.hasMetadata("bodyguard")) {
            BodyGuard guard = plugin.getGuardManager().getGuardByEntity(golem.getUniqueId());
            if (guard != null && damaged instanceof Player player) {
                if (guard.getOwnerUUID().equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }

        // Prevent owner from damaging their own guard
        if (damaged instanceof IronGolem golem && golem.hasMetadata("bodyguard")) {
            BodyGuard guard = plugin.getGuardManager().getGuardByEntity(golem.getUniqueId());
            if (guard != null && damager instanceof Player player) {
                if (guard.getOwnerUUID().equals(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof IronGolem golem)) return;

        if (!golem.hasMetadata("bodyguard")) return;

        BodyGuard guard = plugin.getGuardManager().getGuardByEntity(golem.getUniqueId());
        if (guard == null) return;

        // Prevent targeting owner
        if (event.getTarget() instanceof Player player) {
            if (guard.getOwnerUUID().equals(player.getUniqueId())) {
                event.setCancelled(true);
                return;
            }
        }

        // Prevent targeting when order is stopped (gstop)
        if (guard.isOrderActive() && guard.isOrderStopped()) {
            event.setCancelled(true);
            return;
        }

        // Prevent targeting in PASSIVE mode
        if (guard.getMode() == GuardMode.PASSIVE) {
            event.setCancelled(true);
            return;
        }

        // In NEUTRAL mode, only allow targeting if it was set by our code (player hit or got hit)
        // Cancel automatic Iron Golem targeting behavior
        if (guard.getMode() == GuardMode.NEUTRAL) {
            // Only allow if target was explicitly set (reason is CUSTOM or TARGET_ATTACKED_OWNER)
            EntityTargetEvent.TargetReason reason = event.getReason();
            if (reason != EntityTargetEvent.TargetReason.CUSTOM &&
                reason != EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY &&
                reason != EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Respawn guards when owner joins
        plugin.getGuardManager().respawnPlayerGuards(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Despawn guards when owner quits
        plugin.getGuardManager().despawnPlayerGuards(event.getPlayer().getUniqueId());
    }
}
