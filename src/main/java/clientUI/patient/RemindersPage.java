package clientUI.patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import LoginSystem.DatabaseManager;
import clientUI.DashboardDataService;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class RemindersPage {
    private MainPage parent;
    private String username;
    private String patientId;

    private TextArea reminderArea;

    public RemindersPage(MainPage parent, String username, int level) {
        this.parent = parent;
        this.username = username;
        this.patientId = loadPatientIdFromAccount(username);
    }

    public void start(Stage stage) {
        stage.setTitle("Medication & Appointment Reminders");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f4f4;");

        Label title = new Label("Reminders Page - " + username);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 20, 10, 20));
        root.setTop(title);

        reminderArea = new TextArea();
        reminderArea.setEditable(false);
        reminderArea.setWrapText(false);
        reminderArea.setStyle(
            "-fx-font-family: 'Consolas';" +
            "-fx-font-size: 14px;" +
            "-fx-control-inner-background: white;" +
            "-fx-border-color: #dddddd;"
        );
        BorderPane.setMargin(reminderArea, new Insets(10, 20, 10, 20));
        root.setCenter(reminderArea);

        Button refreshButton = new Button("Refresh");
        Button backButton = new Button("Back");

        refreshButton.setOnAction(e -> {
            patientId = loadPatientIdFromAccount(username);
            loadRemindersFromDb();
        });

        backButton.setOnAction(e -> parent.start(stage));

        HBox buttonBox = new HBox(10, refreshButton, backButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 20, 20, 20));
        root.setBottom(buttonBox);

        loadRemindersFromDb();

        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }

    private String loadPatientIdFromAccount(String username) {
        if (username == null || username.isBlank()) {
            return "unknown";
        }

        String trimmed = username.trim();

        if (trimmed.toLowerCase().startsWith("pt-")) {
            return trimmed;
        }

        DatabaseManager.UserRow row = DatabaseManager.findUser(trimmed);
        if (row == null || row.patientId == null || row.patientId.isBlank()) {
            return "unknown";
        }

        return row.patientId;
    }

    private void loadRemindersFromDb() {
        StringBuilder output = new StringBuilder();

        output.append("Identifier : ").append(username).append("\n");
        output.append("Patient ID : ").append(patientId).append("\n");
        output.append("============================================\n\n");

        if ("unknown".equals(patientId)) {
            output.append("No patient is linked to this account yet.\n");
            reminderArea.setText(output.toString());
            return;
        }

        List<DashboardDataService.AppointmentItem> appointments = DashboardDataService.getAppointments(username);
        ArrayList<DbMedication> medications = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection()) {
            medications = loadMedications(conn, patientId);
        } catch (SQLException e) {
            output.append("Error reading database: ").append(e.getMessage());
            reminderArea.setText(output.toString());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        output.append("Upcoming Appointments\n");
        output.append("--------------------------------------------\n");


        boolean foundAppointment = false;

        for (DashboardDataService.AppointmentItem appt : appointments) {
            LocalDateTime dateTime = parseDateTime(appt.date(), appt.appointmentTime());
            long minutesUntil = Duration.between(now, dateTime).toMinutes();

            if (minutesUntil < 0) {
                continue;
            }

            foundAppointment = true;

            if (minutesUntil <= 15) {
                output.append("[REMINDER] Appointment soon!\n");
            } else {
                output.append("[UPCOMING]\n");
            }

            output.append("ID       : ").append(appt.appointmentId()).append("\n");
            output.append("Type     : ").append(appt.type()).append("\n");
            output.append("Time     : ").append(dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
            output.append("Location : ").append(appt.location()).append("\n");
            output.append("Reason   : ").append(appt.reason()).append("\n");
            output.append("--------------------------------------------\n");
        }

        if (!foundAppointment) {
            output.append("No upcoming appointments found.\n");
            output.append("--------------------------------------------\n");
        }

        output.append("\nMedication Reminders\n");
        output.append("--------------------------------------------\n");

        boolean foundMedication = false;

        for (DbMedication med : medications) {
            foundMedication = true;

            output.append(med.drug)
                  .append(" | ")
                  .append(med.dose)
                  .append(" | ")
                  .append(med.frequency)
                  .append("\n");

            output.append("Reminder schedule: ")
                  .append(buildReminderText(med))
                  .append("\n");

            if (isMedicationDueNow(med, now)) {
                output.append("[TAKE NOW]\n");
            }

            output.append("--------------------------------------------\n");
        }

        if (!foundMedication) {
            output.append("No medication reminders available.\n");
        }

        reminderArea.setText(output.toString());
    }

    private ArrayList<DbMedication> loadMedications(Connection conn, String patientId) throws SQLException {
        ArrayList<DbMedication> results = new ArrayList<>();

        String sql = """
            SELECT p.prescription_id, p.drug, p.dose, p.frequency,
                    r.day_of_week, r.time_1, r.time_2, r.time_3
            FROM patient_prescriptions p
            LEFT JOIN medication_reminder_settings r
                ON p.prescription_id = r.prescription_id
            WHERE p.patient_id = ?
            ORDER BY p.drug
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String drug = rs.getString("drug");
                String dose = rs.getString("dose");
                String frequency = rs.getString("frequency");
                String dayOfWeek = rs.getString("day_of_week");
                String time1 = rs.getString("time_1");
                String time2 = rs.getString("time_2");
                String time3 = rs.getString("time_3");

                results.add(new DbMedication(   
                    drug,
                    dose,
                    frequency,
                    dayOfWeek,
                    time1,
                    time2,
                    time3  
                ));
            }
        }

        return results;
    }

    private LocalDateTime parseDateTime(String date, String time) {
        if (time == null || time.isBlank()) {
            return LocalDate.parse(date).atStartOfDay();
        }

        try {
            return LocalDateTime.parse(date + "T" + time);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(date + "T" + time + ":00");
            } catch (Exception ex) {
                return LocalDate.parse(date).atStartOfDay();
            }
        }
    }

    private String buildReminderText(DbMedication med) {
        if ("as needed".equalsIgnoreCase(med.frequency)) {
            return "No fixed reminder time";
        }

        if ("once weekly".equalsIgnoreCase(med.frequency)) {
            return (med.dayOfWeek == null ? "Weekly" : med.dayOfWeek) +
                   (med.time1 == null ? "" : " at " + med.time1);
        }

        ArrayList<String> times = new ArrayList<>();
        if (med.time1 != null && !med.time1.isBlank()) times.add(med.time1);
        if (med.time2 != null && !med.time2.isBlank()) times.add(med.time2);
        if (med.time3 != null && !med.time3.isBlank()) times.add(med.time3);

        if (times.isEmpty()) {
            return "No reminder time set";
        }

        return String.join(", ", times);
    }

    private boolean isMedicationDueNow(DbMedication med, LocalDateTime now) {
        if ("as needed".equalsIgnoreCase(med.frequency)) {
            return false;
        }

        LocalTime currentTime = now.toLocalTime().withSecond(0).withNano(0);

        if ("once weekly".equalsIgnoreCase(med.frequency)) {
            if (med.dayOfWeek == null || med.time1 == null || med.time1.isBlank()) {
                return false;
            }
            if (!now.getDayOfWeek().name().equalsIgnoreCase(med.dayOfWeek)) {
                return false;
            }
            return matchesTime(currentTime, med.time1);
        }

        return matchesTime(currentTime, med.time1)
            || matchesTime(currentTime, med.time2)
            || matchesTime(currentTime, med.time3);
    }

    private boolean matchesTime(LocalTime currentTime, String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return false;
        }
        try {
            LocalTime scheduled = LocalTime.parse(rawTime);
            return Math.abs(Duration.between(currentTime, scheduled).toMinutes()) <= 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static class DbMedication {
        String drug;
        String dose;
        String frequency;
        String dayOfWeek;
        String time1;
        String time2;
        String time3;

        DbMedication(String drug,
            String dose,
            String frequency,
            String dayOfWeek,
            String time1,
            String time2,
            String time3
        ) {
            this.drug = drug;
            this.dose = dose;
            this.frequency = frequency;
            this.dayOfWeek = dayOfWeek;
            this.time1 = time1;
            this.time2 = time2;
            this.time3 = time3;
        }
    }
}