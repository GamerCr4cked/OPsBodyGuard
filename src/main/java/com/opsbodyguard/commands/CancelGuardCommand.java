package com.opsbodyguard.commands;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.gui.GUIManager;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CancelGuardCommand implements CommandExecutor {

    private final OPsBodyGuard plugin;

    public CancelGuardCommand(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        // Check permission
        if (!player.hasPermission("opsbodyguard.default")) {
            MessageUtil.send(player, "&cYou don't have permission to use this command!");
            return true;
        }

        // Check if player has guards
        List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
        if (guards.isEmpty()) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.no-guard"));
            return true;
        }

        GUIManager guiManager = plugin.getGUIManager();

        // Check for confirmation
        if (guiManager.hasCancelConfirmation(player.getUniqueId())) {
            // Confirmed - remove all guards (use copy to avoid ConcurrentModificationException)
            for (BodyGuard guard : new ArrayList<>(guards)) {
                plugin.getGuardManager().removeGuard(guard);
            }
            guiManager.removeCancelConfirmation(player.getUniqueId());
            MessageUtil.send(player, plugin.getConfig().getString("messages.guard-cancelled"));
        } else {
            // First time - ask for confirmation
            guiManager.setCancelConfirmation(player.getUniqueId());
            MessageUtil.send(player, plugin.getConfig().getString("messages.cancel-confirm"));
        }

        return true;
    }
}
