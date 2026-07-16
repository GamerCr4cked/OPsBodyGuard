package com.opsbodyguard.manager;

import com.opsbodyguard.OPsBodyGuard;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyManager {

    private final OPsBodyGuard plugin;
    private final Economy economy;

    public EconomyManager(OPsBodyGuard plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public boolean hasEnough(Player player, double amount) {
        return economy.has(player, amount);
    }

    public boolean hasEnough(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!hasEnough(player, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean withdraw(UUID playerUUID, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!hasEnough(playerUUID, amount)) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public double getBalance(UUID playerUUID) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return economy.getBalance(player);
    }

    public double getPricePerDay() {
        return plugin.getConfig().getDouble("price.per-day", 50000);
    }

    public double getPriceForDays(int days) {
        double pricePerDay = getPricePerDay();
        double totalPrice = pricePerDay * days;

        // Apply discount if applicable
        double discountPercent = getDiscountForDays(days);
        if (discountPercent > 0) {
            totalPrice = totalPrice * (100 - discountPercent) / 100;
        }

        return totalPrice;
    }

    /**
     * Calculate discount for given days using configurable min/max discount
     * Formula creates a gentle curve from min-discount to max-discount
     */
    public double getDiscountForDays(int days) {
        // First check if there's a preset package discount
        String presetPath = "day-packages." + days + ".discount";
        if (plugin.getConfig().contains(presetPath)) {
            return plugin.getConfig().getDouble(presetPath, 0);
        }

        // Otherwise calculate discount based on custom-days config
        int minDays = plugin.getConfig().getInt("custom-days.min-days", 5);
        int maxDays = plugin.getConfig().getInt("custom-days.max-days", 30);
        double minDiscount = plugin.getConfig().getDouble("custom-days.discount.min-discount", 8);
        double maxDiscount = plugin.getConfig().getDouble("custom-days.discount.max-discount", 68);

        // If days is below min, no discount
        if (days < minDays) {
            return 0;
        }

        // If days is at or above max, return max discount
        if (days >= maxDays) {
            return maxDiscount;
        }

        // Calculate discount using linear interpolation
        // discount = minDiscount + (days - minDays) / (maxDays - minDays) * (maxDiscount - minDiscount)
        double progress = (double) (days - minDays) / (maxDays - minDays);
        double discount = minDiscount + (progress * (maxDiscount - minDiscount));

        return Math.round(discount * 100.0) / 100.0; // Round to 2 decimal places
    }
}
