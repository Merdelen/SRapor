package me.mert.srapor.service;

import me.mert.srapor.SRapor;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NotifyService {

    private final SRapor plugin;

    public NotifyService(SRapor plugin) {
        this.plugin = plugin;
    }

    public void notifyAdmins(String msg) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("rapor.admin")) {
                p.sendMessage(msg);
            }
        }
    }

    public void sendReportCreatedEmbed(String reporter, String target, String reason, long id, long createdAt) {
        if (!plugin.getConfig().getBoolean("webhook.embed.report-created.enabled", false)) return;

        String time = Instant.ofEpochMilli(createdAt)
                .atZone(plugin.getZoneId())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("reporter", reporter);
        fields.put("target", target);
        fields.put("reason", reason);
        fields.put("id", "#" + id);
        fields.put("time", time);

        sendEmbed(
                plugin.getConfig().getString("webhook.embed.report-created.title", ""),
                plugin.getConfig().getInt("webhook.embed.report-created.color", 0),
                "webhook.embed.report-created.fields.",
                fields
        );
    }

    public void sendReportClaimedEmbed(String admin, long id) {
        if (!plugin.getConfig().getBoolean("webhook.embed.report-claimed.enabled", false)) return;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("admin", admin);
        fields.put("id", "#" + id);

        sendEmbed(
                plugin.getConfig().getString("webhook.embed.report-claimed.title", ""),
                plugin.getConfig().getInt("webhook.embed.report-claimed.color", 0),
                "webhook.embed.report-claimed.fields.",
                fields
        );
    }

    public void sendReportResolvedEmbed(String admin, long id) {
        if (!plugin.getConfig().getBoolean("webhook.embed.report-resolved.enabled", false)) return;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("admin", admin);
        fields.put("id", "#" + id);

        sendEmbed(
                plugin.getConfig().getString("webhook.embed.report-resolved.title", ""),
                plugin.getConfig().getInt("webhook.embed.report-resolved.color", 0),
                "webhook.embed.report-resolved.fields.",
                fields
        );
    }

    private void sendEmbed(String title, int color, String fieldPrefix, Map<String, String> values) {
        if (!plugin.getConfig().getBoolean("webhook.enabled", false)) return;

        String url = plugin.getConfig().getString("webhook.url", "");
        if (url == null || url.trim().isEmpty()) return;

        String footer = plugin.getConfig().getString("webhook.embed.footer", "");
        boolean ts = plugin.getConfig().getBoolean("webhook.embed.show-timestamp", true);

        String payload = buildEmbedPayload(title, color, footer, ts, fieldPrefix, values);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(plugin.getConfig().getInt("webhook.timeout-ms", 5000));
                con.setReadTimeout(plugin.getConfig().getInt("webhook.timeout-ms", 5000));
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");

                byte[] out = payload.getBytes(StandardCharsets.UTF_8);
                con.setFixedLengthStreamingMode(out.length);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(out);
                }

                con.getInputStream().close();
            } catch (Exception ignored) {
            } finally {
                if (con != null) con.disconnect();
            }
        });
    }

    private String buildEmbedPayload(String title, int color, String footer, boolean ts, String fieldPrefix, Map<String, String> values) {
        StringBuilder fields = new StringBuilder();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String name = plugin.getConfig().getString(fieldPrefix + e.getKey(), e.getKey());
            fields.append("{\"name\":\"")
                    .append(json(name))
                    .append("\",\"value\":\"")
                    .append(json(e.getValue()))
                    .append("\",\"inline\":false},");
        }
        if (fields.length() > 0) fields.setLength(fields.length() - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{");
        sb.append("\"title\":\"").append(json(title)).append("\",");
        sb.append("\"color\":").append(color).append(",");
        sb.append("\"fields\":[").append(fields).append("]");

        if (footer != null && !footer.isEmpty()) {
            sb.append(",\"footer\":{\"text\":\"").append(json(footer)).append("\"}");
        }
        if (ts) {
            sb.append(",\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        }
        sb.append("}]}");
        return sb.toString();
    }

    private String json(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
