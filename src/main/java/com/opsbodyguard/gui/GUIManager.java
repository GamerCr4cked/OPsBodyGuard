package com.opsbodyguard.gui;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIManager {

    private final OPsBodyGuard plugin;

    // Track what GUI each player has open
    private final Map<UUID, GUIType> openGUIs = new HashMap<>();

    // Track pending purchases
    private final Map<UUID, PendingPurchase> pendingPurchases = new HashMap<>();

    // Track cancel confirmations
    private final Map<UUID, Long> cancelConfirmations = new HashMap<>();

    // Track custom day input
    private final Map<UUID, Boolean> awaitingCustomDays = new HashMap<>();

    // Track custom attack input
    private final Map<UUID, BodyGuard> awaitingCustomAttack = new HashMap<>();

    // Track which guard is being controlled (for single guard GUI)
    private final Map<UUID, BodyGuard> controllingGuard = new HashMap<>();

    // Track if in "All Guards" mode
    private final Map<UUID, Boolean> allGuardsMode = new HashMap<>();

    public GUIManager(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    public void openMainGUI(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.main-title", "&8&lBodyGuard Shop"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Guard icon in middle (slot 13)
        Material guardIcon = Material.valueOf(plugin.getConfig().getString("gui-items.guard-icon", "IRON_GOLEM_SPAWN_EGG"));
        String priceFormatted = MessageUtil.formatMoneyClean(plugin.getConfig().getDouble("price.per-day", 50000));
        ItemStack guardItem = createItem(guardIcon, "&b&lBodyGuard",
                "&7Click to purchase a BodyGuard!",
                "&7",
                "&ePer day: &f$" + priceFormatted);
        inv.setItem(13, guardItem);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.MAIN);
    }

    public void openPaymentTypeGUI(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.payment-type-title", "&8&lSelect Payment Type"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Per-Day Payment (slot 11)
        Material perDayIcon = Material.valueOf(plugin.getConfig().getString("gui-items.per-day-icon", "CLOCK"));
        String perDayPrice = MessageUtil.formatMoneyClean(plugin.getConfig().getDouble("price.per-day", 50000));
        ItemStack perDayItem = createItem(perDayIcon, "&b&lPer-Day Payment",
                "&7Pay daily for your guard",
                "&7Auto-deducts each MC day",
                "&7",
                "&ePrice: &f$" + perDayPrice + " &7per day",
                "&7",
                "&eClick to select!");
        inv.setItem(11, perDayItem);

        // Custom Days Payment (slot 15)
        Material customIcon = Material.valueOf(plugin.getConfig().getString("gui-items.custom-day-icon", "WRITABLE_BOOK"));
        int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
        int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);
        ItemStack customItem = createItem(customIcon, "&b&lCustom Days Payment",
                "&7Pay upfront for multiple days",
                "&7(" + minDays + "-" + maxDays + " days)",
                "&7",
                "&eDiscounts available!",
                "&7",
                "&eClick to select!");
        inv.setItem(15, customItem);

        // Back button (slot 22)
        Material backIcon = Material.valueOf(plugin.getConfig().getString("gui-items.back-icon", "ARROW"));
        ItemStack backItem = createItem(backIcon, "&cBack");
        inv.setItem(22, backItem);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.PAYMENT_TYPE);
    }

    public void openPerDayConfirmGUI(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.per-day-confirm-title", "&8&lConfirm Per-Day Payment"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        double price = plugin.getConfig().getDouble("price.per-day", 50000);
        String priceFormatted = MessageUtil.formatMoneyClean(price);

        // Info item (slot 13)
        ItemStack infoItem = createItem(Material.PAPER, "&b&lBodyGuard",
                "&7Per-Day Payment",
                "&7",
                "&ePrice: &f$" + priceFormatted + " &7per MC day",
                "&7",
                "&7Your balance will be charged",
                "&7automatically each Minecraft day.");
        inv.setItem(13, infoItem);

        // Confirm button (slot 11)
        Material confirmIcon = Material.valueOf(plugin.getConfig().getString("gui-items.confirm-icon", "LIME_WOOL"));
        ItemStack confirmItem = createItem(confirmIcon, "&a&lConfirm");
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        Material cancelIcon = Material.valueOf(plugin.getConfig().getString("gui-items.cancel-icon", "RED_WOOL"));
        ItemStack cancelItem = createItem(cancelIcon, "&c&lCancel");
        inv.setItem(15, cancelItem);

        // Store pending purchase
        pendingPurchases.put(player.getUniqueId(), new PendingPurchase(true, 0, price));

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.PER_DAY_CONFIRM);
    }

    public void openCustomDaysGUI(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.custom-days-title", "&8&lSelect Days"));
        Inventory inv = Bukkit.createInventory(null, 36, title);

        // 5 Days (slot 10)
        if (plugin.getConfig().getBoolean("day-packages.5.enabled", true)) {
            double discount = plugin.getEconomyManager().getDiscountForDays(5);
            double price = plugin.getEconomyManager().getPriceForDays(5);
            ItemStack item = createItem(Material.LIGHT_BLUE_WOOL, "&b&l5 Days",
                    "&7",
                    "&ePrice: &f$" + MessageUtil.formatMoneyClean(price),
                    discount > 0 ? "&a" + String.format("%.1f", discount) + "% Discount!" : "&7No discount",
                    "&7",
                    "&eClick to select!");
            inv.setItem(10, item);
        }

        // 10 Days (slot 12)
        if (plugin.getConfig().getBoolean("day-packages.10.enabled", true)) {
            double discount = plugin.getEconomyManager().getDiscountForDays(10);
            double price = plugin.getEconomyManager().getPriceForDays(10);
            ItemStack item = createItem(Material.LIME_WOOL, "&a&l10 Days",
                    "&7",
                    "&ePrice: &f$" + MessageUtil.formatMoneyClean(price),
                    discount > 0 ? "&a" + String.format("%.1f", discount) + "% Discount!" : "&7No discount",
                    "&7",
                    "&eClick to select!");
            inv.setItem(12, item);
        }

        // 20 Days (slot 14)
        if (plugin.getConfig().getBoolean("day-packages.20.enabled", true)) {
            double discount = plugin.getEconomyManager().getDiscountForDays(20);
            double price = plugin.getEconomyManager().getPriceForDays(20);
            ItemStack item = createItem(Material.YELLOW_WOOL, "&e&l20 Days",
                    "&7",
                    "&ePrice: &f$" + MessageUtil.formatMoneyClean(price),
                    discount > 0 ? "&a" + String.format("%.1f", discount) + "% Discount!" : "&7No discount",
                    "&7",
                    "&eClick to select!");
            inv.setItem(14, item);
        }

        // 30 Days (slot 16)
        if (plugin.getConfig().getBoolean("day-packages.30.enabled", true)) {
            double discount = plugin.getEconomyManager().getDiscountForDays(30);
            double price = plugin.getEconomyManager().getPriceForDays(30);
            ItemStack item = createItem(Material.GOLD_BLOCK, "&6&l30 Days",
                    "&7",
                    "&ePrice: &f$" + MessageUtil.formatMoneyClean(price),
                    discount > 0 ? "&a" + String.format("%.1f", discount) + "% Discount!" : "&7No discount",
                    "&7",
                    "&eClick to select!");
            inv.setItem(16, item);
        }

        // Custom Days (slot 22)
        int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
        int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);
        ItemStack customItem = createItem(Material.BOOK, "&b&lCustom Days",
                "&7",
                "&7Enter any number of days",
                "&7(" + minDays + "-" + maxDays + " days)",
                "&7",
                "&eClick to enter custom days!");
        inv.setItem(22, customItem);

        // Back button (slot 31)
        Material backIcon = Material.valueOf(plugin.getConfig().getString("gui-items.back-icon", "ARROW"));
        ItemStack backItem = createItem(backIcon, "&cBack");
        inv.setItem(31, backItem);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.CUSTOM_DAYS);
    }

    public void openCustomDaysConfirmGUI(Player player, int days) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.custom-confirm-title", "&8&lConfirm Payment"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        double price = plugin.getEconomyManager().getPriceForDays(days);
        double discount = plugin.getEconomyManager().getDiscountForDays(days);

        // Info item (slot 13)
        ItemStack infoItem = createItem(Material.PAPER, "&b&lBodyGuard",
                "&7Custom Days Payment",
                "&7",
                "&eDays: &f" + days + " MC days",
                "&eTotal Price: &f$" + MessageUtil.formatMoneyClean(price),
                discount > 0 ? "&aDiscount: " + String.format("%.1f", discount) + "%" : "",
                "&7",
                "&7Pay once for " + days + " MC days!");
        inv.setItem(13, infoItem);

        // Confirm button (slot 11)
        Material confirmIcon = Material.valueOf(plugin.getConfig().getString("gui-items.confirm-icon", "LIME_WOOL"));
        ItemStack confirmItem = createItem(confirmIcon, "&a&lConfirm");
        inv.setItem(11, confirmItem);

        // Cancel button (slot 15)
        Material cancelIcon = Material.valueOf(plugin.getConfig().getString("gui-items.cancel-icon", "RED_WOOL"));
        ItemStack cancelItem = createItem(cancelIcon, "&c&lCancel");
        inv.setItem(15, cancelItem);

        // Store pending purchase
        pendingPurchases.put(player.getUniqueId(), new PendingPurchase(false, days, price));

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.CUSTOM_CONFIRM);
    }

    public void openGuardControlGUI(Player player, BodyGuard guard) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.guard-control-title", "&8&lGuard Control"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Current mode indicator (slot 4)
        ItemStack currentMode = createItem(Material.NETHER_STAR, "&b&lCurrent Mode",
                "&7" + guard.getMode().getDisplayName(),
                "&7" + guard.getMode().getDescription());
        inv.setItem(4, currentMode);

        // Aggressive mode (slot 10)
        Material aggIcon = Material.valueOf(plugin.getConfig().getString("gui-items.aggressive-icon", "DIAMOND_SWORD"));
        ItemStack aggItem = createItem(aggIcon, "&c&lAggressive",
                "&7Attack anyone nearby",
                "&7",
                guard.getMode().name().equals("AGGRESSIVE") ? "&a> Selected" : "&eClick to select");
        inv.setItem(10, aggItem);

        // Neutral mode (slot 11) - renamed from Order
        Material neutralIcon = Material.valueOf(plugin.getConfig().getString("gui-items.neutral-icon", "LEAD"));
        ItemStack neutralItem = createItem(neutralIcon, "&6&lNeutral",
                "&7Hit entity to attack",
                "&7",
                guard.getMode().name().equals("NEUTRAL") ? "&a> Selected" : "&eClick to select");
        inv.setItem(11, neutralItem);

        // Hostile mode (slot 12) - moved from 14
        Material hostileIcon = Material.valueOf(plugin.getConfig().getString("gui-items.hostile-icon", "ROTTEN_FLESH"));
        ItemStack hostileItem = createItem(hostileIcon, "&5&lHostile",
                "&7Only attack hostile mobs",
                "&7",
                guard.getMode().name().equals("HOSTILE") ? "&a> Selected" : "&eClick to select");
        inv.setItem(12, hostileItem);

        // Passive mode (slot 14) - moved from 12
        Material passiveIcon = Material.valueOf(plugin.getConfig().getString("gui-items.passive-icon", "WHEAT"));
        ItemStack passiveItem = createItem(passiveIcon, "&a&lPassive",
                "&7Won't attack anyone",
                "&7",
                guard.getMode().name().equals("PASSIVE") ? "&a> Selected" : "&eClick to select");
        inv.setItem(14, passiveItem);

        // Custom Attack mode (slot 15) - moved from 16
        Material customIcon = Material.valueOf(plugin.getConfig().getString("gui-items.custom-attack-icon", "PLAYER_HEAD"));
        String targetName = guard.getCustomTargetName() != null ? guard.getCustomTargetName() : "None";
        ItemStack customItem = createItem(customIcon, "&d&lCustom Attack",
                "&7Attack specific player",
                "&7Current target: &e" + targetName,
                "&7",
                guard.getMode().name().equals("CUSTOM_ATTACK") ? "&a> Selected" : "&eClick to select");
        inv.setItem(15, customItem);

        // Order toggle (slot 16) - works with any mode
        Material orderIcon = Material.valueOf(plugin.getConfig().getString("gui-items.order-icon", "COMPARATOR"));
        String orderStatus = guard.isOrderActive() ? "&aON" : "&cOFF";
        String stoppedStatus = guard.isOrderStopped() ? " &7(Stopped)" : "";
        ItemStack orderItem = createItem(orderIcon, "&b&lOrder Toggle",
                "&7Listen to chat commands",
                "&7Use &egstop&7/&egattack&7 in chat",
                "&7",
                "&7Status: " + orderStatus + stoppedStatus,
                "&7",
                "&eClick to toggle ON/OFF");
        inv.setItem(16, orderItem);

        // Cancel Guard button (slot 20)
        ItemStack cancelGuardItem = createItem(Material.BARRIER, "&c&lCancel Guard",
                "&7Remove this guard permanently",
                "&7",
                "&cNo refund will be given!");
        inv.setItem(20, cancelGuardItem);

        // Close button (slot 22)
        Material closeIcon = Material.valueOf(plugin.getConfig().getString("gui-items.cancel-icon", "RED_WOOL"));
        ItemStack closeItem = createItem(closeIcon, "&c&lClose");
        inv.setItem(22, closeItem);

        // Track which guard is being controlled
        controllingGuard.put(player.getUniqueId(), guard);
        allGuardsMode.put(player.getUniqueId(), false);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.GUARD_CONTROL);
    }

    // All Guards GUI - opens on sneak+right-click
    public void openAllGuardsControlGUI(Player player) {
        String title = MessageUtil.color(plugin.getConfig().getString("gui.all-guards-title", "&8&lAll Guards Control"));
        Inventory inv = Bukkit.createInventory(null, 27, title);

        java.util.List<com.opsbodyguard.guard.BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
        if (guards.isEmpty()) return;

        // Use first guard for mode display (all guards will have same mode when changed)
        com.opsbodyguard.guard.BodyGuard firstGuard = guards.get(0);

        // Current mode indicator (slot 4)
        ItemStack currentMode = createItem(Material.NETHER_STAR, "&6&lAll Guards Mode",
                "&7Mode: " + firstGuard.getMode().getDisplayName(),
                "&7Total Guards: &e" + guards.size());
        inv.setItem(4, currentMode);

        // Aggressive mode (slot 10)
        Material aggIcon = Material.valueOf(plugin.getConfig().getString("gui-items.aggressive-icon", "DIAMOND_SWORD"));
        ItemStack aggItem = createItem(aggIcon, "&c&lAggressive",
                "&7Attack anyone nearby",
                "&7",
                "&eClick to set ALL guards");
        inv.setItem(10, aggItem);

        // Neutral mode (slot 11)
        Material neutralIcon = Material.valueOf(plugin.getConfig().getString("gui-items.neutral-icon", "LEAD"));
        ItemStack neutralItem = createItem(neutralIcon, "&6&lNeutral",
                "&7Hit entity to attack",
                "&7",
                "&eClick to set ALL guards");
        inv.setItem(11, neutralItem);

        // Hostile mode (slot 12)
        Material hostileIcon = Material.valueOf(plugin.getConfig().getString("gui-items.hostile-icon", "ROTTEN_FLESH"));
        ItemStack hostileItem = createItem(hostileIcon, "&5&lHostile",
                "&7Only attack hostile mobs",
                "&7",
                "&eClick to set ALL guards");
        inv.setItem(12, hostileItem);

        // Passive mode (slot 14)
        Material passiveIcon = Material.valueOf(plugin.getConfig().getString("gui-items.passive-icon", "WHEAT"));
        ItemStack passiveItem = createItem(passiveIcon, "&a&lPassive",
                "&7Won't attack anyone",
                "&7",
                "&eClick to set ALL guards");
        inv.setItem(14, passiveItem);

        // Custom Attack mode (slot 15) - disabled for all guards
        ItemStack customItem = createItem(Material.PLAYER_HEAD, "&d&lCustom Attack",
                "&7Attack specific player",
                "&7",
                "&cNot available for all guards");
        inv.setItem(15, customItem);

        // Order toggle (slot 16)
        Material orderIcon = Material.valueOf(plugin.getConfig().getString("gui-items.order-icon", "COMPARATOR"));
        ItemStack orderItem = createItem(orderIcon, "&b&lOrder Toggle",
                "&7Listen to chat commands",
                "&7Use &egstop&7/&egattack&7 in chat",
                "&7",
                "&eClick to toggle ALL guards");
        inv.setItem(16, orderItem);

        // Cancel ALL Guards button (slot 20)
        ItemStack cancelAllItem = createItem(Material.BARRIER, "&c&lCancel ALL Guards",
                "&7Remove ALL guards permanently",
                "&7",
                "&cNo refund will be given!");
        inv.setItem(20, cancelAllItem);

        // Close button (slot 22)
        Material closeIcon = Material.valueOf(plugin.getConfig().getString("gui-items.cancel-icon", "RED_WOOL"));
        ItemStack closeItem = createItem(closeIcon, "&c&lClose");
        inv.setItem(22, closeItem);

        // Track all guards mode
        allGuardsMode.put(player.getUniqueId(), true);
        controllingGuard.remove(player.getUniqueId());

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.ALL_GUARDS_CONTROL);
    }

    // Cancel confirmation GUI
    public void openCancelConfirmGUI(Player player, boolean allGuards) {
        String title = MessageUtil.color(allGuards ? "&8&lCancel ALL Guards?" : "&8&lCancel Guard?");
        Inventory inv = Bukkit.createInventory(null, 27, title);

        // Warning info (slot 4)
        ItemStack infoItem = createItem(Material.PAPER, "&c&lWarning!",
                allGuards ? "&7You are about to cancel ALL guards" : "&7You are about to cancel this guard",
                "&7",
                "&cNo refund will be given!",
                "&7",
                "&7This action cannot be undone.");
        inv.setItem(4, infoItem);

        // Confirm button (slot 11)
        Material confirmIcon = Material.valueOf(plugin.getConfig().getString("gui-items.confirm-icon", "LIME_WOOL"));
        ItemStack confirmItem = createItem(confirmIcon, "&a&lConfirm Cancel",
                "&7Click to confirm cancellation");
        inv.setItem(11, confirmItem);

        // Go back button (slot 15)
        Material cancelIcon = Material.valueOf(plugin.getConfig().getString("gui-items.cancel-icon", "RED_WOOL"));
        ItemStack cancelItem = createItem(cancelIcon, "&c&lGo Back",
                "&7Don't cancel");
        inv.setItem(15, cancelItem);

        player.openInventory(inv);
        openGUIs.put(player.getUniqueId(), GUIType.CANCEL_CONFIRM);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageUtil.color(name));
        if (lore.length > 0) {
            meta.setLore(Arrays.stream(lore).map(MessageUtil::color).toList());
        }
        item.setItemMeta(meta);
        return item;
    }

    // Getters and utility methods
    public GUIType getOpenGUI(UUID playerUUID) {
        return openGUIs.get(playerUUID);
    }

    public void removeOpenGUI(UUID playerUUID) {
        openGUIs.remove(playerUUID);
    }

    public PendingPurchase getPendingPurchase(UUID playerUUID) {
        return pendingPurchases.get(playerUUID);
    }

    public void removePendingPurchase(UUID playerUUID) {
        pendingPurchases.remove(playerUUID);
    }

    public boolean hasCancelConfirmation(UUID playerUUID) {
        Long time = cancelConfirmations.get(playerUUID);
        if (time == null) return false;
        // 30 second window
        return System.currentTimeMillis() - time < 30000;
    }

    public void setCancelConfirmation(UUID playerUUID) {
        cancelConfirmations.put(playerUUID, System.currentTimeMillis());
    }

    public void removeCancelConfirmation(UUID playerUUID) {
        cancelConfirmations.remove(playerUUID);
    }

    public boolean isAwaitingCustomDays(UUID playerUUID) {
        return awaitingCustomDays.getOrDefault(playerUUID, false);
    }

    public void setAwaitingCustomDays(UUID playerUUID, boolean awaiting) {
        if (awaiting) {
            awaitingCustomDays.put(playerUUID, true);
        } else {
            awaitingCustomDays.remove(playerUUID);
        }
    }

    public BodyGuard getAwaitingCustomAttack(UUID playerUUID) {
        return awaitingCustomAttack.get(playerUUID);
    }

    public void setAwaitingCustomAttack(UUID playerUUID, BodyGuard guard) {
        if (guard != null) {
            awaitingCustomAttack.put(playerUUID, guard);
        } else {
            awaitingCustomAttack.remove(playerUUID);
        }
    }

    // Inner class for pending purchases
    public static class PendingPurchase {
        private final boolean perDay;
        private final int days;
        private final double price;

        public PendingPurchase(boolean perDay, int days, double price) {
            this.perDay = perDay;
            this.days = days;
            this.price = price;
        }

        public boolean isPerDay() {
            return perDay;
        }

        public int getDays() {
            return days;
        }

        public double getPrice() {
            return price;
        }
    }

    public enum GUIType {
        MAIN,
        PAYMENT_TYPE,
        PER_DAY_CONFIRM,
        CUSTOM_DAYS,
        CUSTOM_CONFIRM,
        GUARD_CONTROL,
        ALL_GUARDS_CONTROL,
        CANCEL_CONFIRM
    }

    // Getters for controlling guard and all guards mode
    public BodyGuard getControllingGuard(UUID playerUUID) {
        return controllingGuard.get(playerUUID);
    }

    public void setControllingGuard(UUID playerUUID, BodyGuard guard) {
        if (guard != null) {
            controllingGuard.put(playerUUID, guard);
        } else {
            controllingGuard.remove(playerUUID);
        }
    }

    public boolean isAllGuardsMode(UUID playerUUID) {
        return allGuardsMode.getOrDefault(playerUUID, false);
    }

    public void setAllGuardsMode(UUID playerUUID, boolean mode) {
        if (mode) {
            allGuardsMode.put(playerUUID, true);
        } else {
            allGuardsMode.remove(playerUUID);
        }
    }
}
