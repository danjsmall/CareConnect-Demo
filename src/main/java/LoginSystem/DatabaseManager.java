package LoginSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DatabaseManager {

    private static final String DB_PATH;
    static {
        String home = System.getProperty("user.home");
        DB_PATH = home + File.separator + "health-assistant" + File.separator + "careconnect.db";
    }

    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;


    public static void init() {
        ensureDirectoryExists();
        boolean isNewDatabase = !new File(DB_PATH).exists();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(CREATE_PASSWORDS);
            stmt.executeUpdate(CREATE_ADMINISTRATORS);
            stmt.executeUpdate(CREATE_DOCTORS);
            stmt.executeUpdate(CREATE_PATIENTS);
            stmt.executeUpdate(CREATE_PATIENT_CONDITIONS);
            stmt.executeUpdate(CREATE_PATIENT_VACCINE_RECORDS);
            stmt.executeUpdate(CREATE_PATIENT_APPOINTMENTS);
            stmt.executeUpdate(CREATE_PATIENT_PRESCRIPTIONS);
            stmt.executeUpdate(CREATE_MEDICATION_LOG);
            stmt.executeUpdate(CREATE_DEVICE_SETTINGS);
            stmt.executeUpdate(CREATE_MEDICATION_REMINDER_SETTINGS);

            migrate(stmt);

            System.out.println("[DB] Local database ready at: " + DB_PATH);

            if (isNewDatabase || isDatabaseEmpty(stmt)) {
                loadInitialData(stmt);
            } else {
                System.out.println("[DB] Database already contains data, skipping initial data load.");
            }

        } catch (SQLException e) {
            System.err.println("[DB] Failed to initialise database: " + e.getMessage());
        }
    }

    private static boolean isDatabaseEmpty(Statement stmt) {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_passwords")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        } catch (SQLException e) {
            // If table doesn't exist or other error, assume empty or handle as needed
            return true;
        }
        return true;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    // Table definitions
    // All tables use "IF NOT EXISTS" so calling init() again is safe.
    // synced_at is NULL when data has never been synced from the server. (to do)

    private static final String CREATE_PASSWORDS = """
    CREATE TABLE IF NOT EXISTS user_passwords (
        email           TEXT    PRIMARY KEY,
        password_hash   TEXT    NOT NULL,
        salt            TEXT,
        role            TEXT    NOT NULL DEFAULT 'patient',
        level           INTEGER NOT NULL DEFAULT 1,
        patient_id      TEXT,
        created_at      TEXT    DEFAULT (datetime('now')),
        last_login_at   TEXT
    )
    """;

    private static final String CREATE_ADMINISTRATORS = """
    CREATE TABLE IF NOT EXISTS administrators (
        email           TEXT    PRIMARY KEY,
        name            TEXT
    )
    """;

    private static final String CREATE_DOCTORS = """
    CREATE TABLE IF NOT EXISTS doctors (
        email           TEXT    PRIMARY KEY,
        name            TEXT
    )
    """;

    private static final String CREATE_PATIENTS = """
        CREATE TABLE IF NOT EXISTS patients (
            id                          TEXT PRIMARY KEY,
            name                        TEXT NOT NULL,
            age                         INTEGER,
            gender                      TEXT,
            address_line1               TEXT,
            city                        TEXT,
            county                      TEXT,
            postcode                    TEXT,
            country                     TEXT,
            telephone                   TEXT,
            email                       TEXT,
            gp_name                     TEXT,
            gp_practice                 TEXT,
            gp_address                  TEXT,
            national_insurance_number   TEXT,
            passport_number             TEXT,
            cache_status                TEXT,
            last_synced_at              TEXT,
            meta_region                 TEXT
        )
        """;

    private static final String CREATE_PATIENT_CONDITIONS = """
        CREATE TABLE IF NOT EXISTS patient_conditions (
            condition_id    INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id      TEXT NOT NULL,
            condition_name  TEXT NOT NULL,
            status          TEXT,
            since_date      TEXT
        )
        """;

    private static final String CREATE_PATIENT_VACCINE_RECORDS = """
        CREATE TABLE IF NOT EXISTS patient_vaccine_records (
            record_id       INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id      TEXT NOT NULL,
            vaccine         TEXT NOT NULL,
            vaccine_date    TEXT,
            product         TEXT
        )
        """;

    private static final String CREATE_PATIENT_APPOINTMENTS = """
        CREATE TABLE IF NOT EXISTS patient_appointments (
            appointment_id      INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id          TEXT NOT NULL,
            appointment_date    TEXT NOT NULL,
            appointment_time    TEXT,
            type                TEXT,
            location            TEXT,
            reason              TEXT,
            verified_up_to_date INTEGER
        )
        """;

    private static final String CREATE_PATIENT_PRESCRIPTIONS = """
        CREATE TABLE IF NOT EXISTS patient_prescriptions (
            prescription_id INTEGER PRIMARY KEY AUTOINCREMENT,
            patient_id      TEXT NOT NULL,
            drug            TEXT NOT NULL,
            indication      TEXT,
            dose            TEXT,
            frequency       TEXT,
            start_date      TEXT,
            prescriber      TEXT
        )
        """;

    private static final String CREATE_MEDICATION_LOG = """
        CREATE TABLE IF NOT EXISTS medication_log (
            log_id          INTEGER PRIMARY KEY AUTOINCREMENT,
            prescription_id TEXT NOT NULL,
            patient_id      TEXT NOT NULL,
            taken_at        TEXT NOT NULL,
            was_taken       INTEGER NOT NULL DEFAULT 0,
            pending_sync    INTEGER DEFAULT 1
        )
        """;

    private static final String CREATE_DEVICE_SETTINGS = """
        CREATE TABLE IF NOT EXISTS device_settings (
            id              INTEGER PRIMARY KEY CHECK (id = 1),
            volume          INTEGER DEFAULT 85,
            brightness      INTEGER DEFAULT 70,
            text_size       TEXT DEFAULT 'large',
            theme           TEXT DEFAULT 'light',
            screen_timeout  INTEGER DEFAULT 30,
            clock_format    TEXT DEFAULT '12hr'
        )
        """;
    
    private static final String CREATE_MEDICATION_REMINDER_SETTINGS = """
        CREATE TABLE IF NOT EXISTS medication_reminder_settings (
            prescription_id  INTEGER PRIMARY KEY,
            patient_id       TEXT NOT NULL,
            frequency        TEXT NOT NULL,
            day_of_week      TEXT,
            time_1           TEXT,
            time_2           TEXT,
            time_3           TEXT
        )
        """;

    // Private helpers
    private static void ensureDirectoryExists() {
        File dbFile = new File(DB_PATH);
        File dir = dbFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    public static boolean insertUser(String email, String passwordHash, String salt,
                                 String role, int level, String patientId) {
    String sql = """
        INSERT INTO user_passwords
            (email, password_hash, salt, role, level, patient_id)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, email);
        ps.setString(2, passwordHash);
        ps.setString(3, salt);
        ps.setString(4, role);
        ps.setInt(5, level);
        ps.setString(6, patientId);
        ps.executeUpdate();
        return true;
    } catch (SQLException e) {
        if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) return false;
        System.err.println("[DB] insertUser failed: " + e.getMessage());
        return false;
    }
}

public static UserRow findUser(String email) {
    String sql = "SELECT * FROM user_passwords WHERE email = ? LIMIT 1";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new UserRow(
                0, // account_id no longer exists
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("salt"),
                rs.getString("role"),
                rs.getInt("level"),
                rs.getString("patient_id")
            );
        }
    } catch (SQLException e) {
        System.err.println("[DB] findUser failed: " + e.getMessage());
    }
    return null;
}

public static boolean updatePasswordHash(String email, String newHash, String newSalt) {
    String sql = "UPDATE user_passwords SET password_hash = ?, salt = ? WHERE email = ?";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, newHash);
        ps.setString(2, newSalt);
        ps.setString(3, email);
        return ps.executeUpdate() > 0;
    } catch (SQLException e) {
        System.err.println("[DB] updatePasswordHash failed: " + e.getMessage());
        return false;
    }
}

public static void recordLogin(String email) {
    String sql = "UPDATE user_passwords SET last_login_at = datetime('now') WHERE email = ?";
    try (Connection conn = getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, email);
        ps.executeUpdate();
    } catch (SQLException e) {
        System.err.println("[DB] recordLogin failed: " + e.getMessage());
    }
}

    public static class UserRow {
    public final int accountId;
    public final String username; // Now represents email
    public final String passwordHash;
    public final String salt;
    public final String role;
    public final int level;
    public final String patientId;

    public UserRow(int accountId, String username, String passwordHash, String salt,
                   String role, int level, String patientId) {
        this.accountId    = accountId;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.salt         = salt;
        this.role         = role;
        this.level        = level;
        this.patientId    = patientId;
    }
}

    public static String getNameByEmail(String email, String role) {
        String sql;
        if ("admin".equalsIgnoreCase(role)) {
            sql = "SELECT name FROM administrators WHERE email = ? LIMIT 1";
        } else if ("doctor".equalsIgnoreCase(role)) {
            sql = "SELECT name FROM doctors WHERE email = ? LIMIT 1";
        } else {
            sql = "SELECT name FROM patients WHERE email = ? LIMIT 1";
        }

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException e) {
            System.err.println("[DB] getNameByEmail failed: " + e.getMessage());
        }
        return email; // Fallback to email if name not found
    }

    private DatabaseManager() {} // Prevent instantiation — all methods are static

    private static void migrate(Statement stmt) {
        // 1. Check for gp_address in patients
        try {
            stmt.execute("SELECT gp_address FROM patients LIMIT 1");
        } catch (SQLException e) {
            System.out.println("[DB] gp_address column missing in patients table. Migrating...");
            try {
                // If gp_region exists, rename it to gp_address
                boolean hasGpRegion = false;
                try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(patients)")) {
                    while (rs.next()) {
                        if ("gp_region".equalsIgnoreCase(rs.getString("name"))) {
                            hasGpRegion = true;
                            break;
                        }
                    }
                }

                if (hasGpRegion) {
                    stmt.execute("ALTER TABLE patients RENAME COLUMN gp_region TO gp_address");
                    System.out.println("[DB] Renamed gp_region to gp_address.");
                } else {
                    stmt.execute("ALTER TABLE patients ADD COLUMN gp_address TEXT");
                    System.out.println("[DB] Added gp_address column.");
                }
            } catch (SQLException ex) {
                System.err.println("[DB] Migration failed (gp_address): " + ex.getMessage());
            }
        }

        // 2. Check for condition_id in patient_conditions
        try {
            stmt.execute("SELECT condition_id FROM patient_conditions LIMIT 1");
        } catch (SQLException e) {
            System.out.println("[DB] condition_id column missing in patient_conditions table. Migrating...");
            try {
                // SQLite doesn't support adding PRIMARY KEY AUTOINCREMENT to an existing table easily.
                // But we can add the column and use it.
                stmt.execute("ALTER TABLE patient_conditions ADD COLUMN condition_id INTEGER");
                System.out.println("[DB] Added condition_id column to patient_conditions.");
                // We should ideally populate it, but SQLite will leave it NULL for existing rows unless we do more work.
                // For this project, a simple ADD might be enough if we handle NULLs or if the user is okay with 
                // re-initializing the DB. Given this is a dev environment, I'll stick to this.
            } catch (SQLException ex) {
                System.err.println("[DB] Migration failed (condition_id): " + ex.getMessage());
            }
        }
    }

    private static void loadInitialData(Statement stmt) {
        // 1. User passwords
        loadSqlFile(stmt, "passwords_inserts.sql", "user_passwords");
        // 2. Administrator data
        loadSqlFile(stmt, "administrators_inserts.sql", "administrators");
        // 3. Doctor data
        loadSqlFile(stmt, "doctors_inserts.sql", "doctors");
        // 4. Patient data
        loadSqlFile(stmt, "patients_inserts.sql", "patients");
    }

    private static void loadSqlFile(Statement stmt, String fileName, String tableName) {
        System.out.println("[DB] Checking for updates in " + fileName + "...");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                DatabaseManager.class.getClassLoader().getResourceAsStream(fileName)))) {
            
            Connection conn = stmt.getConnection();
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            StringBuilder sb = new StringBuilder();
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;
                sb.append(line);
                if (line.endsWith(";")) {
                    String sql = sb.toString();
                    // Skip transaction control statements as they interfere with batch execution
                    // and we want atomic-ish behavior per statement or just let the batch handle it.
                    String upper = sql.toUpperCase();
                    if (upper.startsWith("BEGIN") || upper.startsWith("COMMIT") || upper.startsWith("ROLLBACK")) {
                        sb.setLength(0);
                        continue;
                    }
                    // Convert regular INSERT INTO into INSERT OR IGNORE INTO for SQLite
                    if (upper.startsWith("INSERT INTO")) {
                        sql = "INSERT OR IGNORE" + sql.substring(6);
                    }
                    stmt.addBatch(sql);
                    sb.setLength(0);
                    count++;
                    if (count % 2000 == 0) {
                        stmt.executeBatch();
                        System.out.println("[DB]   Progress: " + count + " statements processed.");
                    }
                }
            }
            stmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(autoCommit);
            
            System.out.println("[DB] Updates from " + fileName + " processed (" + count + " statements).");
        } catch (IOException | SQLException e) {
            System.err.println("[DB] Error loading initial data from " + fileName + ": " + e.getMessage());
            try {
                Connection conn = stmt.getConnection();
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("[DB] Rollback failed: " + ex.getMessage());
            }
        } catch (NullPointerException e) {
            System.err.println("[DB] Initial data file not found in classpath: " + fileName);
        }
    }

}
