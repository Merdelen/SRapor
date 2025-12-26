package me.mert.srapor.command;

import me.mert.srapor.SRapor;
import me.mert.srapor.gui.AdminReportsMenu;
import me.mert.srapor.model.ReportRecord;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class RaporlarCommand implements CommandExecutor {

    private final SRapor plugin;

    public RaporlarCommand(SRapor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.msg("only-player"));
            return true;
        }

        Player p = (Player) sender;

        if (!p.hasPermission("rapor.admin")) {
            p.sendMessage(plugin.msg("no-permission"));
            return true;
        }

        openPage(p, 0);
        return true;
    }

    public void openPage(Player p, int page) {
        openPage(p, page, "ALL");
    }

    public void openPage(Player p, int page, String filter) {
        int safePage = Math.max(0, page);
        int limit = 45;
        int offset = safePage * limit;

        String f = filter == null ? "ALL" : filter;

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ReportRecord> reports = plugin.getStorage().listRecentReportsFiltered(limit, offset, f);
            plugin.getServer().getScheduler().runTask(plugin, () -> p.openInventory(AdminReportsMenu.create(plugin, safePage, reports, f)));
        });
    }
}
