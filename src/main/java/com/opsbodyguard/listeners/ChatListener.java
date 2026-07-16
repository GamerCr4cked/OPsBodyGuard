package com.opsbodyguard.listeners;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.guard.GuardMode;
import com.opsbodyguard.gui.GUIManager;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatListener implements Listener {

    private final OPsBodyGuard plugin;

    public ChatListener(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    private GUIManager getGUIManager() {
        return plugin.getGUIManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        GUIManager guiManager = getGUIManager();
        if (guiManager == null) return;

        // Check for Order commands (gstop/gattack) - case insensitive
        // Order is a toggle that works with any mode - when ON, guards listen to these commands
        String lowerMessage = message.toLowerCase();
        if (lowerMessage.equals("gstop") || lowerMessage.equals("gattack")) {
            // Run everything on main thread since AsyncPlayerChatEvent is async
            final boolean isStop = lowerMessage.equals("gstop");
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
                boolean hasOrderEnabled = false;

                for (BodyGuard guard : guards) {
                    // Check if this guard has Order toggle enabled (works with any mode)
                    if (guard.isOrderActive()) {
                        hasOrderEnabled = true;
                        if (isStop) {
                            // Stop attacking - set flag and clear target
                            guard.setOrderStopped(true);
                            IronGolem entity = guard.getEntity();
                            if (entity != null) {
                                entity.setTarget(null);
                            }
                        } else { // gattack
                            // Resume attacking - clear the stop flag
                            guard.setOrderStopped(false);
                        }
                    }
                }

                if (hasOrderEnabled) {
                    String msg = isStop
                            ? plugin.getConfig().getString("messages.order-stopped", "&eGuard stopped attacking.")
                            : plugin.getConfig().getString("messages.order-attack", "&aGuard resuming attack.");
                    MessageUtil.send(player, msg);
                }
            });
            event.setCancelled(true); // Cancel chat so command doesn't show
            return;
        }

        // Check for custom days input
        if (guiManager.isAwaitingCustomDays(player.getUniqueId())) {
            event.setCancelled(true);

            // Cancel input
            if (message.equals("-")) {
                guiManager.setAwaitingCustomDays(player.getUniqueId(), false);
                Bukkit.getScheduler().runTask(plugin, () -> guiManager.openCustomDaysGUI(player));
                return;
            }

            try {
                int days = Integer.parseInt(message);
                int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
                int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);

                if (days < minDays || days > maxDays) {
                    MessageUtil.send(player, plugin.getConfig().getString("messages.invalid-days")
                            .replace("%min%", String.valueOf(minDays))
                            .replace("%max%", String.valueOf(maxDays)));
                    return;
                }

                guiManager.setAwaitingCustomDays(player.getUniqueId(), false);
                Bukkit.getScheduler().runTask(plugin, () -> guiManager.openCustomDaysConfirmGUI(player, days));

            } catch (NumberFormatException e) {
                int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
                int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);
                MessageUtil.send(player, plugin.getConfig().getString("messages.invalid-days")
                        .replace("%min%", String.valueOf(minDays))
                        .replace("%max%", String.valueOf(maxDays)));
            }
            return;
        }

        // Check for custom attack input
        BodyGuard awaitingAttack = guiManager.getAwaitingCustomAttack(player.getUniqueId());
        if (awaitingAttack != null) {
            event.setCancelled(true);

            // Cancel input
            if (message.equals("-")) {
                guiManager.setAwaitingCustomAttack(player.getUniqueId(), null);
                Bukkit.getScheduler().runTask(plugin, () -> guiManager.openGuardControlGUI(player, awaitingAttack));
                return;
            }

            // Find player
            Player target = Bukkit.getPlayerExact(message);
            if (target == null) {
                MessageUtil.send(player, plugin.getConfig().getString("messages.player-not-found"));
                return;
            }

            // Prevent targeting self
            if (target.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getConfig().getString("messages.cannot-target-self", "&cYou cannot target yourself!"));
                return;
            }

            awaitingAttack.setCustomTargetName(target.getName());
            awaitingAttack.setMode(GuardMode.CUSTOM_ATTACK);
            guiManager.setAwaitingCustomAttack(player.getUniqueId(), null);

            MessageUtil.send(player, plugin.getConfig().getString("messages.custom-attack-set")
                    .replace("%player%", target.getName()));

            Bukkit.getScheduler().runTask(plugin, () -> guiManager.openGuardControlGUI(player, awaitingAttack));
        }
    }
}
