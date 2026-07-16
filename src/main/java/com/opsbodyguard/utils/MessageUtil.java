package com.opsbodyguard.utils;

import com.opsbodyguard.OPsBodyGuard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class MessageUtil {

    private static final String CONSOLE_PREFIX = "&f[&bOPsBodyGuard&f] &r";

    public static String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void send(Player player, String message) {
        if (player == null || message == null) return;
        String prefix = OPsBodyGuard.getInstance().getConfig().getString("messages.prefix", "&8[&bBodyGuard&8] &r");
        player.sendMessage(color(prefix + message));
    }

    public static void sendRaw(Player player, String message) {
        if (player == null || message == null) return;
        player.sendMessage(color(message));
    }

    // Console logging methods with colored prefix
    public static void logInfo(String message) {
        Bukkit.getConsoleSender().sendMessage(color(CONSOLE_PREFIX + "&a" + message));
    }

    public static void logWarning(String message) {
        Bukkit.getConsoleSender().sendMessage(color(CONSOLE_PREFIX + "&e" + message));
    }

    public static void logError(String message) {
        Bukkit.getConsoleSender().sendMessage(color(CONSOLE_PREFIX + "&c" + message));
    }

    public static void logDebug(String message) {
        Bukkit.getConsoleSender().sendMessage(color(CONSOLE_PREFIX + "&7" + message));
    }

    /**
     * Format a number with k/m/b/t abbreviations
     * @param number The number to format
     * @return Formatted string (e.g., 50000 -> "50k", 1500000 -> "1.5m")
     */
    public static String formatMoney(double number) {
        boolean useAbbreviations = OPsBodyGuard.getInstance().getConfig().getBoolean("formatting.use-abbreviations", true);

        if (!useAbbreviations) {
            return String.format("%.2f", number);
        }

        if (number >= 1_000_000_000_000L) {
            return String.format("%.2ft", number / 1_000_000_000_000.0);
        } else if (number >= 1_000_000_000L) {
            return String.format("%.2fb", number / 1_000_000_000.0);
        } else if (number >= 1_000_000L) {
            return String.format("%.2fm", number / 1_000_000.0);
        } else if (number >= 1_000L) {
            return String.format("%.2fk", number / 1_000.0);
        } else {
            return String.format("%.2f", number);
        }
    }

    /**
     * Format money with cleaner output (removes trailing zeros)
     */
    public static String formatMoneyClean(double number) {
        String formatted = formatMoney(number);
        // Remove unnecessary .00 but keep meaningful decimals
        if (formatted.endsWith(".00k") || formatted.endsWith(".00m") ||
            formatted.endsWith(".00b") || formatted.endsWith(".00t")) {
            formatted = formatted.replace(".00", "");
        } else if (formatted.endsWith(".00")) {
            formatted = formatted.replace(".00", "");
        }
        // Clean up single trailing zero
        if (formatted.matches(".*\\.\\d0[kmbt]$")) {
            formatted = formatted.replaceAll("0([kmbt])$", "$1");
        }
        return formatted;
    }
}
