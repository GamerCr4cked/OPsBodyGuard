package com.opsbodyguard.listeners;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.guard.GuardMode;
import com.opsbodyguard.guard.PaymentType;
import com.opsbodyguard.gui.GUIManager;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GUIListener implements Listener {

    private final OPsBodyGuard plugin;

    public GUIListener(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    private GUIManager getGUIManager() {
        return plugin.getGUIManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GUIManager guiManager = getGUIManager();
        GUIManager.GUIType guiType = guiManager.getOpenGUI(player.getUniqueId());
        if (guiType == null) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        int slot = event.getRawSlot();

        // Debug (uncomment to enable)
        // MessageUtil.logDebug("GUI Click - Type: " + guiType + ", Slot: " + slot + ", Item: " + clicked.getType());

        switch (guiType) {
            case MAIN -> handleMainGUI(player, slot);
            case PAYMENT_TYPE -> handlePaymentTypeGUI(player, slot);
            case PER_DAY_CONFIRM -> handlePerDayConfirmGUI(player, slot);
            case CUSTOM_DAYS -> handleCustomDaysGUI(player, slot);
            case CUSTOM_CONFIRM -> handleCustomConfirmGUI(player, slot);
            case GUARD_CONTROL -> handleGuardControlGUI(player, slot);
            case ALL_GUARDS_CONTROL -> handleAllGuardsControlGUI(player, slot);
            case CANCEL_CONFIRM -> handleCancelConfirmGUI(player, slot);
        }
    }

    private void handleMainGUI(Player player, int slot) {
        if (slot == 13) {
            // Check if player can have more guards
            if (!plugin.getGuardManager().canHaveMoreGuards(player.getUniqueId())) {
                MessageUtil.send(player, plugin.getConfig().getString("messages.max-guards"));
                player.closeInventory();
                return;
            }
            getGUIManager().openPaymentTypeGUI(player);
        }
    }

    private void handlePaymentTypeGUI(Player player, int slot) {
        switch (slot) {
            case 11 -> getGUIManager().openPerDayConfirmGUI(player);
            case 15 -> getGUIManager().openCustomDaysGUI(player);
            case 22 -> getGUIManager().openMainGUI(player);
        }
    }

    private void handlePerDayConfirmGUI(Player player, int slot) {
        GUIManager guiManager = getGUIManager();
        switch (slot) {
            case 11 -> { // Confirm
                GUIManager.PendingPurchase purchase = guiManager.getPendingPurchase(player.getUniqueId());
                if (purchase == null) {
                    player.closeInventory();
                    return;
                }

                // Check money
                if (!plugin.getEconomyManager().hasEnough(player, purchase.getPrice())) {
                    MessageUtil.send(player, plugin.getConfig().getString("messages.no-money"));
                    player.closeInventory();
                    return;
                }

                // Withdraw and spawn
                plugin.getEconomyManager().withdraw(player, purchase.getPrice());

                // Send payment message first
                String priceFormatted = MessageUtil.formatMoneyClean(purchase.getPrice());
                MessageUtil.send(player, plugin.getConfig().getString("messages.guard-purchased", "&aPurchase successful! &f$%price% &ahas been deducted.")
                        .replace("%price%", priceFormatted));

                plugin.getGuardManager().spawnGuard(player, PaymentType.PER_DAY, 0);
                MessageUtil.send(player, plugin.getConfig().getString("messages.guard-arrived"));
                guiManager.removePendingPurchase(player.getUniqueId());
                player.closeInventory();
            }
            case 15 -> { // Cancel
                guiManager.removePendingPurchase(player.getUniqueId());
                guiManager.openPaymentTypeGUI(player);
            }
        }
    }

    private void handleCustomDaysGUI(Player player, int slot) {
        GUIManager guiManager = getGUIManager();
        switch (slot) {
            case 10 -> { // 5 days
                if (plugin.getConfig().getBoolean("day-packages.5.enabled", true)) {
                    guiManager.openCustomDaysConfirmGUI(player, 5);
                }
            }
            case 12 -> { // 10 days
                if (plugin.getConfig().getBoolean("day-packages.10.enabled", true)) {
                    guiManager.openCustomDaysConfirmGUI(player, 10);
                }
            }
            case 14 -> { // 20 days
                if (plugin.getConfig().getBoolean("day-packages.20.enabled", true)) {
                    guiManager.openCustomDaysConfirmGUI(player, 20);
                }
            }
            case 16 -> { // 30 days
                if (plugin.getConfig().getBoolean("day-packages.30.enabled", true)) {
                    guiManager.openCustomDaysConfirmGUI(player, 30);
                }
            }
            case 22 -> { // Custom days
                player.closeInventory();
                guiManager.setAwaitingCustomDays(player.getUniqueId(), true);
                int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
                int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);
                MessageUtil.send(player, plugin.getConfig().getString("messages.enter-days")
                        .replace("%min%", String.valueOf(minDays))
                        .replace("%max%", String.valueOf(maxDays)));
            }
            case 31 -> guiManager.openPaymentTypeGUI(player);
        }
    }

    private void handleCustomConfirmGUI(Player player, int slot) {
        GUIManager guiManager = getGUIManager();
        switch (slot) {
            case 11 -> { // Confirm
                GUIManager.PendingPurchase purchase = guiManager.getPendingPurchase(player.getUniqueId());
                if (purchase == null) {
                    player.closeInventory();
                    return;
                }

                // Check money
                if (!plugin.getEconomyManager().hasEnough(player, purchase.getPrice())) {
                    MessageUtil.send(player, plugin.getConfig().getString("messages.no-money"));
                    player.closeInventory();
                    return;
                }

                // Withdraw and spawn
                plugin.getEconomyManager().withdraw(player, purchase.getPrice());

                // Send payment message first
                String priceFormatted = MessageUtil.formatMoneyClean(purchase.getPrice());
                MessageUtil.send(player, plugin.getConfig().getString("messages.guard-purchased", "&aPurchase successful! &f$%price% &ahas been deducted.")
                        .replace("%price%", priceFormatted));

                plugin.getGuardManager().spawnGuard(player, PaymentType.CUSTOM_DAYS, purchase.getDays());
                MessageUtil.send(player, plugin.getConfig().getString("messages.guard-arrived"));
                guiManager.removePendingPurchase(player.getUniqueId());
                player.closeInventory();
            }
            case 15 -> { // Cancel
                guiManager.removePendingPurchase(player.getUniqueId());
                guiManager.openCustomDaysGUI(player);
            }
        }
    }

    private void handleGuardControlGUI(Player player, int slot) {
        GUIManager guiManager = getGUIManager();

        // Get the guard that was right-clicked (not always the first one)
        BodyGuard guard = guiManager.getControllingGuard(player.getUniqueId());
        if (guard == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> { // Aggressive
                guard.setMode(GuardMode.AGGRESSIVE);
                guard.setCustomTargetName(null); // Clear custom target
                clearGuardTarget(guard); // Clear current target
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Aggressive"));
                guiManager.openGuardControlGUI(player, guard);
            }
            case 11 -> { // Neutral (renamed from Order)
                guard.setMode(GuardMode.NEUTRAL);
                guard.setCustomTargetName(null); // Clear custom target
                clearGuardTarget(guard); // Clear current target
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Neutral"));
                guiManager.openGuardControlGUI(player, guard);
            }
            case 12 -> { // Hostile (moved from 14)
                guard.setMode(GuardMode.HOSTILE);
                guard.setCustomTargetName(null); // Clear custom target
                clearGuardTarget(guard); // Clear current target
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Hostile"));
                guiManager.openGuardControlGUI(player, guard);
            }
            case 14 -> { // Passive (moved from 12)
                guard.setMode(GuardMode.PASSIVE);
                guard.setCustomTargetName(null); // Clear custom target
                clearGuardTarget(guard); // Clear current target
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Passive"));
                guiManager.openGuardControlGUI(player, guard);
            }
            case 15 -> { // Custom Attack (moved from 16)
                player.closeInventory();
                guiManager.setAwaitingCustomAttack(player.getUniqueId(), guard);
                MessageUtil.send(player, plugin.getConfig().getString("messages.custom-attack-enter"));
            }
            case 16 -> { // Order toggle (works with any mode)
                guard.setOrderActive(!guard.isOrderActive());
                if (!guard.isOrderActive()) {
                    guard.setOrderStopped(false); // Reset stop state when turning off
                }
                String status = guard.isOrderActive() ? "ON" : "OFF";
                MessageUtil.send(player, plugin.getConfig().getString("messages.order-toggled", "&eOrder toggle: &f%status%")
                        .replace("%status%", status));
                guiManager.openGuardControlGUI(player, guard);
            }
            case 20 -> { // Cancel Guard
                guiManager.openCancelConfirmGUI(player, false);
            }
            case 22 -> player.closeInventory();
        }
    }

    private void handleAllGuardsControlGUI(Player player, int slot) {
        List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
        if (guards.isEmpty()) {
            player.closeInventory();
            return;
        }

        GUIManager guiManager = getGUIManager();

        switch (slot) {
            case 10 -> { // Aggressive - set all guards
                for (BodyGuard guard : guards) {
                    guard.setMode(GuardMode.AGGRESSIVE);
                    guard.setCustomTargetName(null); // Clear custom target
                    clearGuardTarget(guard); // Clear current target
                }
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Aggressive") + " &7(All Guards)");
                guiManager.openAllGuardsControlGUI(player);
            }
            case 11 -> { // Neutral - set all guards
                for (BodyGuard guard : guards) {
                    guard.setMode(GuardMode.NEUTRAL);
                    guard.setCustomTargetName(null); // Clear custom target
                    clearGuardTarget(guard); // Clear current target
                }
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Neutral") + " &7(All Guards)");
                guiManager.openAllGuardsControlGUI(player);
            }
            case 12 -> { // Hostile - set all guards
                for (BodyGuard guard : guards) {
                    guard.setMode(GuardMode.HOSTILE);
                    guard.setCustomTargetName(null); // Clear custom target
                    clearGuardTarget(guard); // Clear current target
                }
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Hostile") + " &7(All Guards)");
                guiManager.openAllGuardsControlGUI(player);
            }
            case 14 -> { // Passive - set all guards
                for (BodyGuard guard : guards) {
                    guard.setMode(GuardMode.PASSIVE);
                    guard.setCustomTargetName(null); // Clear custom target
                    clearGuardTarget(guard); // Clear current target
                }
                MessageUtil.send(player, plugin.getConfig().getString("messages.mode-changed")
                        .replace("%mode%", "Passive") + " &7(All Guards)");
                guiManager.openAllGuardsControlGUI(player);
            }
            case 15 -> { // Custom Attack - not available for all guards
                MessageUtil.send(player, "&cCustom Attack is not available for all guards!");
            }
            case 16 -> { // Order toggle - toggle all guards
                boolean newState = !guards.get(0).isOrderActive(); // Toggle based on first guard
                for (BodyGuard guard : guards) {
                    guard.setOrderActive(newState);
                    if (!newState) {
                        guard.setOrderStopped(false);
                    }
                }
                String status = newState ? "ON" : "OFF";
                MessageUtil.send(player, plugin.getConfig().getString("messages.order-toggled", "&eOrder toggle: &f%status%")
                        .replace("%status%", status) + " &7(All Guards)");
                guiManager.openAllGuardsControlGUI(player);
            }
            case 20 -> { // Cancel ALL Guards
                guiManager.openCancelConfirmGUI(player, true);
            }
            case 22 -> player.closeInventory();
        }
    }

    private void handleCancelConfirmGUI(Player player, int slot) {
        GUIManager guiManager = getGUIManager();
        boolean allGuards = guiManager.isAllGuardsMode(player.getUniqueId());

        switch (slot) {
            case 11 -> { // Confirm cancel
                if (allGuards) {
                    // Cancel ALL guards
                    List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
                    for (BodyGuard guard : new java.util.ArrayList<>(guards)) {
                        plugin.getGuardManager().removeGuard(guard);
                    }
                    MessageUtil.send(player, plugin.getConfig().getString("messages.all-guards-cancelled", "&eAll guards have been cancelled."));
                } else {
                    // Cancel single guard
                    BodyGuard guard = guiManager.getControllingGuard(player.getUniqueId());
                    if (guard != null) {
                        plugin.getGuardManager().removeGuard(guard);
                        MessageUtil.send(player, plugin.getConfig().getString("messages.guard-cancelled"));
                    }
                }
                guiManager.setControllingGuard(player.getUniqueId(), null);
                guiManager.setAllGuardsMode(player.getUniqueId(), false);
                player.closeInventory();
            }
            case 15 -> { // Go back
                if (allGuards) {
                    guiManager.openAllGuardsControlGUI(player);
                } else {
                    BodyGuard guard = guiManager.getControllingGuard(player.getUniqueId());
                    if (guard != null) {
                        guiManager.openGuardControlGUI(player, guard);
                    } else {
                        player.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // Check if this was one of our GUIs
        GUIManager guiManager = getGUIManager();
        if (guiManager.getOpenGUI(player.getUniqueId()) == null) return;

        // Delay removal to allow GUI transitions (2 ticks to be safe)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Only remove if player doesn't have a plugin GUI open anymore
            // Check if the current open inventory title matches our GUI titles
            String topTitle = player.getOpenInventory().getTitle();
            if (topTitle == null || !isPluginGUI(topTitle)) {
                guiManager.removeOpenGUI(player.getUniqueId());
            }
        }, 2L);
    }

    private boolean isPluginGUI(String title) {
        // Check if title matches any of our GUI titles
        String mainTitle = MessageUtil.color(plugin.getConfig().getString("gui.main-title", "&8&lBodyGuard Shop"));
        String paymentTitle = MessageUtil.color(plugin.getConfig().getString("gui.payment-type-title", "&8&lSelect Payment Type"));
        String perDayTitle = MessageUtil.color(plugin.getConfig().getString("gui.per-day-confirm-title", "&8&lConfirm Per-Day Payment"));
        String customDaysTitle = MessageUtil.color(plugin.getConfig().getString("gui.custom-days-title", "&8&lSelect Days"));
        String customConfirmTitle = MessageUtil.color(plugin.getConfig().getString("gui.custom-confirm-title", "&8&lConfirm Payment"));
        String guardControlTitle = MessageUtil.color(plugin.getConfig().getString("gui.guard-control-title", "&8&lGuard Control"));
        String allGuardsTitle = MessageUtil.color(plugin.getConfig().getString("gui.all-guards-title", "&8&lAll Guards Control"));
        String cancelConfirmTitle = MessageUtil.color("&8&lCancel Guard?");
        String cancelAllTitle = MessageUtil.color("&8&lCancel ALL Guards?");

        return title.equals(mainTitle) || title.equals(paymentTitle) || title.equals(perDayTitle) ||
               title.equals(customDaysTitle) || title.equals(customConfirmTitle) || title.equals(guardControlTitle) ||
               title.equals(allGuardsTitle) || title.equals(cancelConfirmTitle) || title.equals(cancelAllTitle);
    }

    private void clearGuardTarget(BodyGuard guard) {
        org.bukkit.entity.IronGolem entity = guard.getEntity();
        if (entity != null) {
            entity.setTarget(null);
        }
    }
}
