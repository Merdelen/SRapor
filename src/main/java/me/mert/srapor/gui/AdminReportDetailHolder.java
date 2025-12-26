package me.mert.srapor.gui;

import me.mert.srapor.model.ReportRecord;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminReportDetailHolder implements InventoryHolder {

    private final ReportRecord report;
    private final int page;
    private final String filter;

    public AdminReportDetailHolder(ReportRecord report) {
        this(report, 0, "ALL");
    }

    public AdminReportDetailHolder(ReportRecord report, int page, String filter) {
        this.report = report;
        this.page = Math.max(0, page);
        this.filter = filter == null ? "ALL" : filter;
    }

    public ReportRecord getReport() {
        return report;
    }

    public int getPage() {
        return page;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
