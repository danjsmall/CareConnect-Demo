package clientUI.patient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import LoginSystem.DatabaseManager;

public class VoiceChatService {
    private static final DateTimeFormatter FRIENDLY_DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.UK);

    private final String username;
    private final PatientContextProvider contextProvider;
    private Intent lastIntent = Intent.NONE;

    public VoiceChatService(String username) {
        this(username, new DatabasePatientContextProvider(username));
    }

    VoiceChatService(String username, PatientContextProvider contextProvider) {
        this.username = username;
        this.contextProvider = contextProvider;
    }

    public String createWelcomeMessage() {
        PatientContext context = loadPatientContext();
        if (!context.found()) {
            if (!context.loadError().isBlank()) {
                return "Hello " + username + ". I could not open the local patient database just now. " + context.loadError();
            }
            return "Hello " + username + ". I can still help with general navigation, but I could not match your login to a patient record yet. "
                + "Try asking for a health summary after logging in with a patient name, patient ID, or linked email address.";
        }

        String nextAppointment = context.nextAppointment() == null
            ? "No future appointment is currently recorded."
            : "Your next appointment is " + formatAppointment(context.nextAppointment()) + ".";

        return "Hello " + context.name() + ". I am ready to help with your local health record. " + nextAppointment;
    }

    public String buildPatientSnapshot() {
        PatientContext context = loadPatientContext();
        if (!context.found()) {
            if (!context.loadError().isBlank()) {
                return "Patient data is temporarily unavailable.\n\n" + context.loadError();
            }
            return "No patient record linked to this username yet.\n\nLog in with a patient name, ID, or linked email to see a personalised summary.";
        }

        StringBuilder snapshot = new StringBuilder();
        snapshot.append(context.name()).append(" (").append(context.patientId()).append(")\n");
        snapshot.append("Age ").append(context.age()).append(", ").append(safe(context.gender())).append("\n");
        snapshot.append("GP: ").append(safe(context.gpName())).append("\n");
        snapshot.append("Practice: ").append(safe(context.gpPractice())).append("\n");
        snapshot.append("Conditions: ").append(context.conditions().isEmpty() ? "None recorded" : String.join(", ", context.conditionNames())).append("\n");
        snapshot.append("Prescriptions: ").append(context.prescriptions().isEmpty() ? "None recorded" : String.valueOf(context.prescriptions().size())).append("\n");
        snapshot.append("Vaccines: ").append(context.vaccines().isEmpty() ? "None recorded" : String.valueOf(context.vaccines().size())).append("\n");
        snapshot.append("Upcoming appointments: ").append(context.upcomingAppointments().size());
        return snapshot.toString();
    }

    public String generateReply(String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        PatientContext context = loadPatientContext();

        if (matchesEmergencyPrompt(normalizedPrompt)) {
            lastIntent = Intent.SAFETY;
            return buildSafetyReply();
        }

        if (!context.found()) {
            if (!context.loadError().isBlank()) {
                return context.loadError();
            }
            return "I could not find a patient record for '" + username + "'. You can ask general questions, but personalised health answers need a matching patient name, ID, or email.";
        }

        if (isFollowUpPrompt(normalizedPrompt)) {
            String followUp = buildFollowUpReply(context, normalizedPrompt);
            if (followUp != null) {
                return followUp;
            }
        }

        Intent intent = classifyIntent(normalizedPrompt);
        lastIntent = intent;

        return switch (intent) {
            case HELP -> buildHelpReply();
            case SUMMARY -> buildSummaryReply(context);
            case APPOINTMENT -> wantsExpandedList(normalizedPrompt)
                ? buildAppointmentListReply(context)
                : buildAppointmentReply(context);
            case PRESCRIPTION -> wantsExpandedList(normalizedPrompt)
                ? buildPrescriptionDetailReply(context)
                : buildPrescriptionReply(context);
            case VACCINE -> wantsExpandedList(normalizedPrompt)
                ? buildVaccineHistoryReply(context)
                : buildVaccineReply(context);
            case CONDITION -> buildConditionReply(context);
            case CONTACT -> buildContactReply(context);
            case SAFETY -> buildSafetyReply();
            case NONE -> buildFallbackReply();
        };
    }

    private String buildHelpReply() {
        return "You can ask me about your next appointment, medicines, vaccines, conditions, GP details, or request a full health summary. "
            + "If something feels urgent or dangerous, contact your GP, NHS 111, or 999 instead of relying on this chat.";
    }

    private String buildFallbackReply() {
        return "I did not fully understand that request yet. Try asking about your appointment, medicines, vaccines, conditions, GP contact details, or say 'health summary'. "
            + "If you want, you can also ask 'what should I ask next?'.";
    }

    private Intent classifyIntent(String normalizedPrompt) {
        if (containsAny(normalizedPrompt, "hello", "hi", "help", "can you help", "what can you do")) {
            return Intent.HELP;
        }
        if (containsAny(normalizedPrompt, "summary", "overview", "profile", "record", "health summary", "health overview")) {
            return Intent.SUMMARY;
        }
        if (containsAny(normalizedPrompt,
            "appointment", "appointments", "visit", "review", "check up", "checkup", "consultation", "booking", "when am i seeing")) {
            return Intent.APPOINTMENT;
        }
        if (containsAny(normalizedPrompt,
            "medicine", "medicines", "medication", "medications", "prescription", "prescriptions", "drug", "drugs", "tablet", "tablets")) {
            return Intent.PRESCRIPTION;
        }
        if (containsAny(normalizedPrompt,
            "vaccine", "vaccination", "jab", "booster", "flu shot", "shot", "immunisation", "immunization")) {
            return Intent.VACCINE;
        }
        if (containsAny(normalizedPrompt, "condition", "conditions", "diagnosis", "diagnoses", "health issue", "illness", "disease")) {
            return Intent.CONDITION;
        }
        if (containsAny(normalizedPrompt,
            "gp", "doctor", "phone", "email", "contact", "address", "practice", "clinic", "call my gp")) {
            return Intent.CONTACT;
        }
        if (containsAny(normalizedPrompt, "what should i ask", "what else can i ask", "suggest", "options", "what next")) {
            return Intent.HELP;
        }
        return Intent.NONE;
    }

    private String buildSummaryReply(PatientContext context) {
        StringBuilder reply = new StringBuilder();
        reply.append(context.name()).append(" is ").append(context.age()).append(" years old");
        if (!safe(context.gender()).equals("Not recorded")) {
            reply.append(" and recorded as ").append(context.gender().toLowerCase(Locale.UK));
        }
        reply.append(". ");
        reply.append(buildAppointmentReply(context)).append(" ");
        reply.append(buildPrescriptionReply(context)).append(" ");
        reply.append(buildVaccineReply(context)).append(" ");
        if (!context.conditions().isEmpty()) {
            reply.append("There are ").append(context.conditions().size()).append(" recorded condition");
            reply.append(context.conditions().size() == 1 ? ". " : "s. ");
        }
        reply.append("You can also ask for your full prescription list, upcoming appointments, or GP contact details. ");
        reply.append("This assistant reads local records, but it does not replace a doctor.");
        return reply.toString();
    }

    private String buildAppointmentReply(PatientContext context) {
        Appointment next = context.nextAppointment();
        if (next == null) {
            Appointment recent = context.mostRecentAppointment();
            if (recent == null) {
                return "There is no appointment recorded in the local database.";
            }
            return "There is no upcoming appointment recorded. Your most recent appointment was " + formatAppointment(recent) + ".";
        }
        String verified = next.verifiedUpToDate() ? " This entry is marked as up to date." : " This entry may need reconfirming with the practice.";
        return "Your next appointment is " + formatAppointment(next) + "." + verified;
    }

    private String buildAppointmentListReply(PatientContext context) {
        List<Appointment> upcoming = context.upcomingAppointments();
        if (upcoming.isEmpty()) {
            return buildAppointmentReply(context);
        }

        List<String> summary = new ArrayList<>();
        for (Appointment appointment : upcoming) {
            summary.add(formatAppointment(appointment) + appointmentConfidence(appointment));
            if (summary.size() == 3) {
                break;
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append("I found ").append(upcoming.size()).append(" upcoming appointment");
        reply.append(upcoming.size() == 1 ? ": " : "s: ");
        reply.append(String.join("; ", summary)).append(".");
        if (upcoming.size() > 3) {
            reply.append(" There are more appointments after these in the local record.");
        }
        return reply.toString();
    }

    private String buildPrescriptionReply(PatientContext context) {
        if (context.prescriptions().isEmpty()) {
            return "There are no prescriptions recorded in your local record.";
        }

        List<String> summary = new ArrayList<>();
        for (Prescription prescription : context.prescriptions()) {
            summary.add(prescription.drug() + " " + safe(prescription.dose()) + ", " + safe(prescription.frequency()));
            if (summary.size() == 2) {
                break;
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append("I found ").append(context.prescriptions().size()).append(" prescription");
        reply.append(context.prescriptions().size() == 1 ? ": " : "s: ");
        reply.append(String.join("; ", summary)).append(".");

        if (context.prescriptions().size() > 2) {
            reply.append(" Ask for a full prescription list if you want more detail.");
        }
        return reply.toString();
    }

    private String buildPrescriptionDetailReply(PatientContext context) {
        if (context.prescriptions().isEmpty()) {
            return "There are no prescriptions recorded in your local record.";
        }

        List<String> lines = new ArrayList<>();
        for (Prescription prescription : context.prescriptions()) {
            StringBuilder line = new StringBuilder();
            line.append(prescription.drug()).append(" ");
            line.append(safe(prescription.dose())).append(", ").append(safe(prescription.frequency()));
            if (!safe(prescription.indication()).equals("Not recorded")) {
                line.append(" for ").append(prescription.indication());
            }
            if (!safe(prescription.startDate()).equals("Not recorded")) {
                line.append(", started ").append(formatDate(prescription.startDate()));
            }
            if (!safe(prescription.prescriber()).equals("Not recorded")) {
                line.append(", prescribed by ").append(prescription.prescriber());
            }
            lines.add(line.toString());
            if (lines.size() == 4) {
                break;
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append("Here is your prescription list: ").append(String.join("; ", lines)).append(".");
        if (context.prescriptions().size() > 4) {
            reply.append(" I have only read the first four items aloud to keep it concise.");
        }
        return reply.toString();
    }

    private String buildVaccineReply(PatientContext context) {
        if (context.vaccines().isEmpty()) {
            return "There are no vaccine records stored for this patient.";
        }

        Vaccine latest = context.latestVaccine();
        if (latest == null) {
            return "I found " + context.vaccines().size() + " vaccine record(s), but I could not determine the latest date.";
        }

        String product = latest.product() == null || latest.product().isBlank() ? "" : " using " + latest.product();
        return "The latest recorded vaccine is " + latest.name() + " on " + formatDate(latest.date()) + product + ".";
    }

    private String buildVaccineHistoryReply(PatientContext context) {
        if (context.vaccines().isEmpty()) {
            return "There are no vaccine records stored for this patient.";
        }

        List<String> lines = new ArrayList<>();
        for (Vaccine vaccine : context.vaccines()) {
            String product = vaccine.product() == null || vaccine.product().isBlank() ? "" : " (" + vaccine.product() + ")";
            lines.add(vaccine.name() + " on " + formatDate(vaccine.date()) + product);
            if (lines.size() == 4) {
                break;
            }
        }

        StringBuilder reply = new StringBuilder();
        reply.append("Recent vaccine history: ").append(String.join("; ", lines)).append(".");
        if (context.vaccines().size() > 4) {
            reply.append(" More vaccine records are available in the local file.");
        }
        return reply.toString();
    }

    private String buildConditionReply(PatientContext context) {
        if (context.conditions().isEmpty()) {
            return "No long-term conditions are recorded in your local file.";
        }
        List<String> lines = new ArrayList<>();
        for (Condition condition : context.conditions()) {
            StringBuilder line = new StringBuilder(condition.name());
            if (!safe(condition.status()).equals("Not recorded")) {
                line.append(" (").append(condition.status()).append(")");
            }
            if (!safe(condition.sinceDate()).equals("Not recorded")) {
                line.append(", since ").append(formatDate(condition.sinceDate()));
            }
            lines.add(line.toString());
        }
        return "Recorded conditions: " + String.join("; ", lines) + ".";
    }

    private String buildContactReply(PatientContext context) {
        return "Your GP is " + safe(context.gpName()) + " at " + safe(context.gpPractice()) + ". "
            + "Address: " + safe(context.gpAddress()) + ". "
            + "Phone: " + safe(context.telephone()) + ". Email: " + safe(context.email()) + ".";
    }

    private String buildSuggestedPromptReply(PatientContext context) {
        List<String> suggestions = new ArrayList<>();
        if (context.nextAppointment() != null) {
            suggestions.add("ask me to list your upcoming appointments");
        }
        if (!context.prescriptions().isEmpty()) {
            suggestions.add("ask for your full prescription list");
        }
        if (!context.conditions().isEmpty()) {
            suggestions.add("ask what conditions are recorded");
        }
        if (!context.vaccines().isEmpty()) {
            suggestions.add("ask about your latest vaccine");
        }
        suggestions.add("ask for GP contact details");

        return "A good next step would be to " + joinSuggestions(suggestions) + ".";
    }

    private String buildSafetyReply() {
        return "This chat is only for local record support. If this is urgent, contact your GP, NHS 111, or call 999 right away.";
    }

    private String buildFollowUpReply(PatientContext context, String normalizedPrompt) {
        if (lastIntent == Intent.NONE) {
            return null;
        }

        if (wantsExpandedList(normalizedPrompt)) {
            return switch (lastIntent) {
                case APPOINTMENT -> buildAppointmentListReply(context);
                case PRESCRIPTION -> buildPrescriptionDetailReply(context);
                case VACCINE -> buildVaccineHistoryReply(context);
                case CONDITION -> buildConditionReply(context);
                case SUMMARY, CONTACT, HELP, SAFETY, NONE -> null;
            };
        }

        if (containsAny(normalizedPrompt, "next", "what else", "another", "what next")) {
            return buildSuggestedPromptReply(context);
        }

        return null;
    }

    private boolean isFollowUpPrompt(String normalizedPrompt) {
        return containsAny(normalizedPrompt,
            "more",
            "detail",
            "details",
            "full",
            "all",
            "list",
            "next",
            "what else",
            "another",
            "what next");
    }

    private boolean wantsExpandedList(String normalizedPrompt) {
        return containsAny(normalizedPrompt, "all", "full", "list", "detail", "details", "history", "upcoming", "schedule", "more");
    }

    private boolean matchesEmergencyPrompt(String normalizedPrompt) {
        return containsAny(normalizedPrompt,
            "emergency",
            "urgent",
            "chest pain",
            "cant breathe",
            "cannot breathe",
            "collapsed",
            "unconscious",
            "bleeding",
            "severe pain",
            "stroke",
            "heart attack");
    }

    private PatientContext loadPatientContext() {
        return contextProvider.load();
    }

    private String formatAppointment(Appointment appointment) {
        String date = formatDate(appointment.date());
        String time = appointment.time() == null || appointment.time().isBlank() ? "time not recorded" : appointment.time();
        String reason = safe(appointment.reason()).equals("Not recorded") ? "" : " regarding " + appointment.reason();
        return date + " at " + time + " for " + safe(appointment.type()) + " at " + safe(appointment.location()) + reason;
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "an unknown date";
        }

        try {
            return LocalDate.parse(raw).format(FRIENDLY_DATE);
        } catch (DateTimeParseException e) {
            return raw;
        }
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null) {
            return "";
        }
        return prompt.toLowerCase(Locale.UK)
            .replace("'", "")
            .replaceAll("[^a-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private boolean containsAny(String source, String... values) {
        for (String value : values) {
            if (source.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String joinSuggestions(List<String> suggestions) {
        if (suggestions.isEmpty()) {
            return "ask for a health summary";
        }
        if (suggestions.size() == 1) {
            return suggestions.getFirst();
        }
        return String.join(", ", suggestions.subList(0, suggestions.size() - 1))
            + ", or " + suggestions.getLast();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not recorded" : value;
    }

    @FunctionalInterface
    interface PatientContextProvider {
        PatientContext load();
    }

    static class DatabasePatientContextProvider implements PatientContextProvider {
        private final String username;

        DatabasePatientContextProvider(String username) {
            this.username = username;
        }

        @Override
        public PatientContext load() {
            String patientSql = """
                SELECT id, name, age, gender, address_line1, city, telephone, email, gp_name, gp_practice, gp_address
                FROM patients
                WHERE LOWER(name) = LOWER(?) OR LOWER(id) = LOWER(?) OR LOWER(email) = LOWER(?)
                LIMIT 1
                """;

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement patientStmt = conn.prepareStatement(patientSql)) {

                patientStmt.setString(1, username);
                patientStmt.setString(2, username);
                patientStmt.setString(3, username);

                try (ResultSet patientRs = patientStmt.executeQuery()) {
                    if (!patientRs.next()) {
                        return PatientContext.notFound();
                    }

                    String patientId = patientRs.getString("id");
                    List<Condition> conditions = loadConditions(conn, patientId);
                    List<Prescription> prescriptions = loadPrescriptions(conn, patientId);
                    List<Appointment> appointments = loadAppointments(conn, patientId);
                    List<Vaccine> vaccines = loadVaccines(conn, patientId);

                    return new PatientContext(
                        true,
                        "",
                        patientId,
                        patientRs.getString("name"),
                        patientRs.getInt("age"),
                        patientRs.getString("gender"),
                        patientRs.getString("address_line1"),
                        patientRs.getString("city"),
                        patientRs.getString("telephone"),
                        patientRs.getString("email"),
                        patientRs.getString("gp_name"),
                        patientRs.getString("gp_practice"),
                        patientRs.getString("gp_address"),
                        conditions,
                        prescriptions,
                        appointments,
                        vaccines
                    );
                }
            } catch (SQLException e) {
                return PatientContext.error("I could not read the local patient database: " + e.getMessage());
            }
        }

        private static List<Condition> loadConditions(Connection conn, String patientId) throws SQLException {
            String sql = "SELECT condition_name, status, since_date FROM patient_conditions WHERE patient_id = ?";
            List<Condition> conditions = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, patientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        conditions.add(new Condition(
                            rs.getString("condition_name"),
                            rs.getString("status"),
                            rs.getString("since_date")
                        ));
                    }
                }
            }
            return conditions;
        }

        private static List<Prescription> loadPrescriptions(Connection conn, String patientId) throws SQLException {
            String sql = """
                SELECT drug, dose, frequency, indication, start_date, prescriber
                FROM patient_prescriptions
                WHERE patient_id = ?
                ORDER BY start_date DESC
                """;
            List<Prescription> prescriptions = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, patientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        prescriptions.add(new Prescription(
                            rs.getString("drug"),
                            rs.getString("dose"),
                            rs.getString("frequency"),
                            rs.getString("indication"),
                            rs.getString("start_date"),
                            rs.getString("prescriber")
                        ));
                    }
                }
            }
            return prescriptions;
        }

        private static List<Appointment> loadAppointments(Connection conn, String patientId) throws SQLException {
            String sql = """
                SELECT appointment_date, appointment_time, type, location, reason, verified_up_to_date
                FROM patient_appointments
                WHERE patient_id = ?
                ORDER BY appointment_date ASC, appointment_time ASC
                """;
            List<Appointment> appointments = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, patientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        appointments.add(new Appointment(
                            rs.getString("appointment_date"),
                            rs.getString("appointment_time"),
                            rs.getString("type"),
                            rs.getString("location"),
                            rs.getString("reason"),
                            rs.getInt("verified_up_to_date") == 1
                        ));
                    }
                }
            }
            return appointments;
        }

        private static List<Vaccine> loadVaccines(Connection conn, String patientId) throws SQLException {
            String sql = """
                SELECT vaccine, vaccine_date, product
                FROM patient_vaccine_records
                WHERE patient_id = ?
                ORDER BY vaccine_date DESC
                """;
            List<Vaccine> vaccines = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, patientId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        vaccines.add(new Vaccine(
                            rs.getString("vaccine"),
                            rs.getString("vaccine_date"),
                            rs.getString("product")
                        ));
                    }
                }
            }
            return vaccines;
        }
    }

    static record PatientContext(
        boolean found,
        String loadError,
        String patientId,
        String name,
        int age,
        String gender,
        String addressLine1,
        String city,
        String telephone,
        String email,
        String gpName,
        String gpPractice,
        String gpAddress,
        List<Condition> conditions,
        List<Prescription> prescriptions,
        List<Appointment> appointments,
        List<Vaccine> vaccines
    ) {
        static PatientContext notFound() {
            return new PatientContext(false, "", "", "", 0, "", "", "", "", "", "", "", "", List.of(), List.of(), List.of(), List.of());
        }

        static PatientContext error(String message) {
            return new PatientContext(false, message, "", "", 0, "", "", "", "", "", "", "", "", List.of(), List.of(), List.of(), List.of());
        }

        List<String> conditionNames() {
            List<String> names = new ArrayList<>();
            for (Condition condition : conditions) {
                names.add(condition.name());
            }
            return names;
        }

        Appointment nextAppointment() {
            LocalDate today = LocalDate.now();
            for (Appointment appointment : appointments) {
                try {
                    if (!LocalDate.parse(appointment.date()).isBefore(today)) {
                        return appointment;
                    }
                } catch (DateTimeParseException ignored) {
                    return appointment;
                }
            }
            return null;
        }

        List<Appointment> upcomingAppointments() {
            List<Appointment> upcoming = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (Appointment appointment : appointments) {
                try {
                    if (!LocalDate.parse(appointment.date()).isBefore(today)) {
                        upcoming.add(appointment);
                    }
                } catch (DateTimeParseException ignored) {
                    upcoming.add(appointment);
                }
            }
            return upcoming;
        }

        Appointment mostRecentAppointment() {
            if (appointments.isEmpty()) {
                return null;
            }

            Appointment recent = null;
            LocalDate recentDate = null;
            for (Appointment appointment : appointments) {
                try {
                    LocalDate currentDate = LocalDate.parse(appointment.date());
                    if (recent == null || currentDate.isAfter(recentDate)) {
                        recent = appointment;
                        recentDate = currentDate;
                    }
                } catch (DateTimeParseException ignored) {
                    recent = appointment;
                }
            }
            return recent;
        }

        Vaccine latestVaccine() {
            return vaccines.isEmpty() ? null : vaccines.getFirst();
        }
    }

    static record Condition(
        String name,
        String status,
        String sinceDate
    ) {}

    static record Prescription(
        String drug,
        String dose,
        String frequency,
        String indication,
        String startDate,
        String prescriber
    ) {}

    static record Appointment(
        String date,
        String time,
        String type,
        String location,
        String reason,
        boolean verifiedUpToDate
    ) {}

    static record Vaccine(
        String name,
        String date,
        String product
    ) {}

    enum Intent {
        NONE,
        HELP,
        SUMMARY,
        APPOINTMENT,
        PRESCRIPTION,
        VACCINE,
        CONDITION,
        CONTACT,
        SAFETY
    }

    private String appointmentConfidence(Appointment appointment) {
        return appointment.verifiedUpToDate() ? " (verified)" : " (needs reconfirmation)";
    }
}
