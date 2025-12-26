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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AdminReportDetailMenu {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static Inventory create(SRapor plugin, ReportRecord r) {
        return create(plugin, r, 0, "ALL");
    }

    public static Inventory create(SRapor plugin, ReportRecord r, int page, String filter) {
        String title = plugin.color("&8Rapor Detay &7(#" + r.id() + ")");
        Inventory inv = Bukkit.createInventory(new AdminReportDetailHolder(r, page, filter), 27, title);

        ItemStack filler = item(plugin, "GRAY_STAINED_GLASS_PANE", " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, filler);

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
        String time = Instant.ofEpochMilli(r.createdAt()).atZone(plugin.getZoneId()).format(F);

        String statusRaw = r.status() != null ? r.status() : "OPEN";
        String statusText = statusText(statusRaw);

        String assignedName = r.assignedToName();
        if (assignedName == null || assignedName.trim().isEmpty()) assignedName = "-";

        String note = r.staffNote();
        if (note == null || note.trim().isEmpty()) note = "-";

        String resolvedTime = "-";
        if (r.resolvedAt() != null) {
            resolvedTime = Instant.ofEpochMilli(r.resolvedAt()).atZone(plugin.getZoneId()).format(F);
        }

        List<String> lore = new ArrayList<>();
        lore.add(plugin.color("&7Hedef: &f" + targetName));
        lore.add(plugin.color("&7Raporlayan: &f" + reporterName));
        lore.add(plugin.color("&7Sebep: &f" + reasonTitle));
        lore.add(plugin.color("&7Tarih: &f" + time));
        lore.add(" ");
        lore.add(plugin.color("&7Durum: " + statusText));
        lore.add(plugin.color("&7Üstlenen: &f" + assignedName));
        lore.add(plugin.color("&7Not: &f" + note));
        lore.add(plugin.color("&7Çözüldü: &f" + resolvedTime));

        ItemStack main = new ItemStack(m);
        ItemMeta mm = main.getItemMeta();
        if (mm != null) {
            mm.setDisplayName(plugin.color("&d✦ &f" + targetName + " &7- &b" + reasonTitle));
            mm.setLore(lore);
            main.setItemMeta(mm);
        }
        inv.setItem(13, main);

        boolean canClaim = "OPEN".equalsIgnoreCase(statusRaw);
        boolean canResolve = !"RESOLVED".equalsIgnoreCase(statusRaw);

        inv.setItem(10, claimItem(plugin, canClaim));
        inv.setItem(12, noteItem(plugin));
        inv.setItem(16, resolveItem(plugin, canResolve));

        inv.setItem(11, item(plugin, "ENDER_PEARL", "&b➤ &fHedefe Isinlan"));
        inv.setItem(15, item(plugin, "REDSTONE_BLOCK", "&c✖ &fRaporu Sil"));
        inv.setItem(22, item(plugin, "ARROW", "&b⬅ &fGeri"));
        inv.setItem(26, item(plugin, "BARRIER", "&cKapat"));

        return inv;
    }

    private static ItemStack claimItem(SRapor plugin, boolean enabled) {
        String base = enabled ? "admin-menu.detail.buttons.claim.enabled" : "admin-menu.detail.buttons.claim.disabled";
        Material m = plugin.cfgMaterial(base + ".material", enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        String name = plugin.cfg(base + ".name", enabled ? "&a✔ &fÜstlen" : "&7✔ &8Üstlenilemez");
        return item(plugin, m.name(), name);
    }

    private static ItemStack noteItem(SRapor plugin) {
        Material m = plugin.cfgMaterial("admin-menu.detail.buttons.note.material", Material.BOOK);
        String name = plugin.cfg("admin-menu.detail.buttons.note.name", "&e✎ &fNot");
        return item(plugin, m.name(), name);
    }

    private static ItemStack resolveItem(SRapor plugin, boolean enabled) {
        String base = enabled ? "admin-menu.detail.buttons.resolve.enabled" : "admin-menu.detail.buttons.resolve.disabled";
        Material m = plugin.cfgMaterial(base + ".material", enabled ? Material.EMERALD_BLOCK : Material.GRAY_CONCRETE);
        String name = plugin.cfg(base + ".name", enabled ? "&a✓ &fÇözüldü" : "&7✓ &8Zaten Çözüldü");
        return item(plugin, m.name(), name);
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
