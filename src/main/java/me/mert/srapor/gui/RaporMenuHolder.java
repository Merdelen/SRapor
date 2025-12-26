package me.mert.srapor.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class RaporMenuHolder implements InventoryHolder {

    private final UUID targetUuid;

    public RaporMenuHolder(UUID targetUuid) {
        this.targetUuid = targetUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
