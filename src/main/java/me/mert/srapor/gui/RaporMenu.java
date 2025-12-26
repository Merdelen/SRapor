package me.mert.srapor.gui;

import me.mert.srapor.SRapor;
import me.mert.srapor.model.ReportReason;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RaporMenu {

    public static Inventory create(SRapor plugin, UUID targetUuid, String targetName) {
        int size = plugin.getConfig().getInt("menu.size", 27);
        String title = plugin.color(plugin.getConfig().getString("menu.title", "&8Rapor: &f{target}").replace("{target}", targetName));

        Inventory inv = Bukkit.createInventory(new RaporMenuHolder(targetUuid), size, title);

        ItemStack filler = item(plugin,
                plugin.getConfig().getString("menu.filler.material", "GRAY_STAINED_GLASS_PANE"),
                plugin.getConfig().getString("menu.filler.name", " "),
                null);

        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        ConfigurationSection header = plugin.getConfig().getConfigurationSection("menu.header");
        if (header != null) {
            int slot = header.getInt("slot", 4);
            String mat = header.getString("material", "PLAYER_HEAD");
            String name = header.getString("name", "&d✦ &fRapor Menusu");
            List<String> lore = header.getStringList("lore");
            inv.setItem(slot, item(plugin, mat, name.replace("{target}", targetName), replaceTarget(lore, targetName)));
        }

        ConfigurationSection close = plugin.getConfig().getConfigurationSection("menu.close");
        if (close != null) {
            int slot = close.getInt("slot", 22);
            String mat = close.getString("material", "BARRIER");
            String name = close.getString("name", "&c✖ Kapat");
            inv.setItem(slot, item(plugin, mat, name, null));
        }

        ConfigurationSection reasons = plugin.getConfig().getConfigurationSection("reasons");
        if (reasons != null) {
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

                int slot = r.getInt("slot", -1);
                if (slot < 0 || slot >= size) continue;

                String mat = r.getString("material", reason.getMaterial().name());
                String name = r.getString("name", reason.getTitle());
                List<String> lore = r.getStringList("lore");

                inv.setItem(slot, item(plugin, mat, name, lore));
            }
        }

        return inv;
    }

    private static ItemStack item(SRapor plugin, String materialName, String displayName, List<String> lore) {
        Material m = Material.matchMaterial(materialName);
        if (m == null) m = Material.PAPER;

        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(displayName));
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>(lore.size());
                for (String s : lore) colored.add(plugin.color(s));
                meta.setLore(colored);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    private static List<String> replaceTarget(List<String> lore, String targetName) {
        if (lore == null || lore.isEmpty()) return lore;
        List<String> out = new ArrayList<>(lore.size());
        for (String s : lore) out.add(s.replace("{target}", targetName));
        return out;
    }
}
