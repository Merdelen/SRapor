package me.mert.srapor.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
        if (url.trim().isEmpty()) return;

        String footer = plugin.getConfig().getString("webhook.embed.footer", "");
        boolean ts = plugin.getConfig().getBoolean("webhook.embed.show-timestamp", true);

        String payload = buildJsonPayload(title, color, footer, ts, fieldPrefix, values);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) new URL(url).openConnection();
                con.setRequestMethod("POST");
                con.setConnectTimeout(plugin.getConfig().getInt("webhook.timeout-ms", 5000));
                con.setReadTimeout(plugin.getConfig().getInt("webhook.timeout-ms", 5000));
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("User-Agent", "SRapor-Plugin");

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

    private String buildJsonPayload(String title, int color, String footer, boolean ts, String fieldPrefix, Map<String, String> values) {
        JsonObject root = new JsonObject();
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();

        embed.addProperty("title", title);
        embed.addProperty("color", color);

        if (footer != null && !footer.isEmpty()) {
            JsonObject footerObj = new JsonObject();
            footerObj.addProperty("text", footer);
            embed.add("footer", footerObj);
        }

        if (ts) {
            embed.addProperty("timestamp", Instant.now().toString());
        }

        JsonArray fieldsArray = new JsonArray();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String fieldName = plugin.getConfig().getString(fieldPrefix + e.getKey(), e.getKey());

            JsonObject fieldObj = new JsonObject();
            fieldObj.addProperty("name", fieldName);
            fieldObj.addProperty("value", e.getValue());
            fieldObj.addProperty("inline", false);

            fieldsArray.add(fieldObj);
        }
        embed.add("fields", fieldsArray);

        embeds.add(embed);
        root.add("embeds", embeds);

        return root.toString();
    }
}