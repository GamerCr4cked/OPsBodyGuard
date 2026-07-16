package com.opsbodyguard.commands;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.guard.BodyGuard;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class AllGuardsCommand implements CommandExecutor {

    private final OPsBodyGuard plugin;

    public AllGuardsCommand(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (!player.hasPermission("opsbodyguard.default")) {
            MessageUtil.send(player, "&cYou don't have permission to use this command!");
            return true;
        }

        List<BodyGuard> guards = plugin.getGuardManager().getPlayerGuards(player.getUniqueId());
        if (guards.isEmpty()) {
            MessageUtil.send(player, plugin.getConfig().getString("messages.no-guard"));
            return true;
        }

        plugin.getGUIManager().openAllGuardsControlGUI(player);

        return true;
    }
}
