package me.mert.srapor.gui;

import me.mert.srapor.model.ReportRecord;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public final class AdminReportsHolder implements InventoryHolder {

    private final int page;
    private final List<ReportRecord> reports;
    private final String filter;

    public AdminReportsHolder(int page, List<ReportRecord> reports, String filter) {
        this.page = page;
        this.reports = reports;
        this.filter = filter == null ? "ALL" : filter;
    }

    public int getPage() {
        return page;
    }

    public List<ReportRecord> getReports() {
        return reports;
    }

    public String getFilter() {
        return filter;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
