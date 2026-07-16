package com.opsbodyguard.commands;

import com.opsbodyguard.OPsBodyGuard;
import com.opsbodyguard.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HelpCommand implements CommandExecutor {

    private final OPsBodyGuard plugin;

    public HelpCommand(OPsBodyGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        double pricePerDay = plugin.getConfig().getDouble("price.per-day", 50000);
        int maxGuards = plugin.getConfig().getInt("limits.max-guards-per-player", 5);
        String maxGuardsStr = maxGuards == -1 ? "Unlimited" : String.valueOf(maxGuards);
        String priceStr = MessageUtil.formatMoneyClean(pricePerDay);

        MessageUtil.sendRaw(player, "&7&m-----------------------------------------");
        MessageUtil.sendRaw(player, "&b&l  OPsBodyGuard &7- &fHelp Guide");
        MessageUtil.sendRaw(player, "&7&m-----------------------------------------");

        // Commands
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&b&lCommands:");
        MessageUtil.sendRaw(player, "  &f/guardbuy &7(&b/gb&7) - &fOpen the bodyguard shop");
        MessageUtil.sendRaw(player, "  &f/guardcontrol &7(&b/gc&7) - &fOpen guard control GUI");
        MessageUtil.sendRaw(player, "  &f/guardall &7(&b/ga&7) - &fControl all your guards");
        MessageUtil.sendRaw(player, "  &f/guardcancel &7(&b/gx&7) - &fRemove all your guards");
        MessageUtil.sendRaw(player, "  &f/guardhelp &7(&b/gh&7) - &fShow this help message");

        if (player.hasPermission("opsbodyguard.admin")) {
            MessageUtil.sendRaw(player, "  &f/guardreload &7(&b/gr&7) - &fReload plugin config");
        }

        // Interaction
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&b&lInteraction:");
        MessageUtil.sendRaw(player, "  &6Right-click &fyour guard &7- &fOpen control GUI");
        MessageUtil.sendRaw(player, "  &6Sneak + Right-click &fyour guard &7- &fControl all guards");

        // Guard Modes
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&b&lGuard Modes:");
        MessageUtil.sendRaw(player, "  &c Aggressive &7- &fAttacks anyone nearby");
        MessageUtil.sendRaw(player, "  &e Neutral &7- &fHit an entity to sic your guard on it");
        MessageUtil.sendRaw(player, "  &a Passive &7- &fWon't attack anyone");
        MessageUtil.sendRaw(player, "  &4 Hostile &7- &fOnly attacks hostile mobs");
        MessageUtil.sendRaw(player, "  &d Custom Attack &7- &fTargets a specific player");

        // Chat Commands
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&b&lChat Commands &7(requires Order toggle ON)&b:");
        MessageUtil.sendRaw(player, "  &fgstop &7- &fStop guards from attacking");
        MessageUtil.sendRaw(player, "  &fgattack &7- &fResume guard attacks");

        // Info
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&b&lInfo:");
        MessageUtil.sendRaw(player, "  &7Price per MC day: &b$" + priceStr);
        MessageUtil.sendRaw(player, "  &7Max guards: &b" + maxGuardsStr);

        MessageUtil.sendRaw(player, "&7&m-----------------------------------------");

        return true;
    }
}
