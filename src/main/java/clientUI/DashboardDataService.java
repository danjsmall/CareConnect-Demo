package clientUI;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import APIhandlers.OpenWeatherMap;
import LoginSystem.DatabaseManager;

public final class DashboardDataService {
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DashboardDataService() {
    }

    public static Optional<PatientSummary> getPatientSummary(String username) {
        String sql = """
            SELECT id, name, age, address_line1, city, postcode, gp_name, gp_practice, gp_address, telephone, email
            FROM patients
            WHERE LOWER(name) = LOWER(?) OR LOWER(id) = LOWER(?) OR LOWER(email) = LOWER(?)
            LIMIT 1
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                String address = joinParts(joinParts(rs.getString("address_line1"), rs.getString("city")), rs.getString("postcode"));
                return Optional.of(new PatientSummary(
                    safe(rs.getString("id")),
                    safe(rs.getString("name")),
                    rs.getInt("age"),
                    address,
                    safe(rs.getString("gp_name")),
                    safe(rs.getString("gp_practice")),
                    safe(rs.getString("gp_address")),
                    safe(rs.getString("telephone")),
                    safe(rs.getString("email"))
                ));
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

public static List<MedicationItem> getMedications(String username) {
    Optional<String> patientId = resolvePatientId(username);
    if (patientId.isEmpty()) {
        return List.of();
    }

    String sql = """
        SELECT DISTINCT p.prescription_id,
               p.patient_id,
               p.drug,
               p.dose,
               p.frequency,
               r.day_of_week,
               r.time_1,
               r.time_2,
               r.time_3,
               COALESCE((
                   SELECT ml.was_taken
                   FROM medication_log ml
                   WHERE CAST(ml.prescription_id AS INTEGER) = p.prescription_id
                     AND ml.patient_id = p.patient_id
                   ORDER BY datetime(ml.taken_at) DESC
                   LIMIT 1
               ), 0) AS was_taken
        FROM patient_prescriptions p
        LEFT JOIN medication_reminder_settings r
          ON p.prescription_id = r.prescription_id
         AND p.patient_id = r.patient_id
        WHERE p.patient_id = ?
        ORDER BY p.prescription_id DESC
        """;

    List<MedicationItem> medications = new ArrayList<>();
    try (Connection conn = DatabaseManager.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, patientId.get());

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String frequency = safe(rs.getString("frequency"));
                String dayOfWeek = cleanNullable(rs.getString("day_of_week"));
                String time1 = cleanNullable(rs.getString("time_1"));
                String time2 = cleanNullable(rs.getString("time_2"));
                String time3 = cleanNullable(rs.getString("time_3"));

                String timeHint = buildMedicationTimeHint(frequency, dayOfWeek, time1, time2, time3);

                medications.add(new MedicationItem(
                    rs.getInt("prescription_id"),
                    rs.getString("patient_id"),
                    safe(rs.getString("drug")),
                    safe(rs.getString("dose")),
                    frequency,
                    timeHint,
                    rs.getInt("was_taken") == 1
                ));
            }
        }
    } catch (SQLException e) {
        return List.of();
    }
    return medications;
}

    public static Optional<AppointmentItem> getNextAppointment(String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return Optional.empty();
        }

        String sql = """
            SELECT appointment_id, appointment_date, appointment_time, type, location, reason
            FROM patient_appointments
            WHERE patient_id = ?
                AND datetime(appointment_date || ' ' || COALESCE(appointment_time, '00:00')) >= datetime('now')
            ORDER BY datetime(appointment_date || ' ' || COALESCE(appointment_time, '00:00')) ASC
            LIMIT 1
            """;

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patientId.get());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                AppointmentItem item = new AppointmentItem(
                    rs.getInt("appointment_id"),
                    safe(rs.getString("type")),
                    safe(rs.getString("location")),
                    formatAppointmentTime(
                        rs.getString("appointment_date"),
                        rs.getString("appointment_time")
                    ),
                    safe(rs.getString("appointment_date")),
                    cleanNullable(rs.getString("appointment_time")),
                    safe(rs.getString("reason"))
                );

                return Optional.of(item);
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public static List<AppointmentItem> getAppointments(String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT appointment_id, appointment_date, appointment_time, type, location, reason
            FROM patient_appointments
            WHERE patient_id = ?
            ORDER BY datetime(appointment_date || ' ' || COALESCE(appointment_time, '00:00')) ASC
            """;

        List<AppointmentItem> appointments = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, patientId.get());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    appointments.add(new AppointmentItem(
                        rs.getInt("appointment_id"),
                        safe(rs.getString("type")),
                        safe(rs.getString("location")),
                        formatAppointmentTime(
                            rs.getString("appointment_date"),
                            rs.getString("appointment_time")
                        ),
                        safe(rs.getString("appointment_date")),
                        cleanNullable(rs.getString("appointment_time")),
                        safe(rs.getString("reason"))
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }

        return appointments;
    }

