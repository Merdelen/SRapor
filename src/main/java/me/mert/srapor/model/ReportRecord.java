package me.mert.srapor.model;

public final class ReportRecord {

    private final long id;
    private final String reporterUuid;
    private final String targetUuid;
    private final String targetName;
    private final String reason;
    private final long createdAt;

    private final String status;
    private final String assignedToUuid;
    private final String assignedToName;
    private final Long assignedAt;
    private final String staffNote;
    private final Long resolvedAt;

    public ReportRecord(long id, String reporterUuid, String targetUuid, String targetName, String reason, long createdAt) {
        this(id, reporterUuid, targetUuid, targetName, reason, createdAt, "OPEN", null, null, null, null, null);
    }

    public ReportRecord(
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
    ) {
        this.id = id;
        this.reporterUuid = reporterUuid;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.reason = reason;
        this.createdAt = createdAt;
        this.status = status;
        this.assignedToUuid = assignedToUuid;
        this.assignedToName = assignedToName;
        this.assignedAt = assignedAt;
        this.staffNote = staffNote;
        this.resolvedAt = resolvedAt;
    }

    public long getId() {
        return id;
    }

    public String getReporterUuid() {
        return reporterUuid;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getReason() {
        return reason;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public String getAssignedToUuid() {
        return assignedToUuid;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public Long getAssignedAt() {
        return assignedAt;
    }

    public String getStaffNote() {
        return staffNote;
    }

    public Long getResolvedAt() {
        return resolvedAt;
    }
}
