package com.opsbodyguard.commands;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final OPsBodyGuard plugin;

    public ReloadCommand(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("opsbodyguard.admin")) {
            sender.sendMessage(MessageUtil.color("&cYou don't have permission to use this command!"));
            return true;
        }

        // Reload config
        plugin.reloadConfig();

        sender.sendMessage(MessageUtil.color("&8[&bBodyGuard&8] &aConfig reloaded successfully!"));
        return true;
    }
}