    public static boolean addAppointment(
        String username,
        String appointmentDate,
        String appointmentTime,
        String type,
        String location,
        String reason
    ) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return false;
        }

        String sql = """
            INSERT INTO patient_appointments
                (patient_id, appointment_date, appointment_time, type, location, reason, verified_up_to_date)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId.get());
            stmt.setString(2, appointmentDate);
            stmt.setString(3, appointmentTime == null || appointmentTime.isBlank() ? null : appointmentTime);
            stmt.setString(4, type == null || type.isBlank() ? "Appointment" : type);
            stmt.setString(5, location == null || location.isBlank() ? null : location);
            stmt.setString(6, reason == null || reason.isBlank() ? null : reason);
            stmt.setInt(7, 1);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean deleteAppointment(Integer appointmentId, String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty() || appointmentId == null) {
            return false;
        }

        String sql = """
            DELETE FROM patient_appointments
            WHERE appointment_id = ? AND patient_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, appointmentId);
            stmt.setString(2, patientId.get());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean updateAppointment(
        Integer appointmentId,
        String username,
        String appointmentDate,
        String appointmentTime,
        String type,
        String location,
        String reason
    ) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty() || appointmentId == null) {
            return false;
        }

        String sql = """
            UPDATE patient_appointments
            SET appointment_date = ?,
                appointment_time = ?,
                type = ?,
                location = ?,
                reason = ?,
                verified_up_to_date = 1
            WHERE appointment_id = ? AND patient_id = ?
            """;

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, appointmentDate);
            stmt.setString(2, appointmentTime == null || appointmentTime.isBlank() ? null : appointmentTime);
            stmt.setString(3, type == null || type.isBlank() ? "Appointment" : type);
            stmt.setString(4, location == null || location.isBlank() ? null : location);
            stmt.setString(5, reason == null || reason.isBlank() ? null : reason);
            stmt.setInt(6, appointmentId);
            stmt.setString(7, patientId.get());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<ConditionItem> getConditions(String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT DISTINCT condition_name, status, since_date
            FROM patient_conditions
            WHERE patient_id = ?
            ORDER BY since_date DESC
            LIMIT 3
            """;

        List<ConditionItem> conditions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId.get());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    conditions.add(new ConditionItem(
                        safe(rs.getString("condition_name")),
                        cleanNullable(rs.getString("status")),
                        cleanNullable(rs.getString("since_date"))
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return conditions;
    }

    public static Optional<VaccineItem> getLatestVaccine(String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return Optional.empty();
        }

        String sql = """
            SELECT DISTINCT vaccine, vaccine_date, product
            FROM patient_vaccine_records
            WHERE patient_id = ?
            ORDER BY vaccine_date DESC
            LIMIT 1
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId.get());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new VaccineItem(
                    safe(rs.getString("vaccine")),
                    safe(rs.getString("vaccine_date")),
                    safe(rs.getString("product"))
                ));
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public static Optional<WeatherItem> getCurrentWeather(String username) {
        Optional<DashboardDataService.PatientSummary> summary = getPatientSummary(username);
        if (summary.isEmpty()) {
            return Optional.empty();
        }

        try {
            String fullAddress = summary.get().address();
            if (fullAddress == null || fullAddress.isBlank() || fullAddress.equals("Not recorded")) {
                return Optional.empty();
            }

            // Extract city from the address (last part after the last comma)
            String city = extractCityFromAddress(fullAddress);
            if (city == null || city.isBlank()) {
                return Optional.empty();
            }

            Object[] coords = OpenWeatherMap.getCoords(city);
            double lat = (double) coords[0];
            double lon = (double) coords[1];
            String locationName = (String) coords[2];

            OpenWeatherMap owm = new OpenWeatherMap();
            Map<String, Object> weather = owm.getWeather(lat, lon);
            
            Float temp = (Float) weather.get("temperature");
            String condition = (String) weather.get("conditionTitle");
            Integer windSpeed = (Integer) weather.get("windSpeed");
            Integer humidity = (Integer) weather.get("humidity");
            
            return Optional.of(new WeatherItem(
                temp,
                condition != null ? condition : "Unknown",
                windSpeed != null ? windSpeed : 0,
                humidity != null ? humidity : 0,
                locationName
            ));
        } catch (Exception e) {
            System.err.println("Failed to get weather data: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private static String extractCityFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }

        // Split by comma and take the last non-empty part (should be the city)
        String[] parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (!part.isBlank()) {
                // If it contains a postcode pattern, skip it and get the previous part
                if (part.matches(".*\\b[A-Z]{1,2}\\d{1,2}[A-Z]?\\s?\\d[A-Z]{2}\\b.*")) {
                    continue; // Skip postcode
                }
                return part;
            }
        }
        return null;
    }

    public static SettingsData getSettings() {
        String sql = """
            SELECT volume, brightness, text_size, theme, screen_timeout, clock_format
            FROM device_settings
            WHERE id = 1
            """;

        try (Connection conn = DatabaseManager.getConnection();
             Statement insert = conn.createStatement()) {
            insert.executeUpdate("INSERT OR IGNORE INTO device_settings (id) VALUES (1)");
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new SettingsData(
                        rs.getInt("volume"),
                        rs.getInt("brightness"),
                        safe(rs.getString("text_size")),
                        safe(rs.getString("theme")),
                        rs.getInt("screen_timeout"),
                        safe(rs.getString("clock_format"))
                    );
                }
            }
        } catch (SQLException e) {
            return new SettingsData(85, 70, "large", "light", 30, "12hr");
        }

        return new SettingsData(85, 70, "large", "light", 30, "12hr");
    }

    public static void saveSettings(SettingsData settings) {
        String sql = """
            INSERT INTO device_settings (id, volume, brightness, text_size, theme, screen_timeout, clock_format)
            VALUES (1, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                volume = excluded.volume,
                brightness = excluded.brightness,
                text_size = excluded.text_size,
                theme = excluded.theme,
                screen_timeout = excluded.screen_timeout,
                clock_format = excluded.clock_format
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, settings.volume());
            stmt.setInt(2, settings.brightness());
            stmt.setString(3, settings.textSize());
            stmt.setString(4, settings.theme());
            stmt.setInt(5, settings.screenTimeout());
            stmt.setString(6, settings.clockFormat());
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public static boolean addMedication(
        String username,
        String name,
        String dose,
        String frequency,
        String dayOfWeek,
        String time1,
        String time2,
        String time3
    ) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return false;
        }

        String sql = """
            INSERT INTO patient_prescriptions (patient_id, drug, dose, frequency)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, patientId.get());
            stmt.setString(2, name);
            stmt.setString(3, dose);
            stmt.setString(4, frequency);
            stmt.executeUpdate();

            Integer prescriptionId = null;
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    prescriptionId = keys.getInt(1);
                }
            }
            if (prescriptionId != null) {
                saveMedicationReminderSettings(
                    prescriptionId,
                    patientId.get(),
                    frequency,
                    dayOfWeek,
                    time1,
                    time2,
                    time3);
            }
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void saveMedicationReminderSettings(
        int prescriptionId,
        String patientId,
        String frequency,
        String dayOfWeek,
        String time1,
        String time2,
        String time3
    ) {
        String sql = """
            INSERT INTO medication_reminder_settings
                (prescription_id, patient_id, frequency, day_of_week, time_1, time_2, time_3)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(prescription_id) DO UPDATE SET
                frequency = excluded.frequency,
                day_of_week = excluded.day_of_week,
                time_1 = excluded.time_1,
                time_2 = excluded.time_2,
                time_3 = excluded.time_3
            """;

        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prescriptionId);
            stmt.setString(2, patientId);
            stmt.setString(3, frequency);
            stmt.setString(4, dayOfWeek);
            stmt.setString(5, time1);
            stmt.setString(6, time2);
            stmt.setString(7, time3);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save reminder settings: " + e.getMessage());
        }
    }
    public record MedicationReminderSetting(
        Integer prescriptionId,
        String patientId,
        String frequency,
        String dayOfWeek,
        String time1,
        String time2,
        String time3
    ) {}

    public static List<MedicationReminderSetting> getMedicationReminderSettings(String username) {
        Optional<String> patientId = resolvePatientId(username);
        if (patientId.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT prescription_id, patient_id, frequency, day_of_week, time_1, time_2, time_3
            FROM medication_reminder_settings
            WHERE patient_id = ?
            ORDER BY prescription_id DESC
            """;

        List<MedicationReminderSetting> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, patientId.get());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new MedicationReminderSetting(
                        rs.getInt("prescription_id"),
                        rs.getString("patient_id"),
                        rs.getString("frequency"),
                        rs.getString("day_of_week"),
                        rs.getString("time_1"),
                        rs.getString("time_2"),
                        rs.getString("time_3")
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return results;
    }

    public static void logMedicationStatus(Integer prescriptionId, String patientId, boolean taken) {
        if (prescriptionId == null || patientId == null || patientId.isBlank()) {
            return;
        }

        String sql = """
            INSERT INTO medication_log (prescription_id, patient_id, taken_at, was_taken)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, String.valueOf(prescriptionId));
            stmt.setString(2, patientId);
            stmt.setString(3, LocalDateTime.now().format(LOG_TIME));
            stmt.setInt(4, taken ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to log medication status: " + e.getMessage());
        }
    }

    private static String buildMedicationTimeHint(
        String frequency,
        String dayOfWeek,
        String time1,
        String time2,
        String time3
    ) {
        List<String> times = new ArrayList<>();

        if (time1 != null &&!time1.isBlank()) times.add(formatDisplayTime(time1));
        if (time2 != null &&!time2.isBlank()) times.add(formatDisplayTime(time2));
        if (time3 != null &&!time3.isBlank()) times.add(formatDisplayTime(time3));

        String normalizedFrequency = cleanNullable(frequency).toLowerCase(Locale.ENGLISH);

        if ("once weekly".equals(normalizedFrequency)) {
            if (!dayOfWeek.isBlank() && !times.isEmpty()) {
                return toTitleCase(dayOfWeek) + " at " + times.get(0);
            }
            if (!dayOfWeek.isBlank()) {
                return toTitleCase(dayOfWeek);
            }
            if (!times.isEmpty()) {
                return times.get(0);
            }
            return "Weekly";
        }

        if ("as needed".equals(normalizedFrequency)) {
            return "As needed";
        }

        if (!times.isEmpty()) {
            return String.join(" / ", times);
        }

        return toTimeHint(frequency);
    }

    private static String formatDisplayTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return "";
        }

        try {
            java.time.LocalTime time = java.time.LocalTime.parse(rawTime.trim());
            return time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
        } catch (Exception e) {
            return rawTime;
        }
    }

    private static String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static String toTimeHint(String frequency) {
        String value = cleanNullable(frequency).toLowerCase(Locale.ENGLISH);
        return switch (value) {
            case "once daily" -> "9:00 AM";
            case "twice daily" -> "9:00 AM / 9:00 PM";
            case "at night" -> "9:00 PM";
            case "weekly" -> "Weekly";
            case "" -> "Time not set";
            default -> frequency;
        };
    }

    private static Optional<String> resolvePatientId(String username) {
        String sql = """
            SELECT id
            FROM patients
            WHERE LOWER(name) = LOWER(?) OR LOWER(id) = LOWER(?) OR LOWER(email) = LOWER(?)
            LIMIT 1
            """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("id"));
                }
            }
        } catch (SQLException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static String formatAppointmentTime(String date, String time) {
        String safeDate = cleanNullable(date);
        String safeTime = cleanNullable(time);
        if (safeDate.isBlank() && safeTime.isBlank()) {
            return "Schedule not set";
        }
        if (safeTime.isBlank()) {
            return safeDate;
        }
        if (safeDate.isBlank()) {
            return safeTime;
        }

        try {
            LocalDate appointmentDate = LocalDate.parse(safeDate);
            LocalTime appointmentTime = LocalTime.parse(safeTime);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime appointment = LocalDateTime.of(appointmentDate, appointmentTime);

            long daysDiff = ChronoUnit.DAYS.between(now.toLocalDate(), appointmentDate);

            String relative;
            if (daysDiff == 0) {
                relative = "Today";
            } else if (daysDiff == 1) {
                relative = "Tomorrow";
            } else if (daysDiff > 1) {
                relative = "In " + daysDiff + " days";
            } else {
                // Past, but assuming next is future
                relative = "In 0 days";
            }

            String timeStr = appointmentTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH));
            String dayOfWeek = appointmentDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH));
            int dayOfMonth = appointmentDate.getDayOfMonth();
            String month = appointmentDate.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH));

            return relative + " at " + timeStr + " on " + dayOfWeek + " the " + getOrdinal(dayOfMonth) + " of " + month;
        } catch (Exception e) {
            // Fallback to old format if parsing fails
            return safeDate + " at " + safeTime;
        }
    }

    private static String joinParts(String left, String right) {
        String safeLeft = cleanNullable(left);
        String safeRight = cleanNullable(right);
        if (safeLeft.isBlank()) {
            return safe(safeRight);
        }
        if (safeRight.isBlank()) {
            return safe(safeLeft);
        }
        return safeLeft + ", " + safeRight;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Not recorded" : value;
    }

    private static String cleanNullable(String value) {
        return value == null ? "" : value.trim();
    }

    public record PatientSummary(
        String patientId,
        String name,
        int age,
        String address,
        String gpName,
        String gpPractice,
        String gpAddress,
        String telephone,
        String email
    ) {
    }

    public record MedicationItem(
        Integer prescriptionId,
        String patientId,
        String name,
        String dose,
        String frequency,
        String timeHint,
        boolean taken
    ) {
    }

    public record AppointmentItem(
        Integer appointmentId,
        String type,
        String location,
        String time,
        String date,
        String appointmentTime,
        String reason
    ) {
    }

    public record ConditionItem(String name, String status, String sinceDate) {
    }

    public record VaccineItem(String name, String date, String product) {
    }

    public record WeatherItem(
        Float temperature,
        String condition,
        Integer windSpeed,
        Integer humidity,
        String location
    ) {
    }

    public record SettingsData(
        int volume,
        int brightness,
        String textSize,
        String theme,
        int screenTimeout,
        String clockFormat
    ) {
    }

    public static boolean deleteMedication(Integer prescriptionId, String patientId) {
        if (prescriptionId == null || patientId == null || patientId.isBlank()) {
            return false;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteLogs = conn.prepareStatement("""
                    DELETE FROM medication_log
                    WHERE prescription_id = ? AND patient_id = ?
                """);
                PreparedStatement deleteReminderSettings = conn.prepareStatement("""
                    DELETE FROM medication_reminder_settings
                    WHERE prescription_id = ? AND patient_id = ?
                """);
                PreparedStatement deletePrescription = conn.prepareStatement("""
                    DELETE FROM patient_prescriptions
                    WHERE prescription_id = ? AND patient_id = ?
                """)) {

                deleteLogs.setString(1, String.valueOf(prescriptionId));
                deleteLogs.setString(2, patientId);
                deleteLogs.executeUpdate();

                deleteReminderSettings.setInt(1, prescriptionId);
                deleteReminderSettings.setString(2, patientId);
                deleteReminderSettings.executeUpdate();

                deletePrescription.setInt(1, prescriptionId);
                deletePrescription.setString(2, patientId);
                int affected = deletePrescription.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static String getOrdinal(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1: return day + "st";
            case 2: return day + "nd";
            case 3: return day + "rd";
            default: return day + "th";
        }
    }
}
