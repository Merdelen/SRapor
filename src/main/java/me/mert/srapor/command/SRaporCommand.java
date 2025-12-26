package me.mert.srapor.command;

import me.mert.srapor.SRapor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SRaporCommand implements CommandExecutor {

    private final SRapor plugin;

    public SRaporCommand(SRapor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (!p.hasPermission("rapor.reload")) {
                    p.sendMessage(plugin.msg("no-permission"));
                    return true;
                }
            }
            plugin.reloadAll();
            sender.sendMessage(ChatColor.GREEN + "SRapor yeniden yuklendi.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "Kullanim: /srapor reload");
        return true;
    }
}
