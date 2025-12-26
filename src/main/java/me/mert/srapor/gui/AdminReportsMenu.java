package me.mert.srapor.gui;

import me.mert.srapor.SRapor;
import me.mert.srapor.model.ReportReason;
import me.mert.srapor.model.ReportRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminReportsMenu {

    public static final int SIZE = 54;

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static Inventory create(SRapor plugin, int page, List<ReportRecord> reports) {
        return create(plugin, page, reports, "ALL");
    }

    public static Inventory create(SRapor plugin, int page, List<ReportRecord> reports, String filter) {
        String title = plugin.color("&8Raporlar &7(Sayfa: &f" + (page + 1) + "&7)");
        Inventory inv = Bukkit.createInventory(new AdminReportsHolder(page, reports, filter), SIZE, title);

        ItemStack filler = item(plugin, "BLACK_STAINED_GLASS_PANE", " ");
        for (int i = 45; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(45, item(plugin, "ARROW", "&b⬅ &fOnceki"));
        inv.setItem(49, item(plugin, "BARRIER", "&c✖ Kapat"));
        inv.setItem(50, item(plugin, "LIME_DYE", "&a⟲ Yenile"));
        inv.setItem(53, item(plugin, "ARROW", "&fSonraki &b➡"));

        inv.setItem(48, filterItem(plugin, filter));

        int slot = 0;
        for (ReportRecord r : reports) {
            if (slot >= 45) break;
            inv.setItem(slot++, reportItem(plugin, r, plugin.getZoneId()));
        }

        return inv;
    }

    private static ItemStack filterItem(SRapor plugin, String filter) {
        String f = filter == null ? "ALL" : filter.toUpperCase();
        String label = plugin.cfg("admin-menu.reports.filter.labels." + f, "&bHepsi");
        Material m = plugin.cfgMaterial("admin-menu.reports.filter.button.material", Material.HOPPER);
        String name = plugin.cfg("admin-menu.reports.filter.button.name", "&b⛭ &fFiltre: {filter}").replace("{filter}", label);

        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lore = plugin.cfgList("admin-menu.reports.filter.button.lore");
            meta.setLore(lore.isEmpty() ? null : lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack reportItem(SRapor plugin, ReportRecord r, ZoneId zoneId) {
        Material m = Material.PAPER;
        String reasonTitle = r.reason();

        try {
            ReportReason rr = ReportReason.valueOf(r.reason());
            reasonTitle = rr.getTitle();
            m = rr.getMaterial();
        } catch (Exception ignored) {
        }

        String targetName = r.targetName() != null ? r.targetName() : "Unknown";
        String reporterName = nameFromUuid(plugin, r.reporterUuid());

        String time = Instant.ofEpochMilli(r.createdAt()).atZone(zoneId).format(F);

        String statusRaw = r.status() != null ? r.status() : "OPEN";
        String statusText = statusText(statusRaw);

        String assignedName = r.assignedToName();
        if (assignedName == null || assignedName.trim().isEmpty()) assignedName = "-";

        String note = r.staffNote();
        if (note == null || note.trim().isEmpty()) note = "-";

        String resolvedTime = "-";
        if (r.resolvedAt() != null) {
            resolvedTime = Instant.ofEpochMilli(r.resolvedAt()).atZone(zoneId).format(F);
        }

        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7ID: &f#" + r.id()));
        lore.add(plugin.color("&7Hedef: &f" + targetName));
        lore.add(plugin.color("&7Raporlayan: &f" + reporterName));
        lore.add(plugin.color("&7Sebep: &f" + reasonTitle));
        lore.add(plugin.color("&7Tarih: &f" + time));
        lore.add(" ");
        lore.add(plugin.color("&7Durum: " + statusText));
        lore.add(plugin.color("&7Üstlenen: &f" + assignedName));
        lore.add(plugin.color("&7Not: &f" + note));
        lore.add(plugin.color("&7Çözüldü: &f" + resolvedTime));
        lore.add(" ");
        lore.add(plugin.color("&a✔ Incele"));

        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color("&d✦ &f" + targetName + " &7- &b" + reasonTitle));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String statusText(String statusRaw) {
        if (statusRaw == null) return "&aAçık";
        if ("CLAIMED".equalsIgnoreCase(statusRaw)) return "&eÜstlenildi";
        if ("RESOLVED".equalsIgnoreCase(statusRaw)) return "&aÇözüldü";
        return "&aAçık";
    }

    private static ItemStack item(SRapor plugin, String materialName, String name) {
        Material m = Material.matchMaterial(materialName);
        if (m == null) m = Material.PAPER;
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color(name));
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String nameFromUuid(SRapor plugin, String uuidStr) {
        try {
            UUID u = UUID.fromString(uuidStr);
            OfflinePlayer op = plugin.getServer().getOfflinePlayer(u);
            String n = op.getName();
            return n != null ? n : shortUuid(uuidStr);
        } catch (Exception e) {
            return shortUuid(uuidStr);
        }
    }

    private static String shortUuid(String s) {
        if (s == null) return "Unknown";
        return s.length() <= 8 ? s : s.substring(0, 8);
    }
}
