package me.mert.srapor.listener;

import me.mert.srapor.SRapor;
import me.mert.srapor.command.RaporlarCommand;
import me.mert.srapor.gui.AdminReportDetailHolder;
import me.mert.srapor.gui.AdminReportDetailMenu;
import me.mert.srapor.gui.AdminReportsHolder;
import me.mert.srapor.model.ReportRecord;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminReportsListener implements Listener {

    private final SRapor plugin;
    private final Map<UUID, Long> pendingNote = new ConcurrentHashMap<>();

    public AdminReportsListener(SRapor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pendingNote.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        Long reportId = pendingNote.get(p.getUniqueId());
        if (reportId == null) return;

        e.setCancelled(true);

        String msg = e.getMessage();
        String trimmed = msg.trim();

        if (trimmed.equalsIgnoreCase("iptal")) {
            pendingNote.remove(p.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> p.sendMessage(plugin.msg("note-cancelled")));
            return;
        }

        pendingNote.remove(p.getUniqueId());

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = plugin.getStorage().setStaffNote(reportId, trimmed);
            ReportRecord fresh = plugin.getStorage().getReportById(reportId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    p.sendMessage(plugin.msg("note-saved"));
                    if (fresh != null) p.openInventory(AdminReportDetailMenu.create(plugin, fresh));
                } else {
                    p.sendMessage(plugin.color("&cBir hata oluştu, not kaydedilemedi!"));
                }
            });
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        Inventory inv = e.getInventory();

        if (inv.getHolder() instanceof AdminReportsHolder h) {
            e.setCancelled(true);

            if (!p.hasPermission("rapor.admin")) return;

            ItemStack it = e.getCurrentItem();
            if (it == null || it.getType() == Material.AIR) return;

            int slot = e.getRawSlot();

            if (slot == 49) {
                p.closeInventory();
                return;
            }
            if (slot == 50) {
                new RaporlarCommand(plugin).openPage(p, h.getPage(), h.getFilter());
                return;
            }
            if (slot == 45) {
                new RaporlarCommand(plugin).openPage(p, h.getPage() - 1, h.getFilter());
                return;
            }
            if (slot == 53) {
                new RaporlarCommand(plugin).openPage(p, h.getPage() + 1, h.getFilter());
                return;
            }
            if (slot == 48) {
                String next = nextFilter(h.getFilter());
                new RaporlarCommand(plugin).openPage(p, 0, next);
                return;
            }

            if (slot < 0 || slot >= 45) return;

            List<ReportRecord> list = h.getReports();
            if (slot >= list.size()) return;

            ReportRecord r = list.get(slot);
            p.openInventory(AdminReportDetailMenu.create(plugin, r, h.getPage(), h.getFilter()));
            return;
        }

        if (inv.getHolder() instanceof AdminReportDetailHolder h) {
            e.setCancelled(true);

            if (!p.hasPermission("rapor.admin")) return;

            ItemStack it = e.getCurrentItem();
            if (it == null || it.getType() == Material.AIR) return;

            ReportRecord r = h.getReport();
            int slot = e.getRawSlot();

            if (slot == 26) {
                p.closeInventory();
                return;
            }

            if (slot == 22) {
                new RaporlarCommand(plugin).openPage(p, h.getPage(), h.getFilter());
                return;
            }

            if (slot == 11) {
                try {
                    UUID target = UUID.fromString(r.targetUuid());
                    Player t = Bukkit.getPlayer(target);
                    if (t != null) p.teleport(t.getLocation());
                } catch (Exception ignored) {
                }
                return;
            }

            if (slot == 15) {
                int page = h.getPage();
                String filter = h.getFilter();
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getStorage().deleteReportById(r.id());
                    Bukkit.getScheduler().runTask(plugin, () -> new RaporlarCommand(plugin).openPage(p, page, filter));
                });
                return;
            }

            if (slot == 10) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = plugin.getStorage().claimReport(
                            r.id(),
                            p.getUniqueId().toString(),
                            p.getName(),
                            System.currentTimeMillis()
                    );

                    if (ok) {
                        plugin.getNotifyService().sendReportClaimedEmbed(p.getName(), r.id());
                    }

                    ReportRecord fresh = plugin.getStorage().getReportById(r.id());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.sendMessage(ok ? plugin.msg("report-claim-success") : plugin.msg("report-claim-fail"));
                        if (fresh != null) p.openInventory(AdminReportDetailMenu.create(plugin, fresh, h.getPage(), h.getFilter()));
                    });
                });
                return;
            }

            if (slot == 16) {
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    boolean ok = plugin.getStorage().resolveReport(r.id(), System.currentTimeMillis());

                        if (ok) {
                        plugin.getNotifyService().sendReportResolvedEmbed(p.getName(), r.id());
                    }

                    ReportRecord fresh = plugin.getStorage().getReportById(r.id());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        p.sendMessage(ok ? plugin.msg("report-resolve-success") : plugin.msg("report-resolve-fail"));
                        if (fresh != null) p.openInventory(AdminReportDetailMenu.create(plugin, fresh, h.getPage(), h.getFilter()));
                    });
                });
                return;
            }

            if (slot == 12) {
                pendingNote.put(p.getUniqueId(), r.id());
                p.closeInventory();
                p.sendMessage(plugin.msg("note-prompt"));
            }
        }
    }

    private String nextFilter(String current) {
        String c = current == null ? "ALL" : current;
        if ("ALL".equalsIgnoreCase(c)) return "OPEN";
        if ("OPEN".equalsIgnoreCase(c)) return "CLAIMED";
        if ("CLAIMED".equalsIgnoreCase(c)) return "RESOLVED";
        return "ALL";
    }
}