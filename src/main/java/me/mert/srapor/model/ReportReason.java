package me.mert.srapor.model;

import org.bukkit.Material;

public enum ReportReason {
    CHEAT("Hile", Material.DIAMOND_SWORD),
    AD("Reklam", Material.OAK_SIGN),
    SWEAR("Kufur", Material.PAPER),
    SPAM("Spam", Material.FIREWORK_ROCKET),
    OTHER("Diger", Material.ANVIL);

    private final String title;
    private final Material material;

    ReportReason(String title, Material material) {
        this.title = title;
        this.material = material;
    }

    public String getTitle() {
        return title;
    }

    public Material getMaterial() {
        return material;
    }
}
