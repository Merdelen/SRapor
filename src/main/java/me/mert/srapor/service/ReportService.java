package me.mert.srapor.service;

import me.mert.srapor.SRapor;
import me.mert.srapor.model.ReportReason;
import me.mert.srapor.storage.SqliteStorage;
import me.mert.srapor.util.DayKey;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.util.UUID;

public final class ReportService {

    private final SRapor plugin;
    private final SqliteStorage storage;
    private final NotifyService notifyService;
    private final ZoneId zoneId;

    public ReportService(SRapor plugin, SqliteStorage storage, NotifyService notifyService, ZoneId zoneId) {
        this.plugin = plugin;
        this.storage = storage;
        this.notifyService = notifyService;
        this.zoneId = zoneId;
    }

    public void submit(Player reporter, UUID targetUuid, String targetName, ReportReason reason) {
        String day = DayKey.today(zoneId);
        long now = System.currentTimeMillis();

        String reporterId = reporter.getUniqueId().toString();
        String targetId = targetUuid.toString();

        boolean inserted = storage.tryInsertDailyLimit(reporterId, targetId, reason.name(), day, now);
        if (!inserted) {
            reporter.sendMessage(plugin.msg("already-reported"));
            return;
        }

        String sent = plugin.getConfig().getString("messages.report-sent", "");
        sent = sent.replace("{target}", targetName).replace("{reason}", reason.getTitle());
        reporter.sendMessage(plugin.color(sent));

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            long id = storage.insertReportReturnId(reporterId, targetId, targetName, reason.name(), now);

            String admin = plugin.getConfig().getString("messages.admin-notify", "");
            admin = admin.replace("{reporter}", reporter.getName()).replace("{target}", targetName).replace("{reason}", reason.getTitle());
            String adminMsg = plugin.color(admin);

            long safeId = id > 0 ? id : -1L;

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                notifyService.notifyAdmins(adminMsg);
                if (safeId > 0) {
                    notifyService.sendReportCreatedEmbed(reporter.getName(), targetName, reason.getTitle(), safeId, now);
                } else {
                    notifyService.sendReportCreatedEmbed(reporter.getName(), targetName, reason.getTitle(), 0L, now);
                }
            });
        });
    }
}
