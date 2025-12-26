package me.mert.srapor.command;

import me.mert.srapor.SRapor;
import me.mert.srapor.gui.RaporMenu;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class RaporCommand implements CommandExecutor {

    private final SRapor plugin;

    public RaporCommand(SRapor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("only-player"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("rapor.use")) {
            player.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.msg("usage"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.msg("target-not-found"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.msg("self-report"));
            return true;
        }

        player.openInventory(RaporMenu.create(plugin, target.getUniqueId(), target.getName()));
        return true;
    }
}
