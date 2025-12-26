package me.mert.srapor;

import me.mert.srapor.command.RaporCommand;
import me.mert.srapor.command.RaporlarCommand;
import me.mert.srapor.command.SRaporCommand;
import me.mert.srapor.listener.AdminReportsListener;
import me.mert.srapor.listener.RaporMenuListener;
import me.mert.srapor.service.NotifyService;
import me.mert.srapor.service.ReportService;
import me.mert.srapor.storage.SqliteStorage;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class SRapor extends JavaPlugin {

    private SqliteStorage storage;
    private NotifyService notifyService;
    private ReportService reportService;

    private ZoneId zoneId;
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initServices();

        if (getCommand("rapor") != null) getCommand("rapor").setExecutor(new RaporCommand(this));
        if (getCommand("srapor") != null) getCommand("srapor").setExecutor(new SRaporCommand(this));
        if (getCommand("raporlar") != null) getCommand("raporlar").setExecutor(new RaporlarCommand(this));

        getServer().getPluginManager().registerEvents(new RaporMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminReportsListener(this), this);

        getLogger().info("SRapor aktif");
    }

    private void initServices() {
        zoneId = ZoneId.of(getConfig().getString("timezone", "Europe/Istanbul"));

        if (storage != null) storage.close();
        storage = new SqliteStorage(this);
        storage.init();

        notifyService = new NotifyService(this);
        reportService = new ReportService(this, storage, notifyService, zoneId);
    }

    public void reloadAll() {
        reloadConfig();
        initServices();
    }

    public SqliteStorage getStorage() {
        return storage;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public NotifyService getNotifyService() {
        return notifyService;
    }

    public String msg(String key) {
        return color(getConfig().getString("messages." + key, ""));
    }

    public String cfg(String path, String def) {
        return color(getConfig().getString(path, def));
    }

    public List<String> cfgList(String path) {
        List<String> list = getConfig().getStringList(path);
        if (list == null) return Collections.emptyList();
        for (int i = 0; i < list.size(); i++) list.set(i, color(list.get(i)));
        return list;
    }

    public Material cfgMaterial(String path, Material def) {
        String s = getConfig().getString(path, "");
        Material m = Material.matchMaterial(s);
        return m != null ? m : def;
    }

    public String color(String s) {
        if (s == null) return "";
        s = HEX.matcher(s).replaceAll("\u00A7x\u00A7$1");
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.close();
        getLogger().info("SRapor kapandi");
    }
}
