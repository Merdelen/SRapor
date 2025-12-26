package me.mert.srapor.listener;

import me.mert.srapor.SRapor;
import me.mert.srapor.gui.RaporMenuHolder;
import me.mert.srapor.model.ReportReason;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class RaporMenuListener implements Listener {

    private final SRapor plugin;

    public RaporMenuListener(SRapor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;

        Inventory inv = e.getInventory();
        if (!(inv.getHolder() instanceof RaporMenuHolder)) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        ConfigurationSection close = plugin.getConfig().getConfigurationSection("menu.close");
        if (close != null) {
            Material cm = Material.matchMaterial(close.getString("material", "BARRIER"));
            if (cm != null && item.getType() == cm) {
                player.closeInventory();
                return;
            }
        }

        ReportReason reason = findReason(item.getType());
        if (reason == null) return;

        UUID targetUuid = ((RaporMenuHolder) inv.getHolder()).getTargetUuid();
        String targetName = plugin.getServer().getOfflinePlayer(targetUuid).getName();
        if (targetName == null) targetName = "Unknown";

        plugin.getReportService().submit(player, targetUuid, targetName, reason);
        player.closeInventory();
    }

    private ReportReason findReason(Material clicked) {
        ConfigurationSection reasons = plugin.getConfig().getConfigurationSection("reasons");
        if (reasons == null) return null;

        for (String key : reasons.getKeys(false)) {
            ConfigurationSection r = reasons.getConfigurationSection(key);
            if (r == null) continue;
            if (!r.getBoolean("enabled", true)) continue;

            ReportReason reason;
            try {
                reason = ReportReason.valueOf(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            Material m = Material.matchMaterial(r.getString("material", reason.getMaterial().name()));
            if (m != null && m == clicked) return reason;
        }
        return null;
    }
}
