package me.mert.srapor.model;

public record ReportRecord(
        long id,
        String reporterUuid,
        String targetUuid,
        String targetName,
        String reason,
        long createdAt,
        String status,
        String assignedToUuid,
        String assignedToName,
        Long assignedAt,
        String staffNote,
        Long resolvedAt
) { }