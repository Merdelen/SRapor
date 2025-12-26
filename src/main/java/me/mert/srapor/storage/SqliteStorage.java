package me.mert.srapor.storage;

import me.mert.srapor.model.ReportRecord;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SqliteStorage {

    private final JavaPlugin plugin;
    private String jdbcUrl;
    private Connection keepAlive;

    public SqliteStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
        }

        File folder = plugin.getDataFolder();
        if (!folder.exists()) folder.mkdirs();

        File dbFile = new File(folder, "srapor.db");
        jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            keepAlive = DriverManager.getConnection(jdbcUrl);
            try (Statement st = keepAlive.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA temp_store=MEMORY");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
        }

        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS report_limits (" +
                            "reporter TEXT NOT NULL," +
                            "target TEXT NOT NULL," +
                            "reason TEXT NOT NULL," +
                            "day TEXT NOT NULL," +
                            "created_at INTEGER NOT NULL," +
                            "PRIMARY KEY (reporter, target, reason, day)" +
                            ")"
            );
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS reports (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "reporter TEXT NOT NULL," +
                            "target TEXT NOT NULL," +
                            "target_name TEXT NOT NULL," +
                            "reason TEXT NOT NULL," +
                            "created_at INTEGER NOT NULL" +
                            ")"
            );
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_target ON reports(target)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_created ON reports(created_at)");
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
        }

        try (Connection c = getConnection()) {
            migrateReportsTable(c);
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_reports_assigned_to_uuid ON reports(assigned_to_uuid)");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(e.getMessage());
        }
    }

    private void migrateReportsTable(Connection c) throws SQLException {
        ensureColumn(c, "reports", "status", "TEXT NOT NULL DEFAULT 'OPEN'");
        ensureColumn(c, "reports", "assigned_to_uuid", "TEXT");
        ensureColumn(c, "reports", "assigned_to_name", "TEXT");
        ensureColumn(c, "reports", "assigned_at", "INTEGER");
        ensureColumn(c, "reports", "staff_note", "TEXT");
        ensureColumn(c, "reports", "resolved_at", "INTEGER");
    }

    private void ensureColumn(Connection c, String table, String column, String ddl) throws SQLException {
        if (hasColumn(c, table, column)) return;
        try (Statement st = c.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + ddl);
        }
    }

    private boolean hasColumn(Connection c, String table, String column) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        }
        return false;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    public void close() {
        if (keepAlive != null) {
            try {
                keepAlive.close();
            } catch (SQLException ignored) {
            }
            keepAlive = null;
        }
    }

    public boolean tryInsertDailyLimit(String reporter, String target, String reason, String day, long createdAt) {
        String sql = "INSERT INTO report_limits(reporter, target, reason, day, created_at) VALUES(?,?,?,?,?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, reporter);
            ps.setString(2, target);
            ps.setString(3, reason);
            ps.setString(4, day);
            ps.setLong(5, createdAt);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public void insertReportAsync(JavaPlugin plugin, String reporter, String target, String targetName, String reason, long createdAt) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO reports(reporter, target, target_name, reason, created_at) VALUES(?,?,?,?,?)";
            try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, reporter);
                ps.setString(2, target);
                ps.setString(3, targetName);
                ps.setString(4, reason);
                ps.setLong(5, createdAt);
                ps.executeUpdate();
            } catch (SQLException ignored) {
            }
        });
    }

    public long insertReportReturnId(String reporter, String target, String targetName, String reason, long createdAt) {
        String sql = "INSERT INTO reports(reporter, target, target_name, reason, created_at) VALUES(?,?,?,?,?)";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reporter);
            ps.setString(2, target);
            ps.setString(3, targetName);
            ps.setString(4, reason);
            ps.setLong(5, createdAt);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException ignored) {
        }
        return -1L;
    }

    public List<ReportRecord> listRecentReports(int limit, int offset) {
        List<ReportRecord> out = new ArrayList<>();
        String sql = "SELECT id, reporter, target, target_name, reason, created_at, status, assigned_to_uuid, assigned_to_name, assigned_at, staff_note, resolved_at " +
                "FROM reports ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReportRecord(
                            rs.getLong("id"),
                            rs.getString("reporter"),
                            rs.getString("target"),
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("created_at"),
                            rs.getString("status"),
                            rs.getString("assigned_to_uuid"),
                            rs.getString("assigned_to_name"),
                            (Long) rs.getObject("assigned_at"),
                            rs.getString("staff_note"),
                            (Long) rs.getObject("resolved_at")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return out;
    }

    public List<ReportRecord> listRecentReportsFiltered(int limit, int offset, String filter) {
        String f = filter == null ? "ALL" : filter;
        if ("ALL".equalsIgnoreCase(f)) return listRecentReports(limit, offset);

        List<ReportRecord> out = new ArrayList<>();
        String sql = "SELECT id, reporter, target, target_name, reason, created_at, status, assigned_to_uuid, assigned_to_name, assigned_at, staff_note, resolved_at " +
                "FROM reports WHERE status = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.toUpperCase());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ReportRecord(
                            rs.getLong("id"),
                            rs.getString("reporter"),
                            rs.getString("target"),
                            rs.getString("target_name"),
                            rs.getString("reason"),
                            rs.getLong("created_at"),
                            rs.getString("status"),
                            rs.getString("assigned_to_uuid"),
                            rs.getString("assigned_to_name"),
                            (Long) rs.getObject("assigned_at"),
                            rs.getString("staff_note"),
                            (Long) rs.getObject("resolved_at")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return out;
    }

    public ReportRecord getReportById(long id) {
        String sql = "SELECT id, reporter, target, target_name, reason, created_at, status, assigned_to_uuid, assigned_to_name, assigned_at, staff_note, resolved_at " +
                "FROM reports WHERE id = ? LIMIT 1";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new ReportRecord(
                        rs.getLong("id"),
                        rs.getString("reporter"),
                        rs.getString("target"),
                        rs.getString("target_name"),
                        rs.getString("reason"),
                        rs.getLong("created_at"),
                        rs.getString("status"),
                        rs.getString("assigned_to_uuid"),
                        rs.getString("assigned_to_name"),
                        (Long) rs.getObject("assigned_at"),
                        rs.getString("staff_note"),
                        (Long) rs.getObject("resolved_at")
                );
            }
        } catch (SQLException ignored) {
            return null;
        }
    }

    public void deleteReportById(long id) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public boolean claimReport(long id, String adminUuid, String adminName, long now) {
        String sql = "UPDATE reports SET status='CLAIMED', assigned_to_uuid=?, assigned_to_name=?, assigned_at=? WHERE id=? AND status='OPEN'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, adminUuid);
            ps.setString(2, adminName);
            ps.setLong(3, now);
            ps.setLong(4, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    public boolean resolveReport(long id, long now) {
        String sql = "UPDATE reports SET status='RESOLVED', resolved_at=? WHERE id=? AND status!='RESOLVED'";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, now);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    public boolean setStaffNote(long id, String note) {
        String sql = "UPDATE reports SET staff_note=? WHERE id=?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (note == null || note.trim().isEmpty()) {
                ps.setNull(1, Types.VARCHAR);
            } else {
                ps.setString(1, note);
            }
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }
}
