package clientUI.patient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import clientUI.DashboardDataService;
import clientUI.FXComponents;
import clientUI.MainPage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class HomeScreen extends VBox {
    private final MainPage app;
    private final Runnable openSettings;
    private final ScrollPane scrollPane;

    private Label timeLabel;
    private Label ampmLabel;
    private Label dateLabel;

    private Timeline clock;

    public HomeScreen(MainPage app, Runnable openSettings) {
        this.app = app;
        this.openSettings = openSettings;

        setStyle("-fx-background-color: transparent;");
        setFillWidth(true);

        scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        refresh();
        startClock();
    }

    public void refresh() {
        scrollPane.setContent(buildContent());
        updateTime();
    }

    private VBox buildContent() {
        Optional<DashboardDataService.PatientSummary> summary = DashboardDataService.getPatientSummary(app.getIdentifier());
        Optional<DashboardDataService.AppointmentItem> appointment = DashboardDataService.getNextAppointment(app.getIdentifier());
        List<DashboardDataService.MedicationItem> medications = DashboardDataService.getMedications(app.getIdentifier());
        List<DashboardDataService.ConditionItem> conditions = DashboardDataService.getConditions(app.getIdentifier());
        Optional<DashboardDataService.VaccineItem> vaccine = DashboardDataService.getLatestVaccine(app.getIdentifier());

        int totalMedications = medications.size();
        int takenCount = (int) medications.stream().filter(DashboardDataService.MedicationItem::taken).count();
        int pendingCount = Math.max(totalMedications - takenCount, 0);

        DashboardDataService.MedicationItem nextMedication = medications.stream()
            .filter(item -> !item.taken())
            .findFirst()
            .orElseGet(() -> medications.isEmpty()
                ? new DashboardDataService.MedicationItem(null, null, "No medication scheduled", "-", "-", "No schedule", false)
                : medications.get(0));

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-spacing: 14;");

        root.getStyleClass().add("screen-content");

        // Main pill - grows to fill available space
        VBox mainPill = buildMainPill(summary, appointment);
        VBox.setVgrow(mainPill, Priority.ALWAYS);
        root.getChildren().add(mainPill);

        // Secondary grid with minimal spacing
        VBox gridSection = new VBox(8);

        Label actionsLabel = new Label("Touch an action to see more");
        actionsLabel.getStyleClass().add("home-actions-label");
        actionsLabel.setStyle("-fx-font-size: 12px;");
        actionsLabel.setMaxWidth(Double.MAX_VALUE);
        actionsLabel.setAlignment(Pos.CENTER);

        gridSection.getChildren().addAll(actionsLabel, buildSecondaryGrid(medications, appointment, pendingCount, nextMedication));
        root.getChildren().add(gridSection);

        return root;
    }

    private VBox buildMainPill(Optional<DashboardDataService.PatientSummary> summary,
                               Optional<DashboardDataService.AppointmentItem> appointment) {
        String displayName = summary.map(DashboardDataService.PatientSummary::name).orElse(app.getDisplayName());
        String appointmentReminder = appointment
            .map(item -> {
                String base = "Next appointment\n" + item.time();
                String loc = summary.map(DashboardDataService.PatientSummary::gpPractice).orElse(null);
                
                if (loc != null && !loc.equalsIgnoreCase("Not recorded")) {
                    return base + " at " + loc;
                }
                return base;
            })
            .orElse("No upcoming appointment");

        VBox card = FXComponents.paddedCard(new Insets(28, 36, 28, 36), "hero-card");
        card.setSpacing(20);
        card.setMinHeight(280);

        // Get weather data
        Optional<DashboardDataService.WeatherItem> weather = DashboardDataService.getCurrentWeather(app.getIdentifier());

        // Top row: user name only (Settings is in the navbar)
        Label userNameLabel = new Label(displayName);
        userNameLabel.getStyleClass().add("hero-title");
        userNameLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        HBox topRow = new HBox(userNameLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Main content row: time on left, weather on right (both grow proportionally)
        dateLabel = new Label(getTodayString());
        dateLabel.getStyleClass().add("hero-kicker");
        dateLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        timeLabel = new Label("10:45");
        timeLabel.getStyleClass().add("hero-time");
        timeLabel.setStyle("-fx-font-size: 130px; -fx-font-weight: bold; -fx-line-spacing: 0; -fx-padding: -12 0 -8 0;");

        ampmLabel = new Label("AM");
        ampmLabel.getStyleClass().add("hero-ampm");
        ampmLabel.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-padding: 0 0 8 0;");

        VBox timeBlock = new VBox(0, dateLabel, timeLabel, ampmLabel);
        timeBlock.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(timeBlock, Priority.ALWAYS);

        VBox weatherPanel = buildWeatherPanel(weather);
        HBox.setHgrow(weatherPanel, Priority.NEVER);

        HBox mainRow = new HBox(24, timeBlock, weatherPanel);
        mainRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(mainRow, Priority.ALWAYS);
        VBox.setVgrow(mainRow, Priority.ALWAYS);

        // Appointment reminder — prominent, dark, large
        Label appointmentLabel = new Label(appointmentReminder);
        appointmentLabel.getStyleClass().add("hero-appointment");
        appointmentLabel.setStyle("-fx-font-size: 22px; -fx-line-spacing: 5px;");
        appointmentLabel.setWrapText(true);
        appointmentLabel.setMaxWidth(Double.MAX_VALUE);
        appointmentLabel.setMinHeight(60);

        // Floating mic button — text left, icon right, whole row right-aligned
        StackPane micBtn = FXComponents.micButton(true, 52);
        micBtn.setOnMouseClicked(event -> app.openVoiceChat());

        Label micLabel = new Label("Start Voice Assistant");
        micLabel.getStyleClass().add("hero-mic-label");
        micLabel.setStyle("-fx-font-size: 18px;");
        micLabel.setOnMouseClicked(event -> app.openVoiceChat());

        Region voiceSpacer = new Region();
        HBox.setHgrow(voiceSpacer, Priority.ALWAYS);

        HBox voiceButtonContainer = new HBox(14, voiceSpacer, micLabel, micBtn);
        voiceButtonContainer.setAlignment(Pos.CENTER_RIGHT);
        voiceButtonContainer.setTranslateY(-10); // Move button container up

        card.getChildren().addAll(topRow, mainRow, appointmentLabel, voiceButtonContainer);
        VBox.setVgrow(voiceButtonContainer, Priority.ALWAYS); // Ensure button container uses space efficiently
        return card;
    }

    private VBox buildWeatherPanel(Optional<DashboardDataService.WeatherItem> weather) {
        VBox panel = new VBox(6);
        panel.getStyleClass().add("weather-panel");
        panel.setStyle("-fx-padding: 14 18;");
        panel.setAlignment(Pos.CENTER_RIGHT);
        panel.setMinWidth(260);

        if (weather.isPresent()) {
            DashboardDataService.WeatherItem w = weather.get();

            // Location — top right
            Label locationLabel = new Label(w.location());
            locationLabel.getStyleClass().add("weather-location");
            locationLabel.setMaxWidth(Double.MAX_VALUE);
            locationLabel.setAlignment(Pos.CENTER_RIGHT);

            // Temperature + icon on the same row: temp on left, icon flush right
            Label tempLabel = new Label(w.temperature().intValue() + "°C");
            tempLabel.getStyleClass().add("weather-temp");

            Region rowSpacer = new Region();
            HBox.setHgrow(rowSpacer, Priority.ALWAYS);

            String symbol = getWeatherSymbol(w.condition());
            Label symbolLabel = new Label(symbol);
            symbolLabel.getStyleClass().add("weather-symbol");

            HBox tempIconRow = new HBox(8, tempLabel, rowSpacer, symbolLabel);
            tempIconRow.setAlignment(Pos.CENTER);
            tempIconRow.setMaxWidth(Double.MAX_VALUE);

            // Condition — bottom right
            Label conditionLabel = new Label(w.condition());
            conditionLabel.getStyleClass().add("weather-condition");
            conditionLabel.setMaxWidth(Double.MAX_VALUE);
            conditionLabel.setAlignment(Pos.CENTER_RIGHT);

            panel.getChildren().addAll(locationLabel, tempIconRow, conditionLabel);
        } else {
            Label unavailableLabel = new Label("Weather\nunavailable");
            unavailableLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 16px;");
            unavailableLabel.setAlignment(Pos.CENTER_RIGHT);
            panel.getChildren().add(unavailableLabel);
        }

        return panel;
    }

    private String getWeatherSymbol(String condition) {
        if (condition == null) return "?";
        switch (condition.toLowerCase()) {
            case "clear": return "\u2600"; // ☀ sun
            case "clouds": return "\u2601"; // ☁ cloud
            case "rain": return "\u2602"; // ☂ umbrella/rain
            case "snow": return "\u2744"; // ❄ snowflake
            case "thunderstorm": return "\u26A1"; // ⚡ lightning
            case "drizzle": return "\u2614"; // ☔ umbrella with rain
            case "mist":
            case "fog":
            case "haze": return "\u2248"; // ≈ haze/mist
            default: return "\u2600"; // ☀ sun
        }
    }

    // ...existing code...

    private VBox buildCareSummaryCard(Optional<DashboardDataService.PatientSummary> summary,
                                      List<DashboardDataService.ConditionItem> conditions,
                                      Optional<DashboardDataService.VaccineItem> vaccine) {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(14);

        Label title = new Label("Care summary");
        title.getStyleClass().add("section-heading");

        Label body = new Label("Local patient record on this device. Please contact the Care Team to have incorrect info changed.");
        body.getStyleClass().add("section-copy");
        body.setWrapText(true);

        String conditionText = conditions.isEmpty()
            ? "None recorded"
            : conditions.stream()
                .map(item -> item.status().isBlank() ? item.name() : item.name() + " (" + item.status() + ")")
                .collect(Collectors.joining(", "));

        card.getChildren().addAll(
            title,
            body,
            FXComponents.infoRow("Patient", summary.map(DashboardDataService.PatientSummary::name).orElse(app.getDisplayName())),
            FXComponents.infoRow("GP", summary.map(item -> item.gpName() + " - " + item.gpPractice()).orElse("Not recorded")),
            FXComponents.infoRow("GP Address", summary.map(DashboardDataService.PatientSummary::gpAddress).orElse("Not recorded")),
            FXComponents.infoRow("Contact", summary.map(DashboardDataService.PatientSummary::telephone).orElse("Not recorded")),
            FXComponents.infoRow("Conditions", conditionText),
            FXComponents.infoRow(
                "Latest vaccine",
                vaccine.map(item -> item.name() + " on " + item.date()).orElse("Not recorded")
            )
        );

        HBox actions = FXComponents.equalWidthRow(
            buildSecondaryAction(" View Records", this::showRecordsDialog),
            buildSecondaryAction("Change Settings", openSettings)
        );
        card.getChildren().add(actions);
        return card;
    }

    private Button buildSecondaryAction(String text, Runnable action) {
        Button button = FXComponents.secondaryButton(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox buildEmergencyCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "alert-card");
        card.setSpacing(12);

        Label title = new Label("Need urgent help?");
        title.getStyleClass().add("section-heading");

        Label body = new Label(
            "If someone is having a medical emergency, contact emergency services immediately."
        );
        body.getStyleClass().add("section-copy");
        body.setWrapText(true);

        VBox contactInfo = new VBox(4);
        Label call999 = new Label("• Call 999 for life-threatening emergencies");
        Label call111 = new Label("• Call 111 for urgent medical advice");
        call999.getStyleClass().add("section-copy");
        call111.getStyleClass().add("section-copy");
        contactInfo.getChildren().addAll(call999, call111);

        Button emergencyButton = new Button("Emergency Help");
        emergencyButton.getStyleClass().add("btn-emergency");
        emergencyButton.setMaxWidth(Double.MAX_VALUE);
        emergencyButton.setPrefHeight(48);
        emergencyButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Emergency Guidance");
            alert.setHeaderText("Immediate help");
            alert.setContentText("If this is a medical emergency, call 999 now. For urgent medical advice that is not an emergency, call 111.");
            alert.showAndWait();
        });

        card.getChildren().addAll(title, body, contactInfo, emergencyButton);
        return card;
    }

    private void showRecordsDialog() {
        var summary = DashboardDataService.getPatientSummary(app.getIdentifier());
        List<DashboardDataService.ConditionItem> conditions = DashboardDataService.getConditions(app.getIdentifier());
        var vaccine = DashboardDataService.getLatestVaccine(app.getIdentifier());

        StringBuilder body = new StringBuilder();
        if (summary.isPresent()) {
            var patient = summary.get();
            body.append(patient.name()).append(" (").append(patient.patientId()).append(")\n");
            body.append("Age: ").append(patient.age()).append("\n");
            body.append("Address: ").append(patient.address()).append("\n");
            body.append("GP: ").append(patient.gpName()).append(" - ").append(patient.gpPractice()).append("\n");
            body.append("GP Address: ").append(patient.gpAddress()).append("\n");
            body.append("Phone: ").append(patient.telephone()).append("\n");
            body.append("Email: ").append(patient.email()).append("\n\n");
        } else if (!conditions.isEmpty() || vaccine.isPresent()) {
            // Fallback if summary is missing but other data exists (identifier likely resolved to a patient)
            body.append(app.getDisplayName()).append(" (Profile details partially unavailable)\n\n");
        } else {
            body.append("No patient profile is linked to this account yet.\n\n");
        }

        body.append("Conditions: ");
        if (conditions.isEmpty()) {
            body.append("None recorded");
        } else {
            body.append(
                conditions.stream()
                    .map(condition -> condition.status().isBlank()
                        ? condition.name()
                        : condition.name() + " (" + condition.status() + ")")
                    .collect(Collectors.joining(", "))
            );
        }
        body.append("\n");

        body.append("Latest vaccine: ");
        if (vaccine.isPresent()) {
            body.append(vaccine.get().name()).append(" on ").append(vaccine.get().date());
        } else {
            body.append("Not recorded");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Patient Records");
        alert.setHeaderText("Local patient summary");
        alert.setContentText(body.toString());
        alert.showAndWait();
    }

    private void startClock() {
        updateTime();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTime()));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateTime() {
        var now = java.time.LocalTime.now();
        boolean uses24HourClock = app.uses24HourClock();

        if (timeLabel != null) {
            if (uses24HourClock) {
                timeLabel.setText(String.format("%02d:%02d", now.getHour(), now.getMinute()));
                ampmLabel.setText("");
                ampmLabel.setManaged(false);
                ampmLabel.setVisible(false);
            } else {
                int hour = now.getHour() % 12;
                if (hour == 0) {
                    hour = 12;
                }
                timeLabel.setText(hour + ":" + String.format("%02d", now.getMinute()));
                ampmLabel.setText(now.getHour() >= 12 ? "PM" : "AM");
                ampmLabel.setManaged(true);
                ampmLabel.setVisible(true);
            }
            if (dateLabel != null) {
                dateLabel.setText(getTodayString());
            }
        }
    }

    private String getGreeting() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) {
            return "Good morning";
        }
        if (hour < 18) {
            return "Good afternoon";
        }
        return "Good evening";
    }

    private String getTodayString() {
        return java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM", java.util.Locale.ENGLISH)
        );
    }

    private HBox buildSecondaryGrid(List<DashboardDataService.MedicationItem> medications,
                                   Optional<DashboardDataService.AppointmentItem> appointment,
                                   int pendingCount,
                                   DashboardDataService.MedicationItem nextMedication) {
        VBox medicationTile = buildSecondaryTile(
            "MEDICATIONS",
            pendingCount == 0 ? "All up to date" : pendingCount + " pending",
            nextMedication.name(),
            pendingCount == 0 ? "action-tile-good" : "action-tile-primary",
            app::openReminders
        );

        VBox nextVisitTile = buildSecondaryTile(
            "SCHEDULE",
            "My Appointments",
            appointment.map(DashboardDataService.AppointmentItem::type).orElse("No appointment on file"),
            "action-tile-neutral",
            app::openAppointments
        );

        VBox careTeamTile = buildSecondaryTile(
            "CARE TEAM",
            "Messaging",
            "Contact updates and support",
            "action-tile-accent",
            app::openMessaging
        );

        VBox moreTile = buildSecondaryTile(
            "MORE",
            "Additional options",
            "Conditions, vaccines, emergency help",
            "action-tile-secondary",
            this::showMoreSubmenu
        );

        HBox grid = FXComponents.equalWidthRow(medicationTile, nextVisitTile, careTeamTile, moreTile);
        grid.setStyle("-fx-spacing: 12;");
        return grid;
    }

    private VBox buildSecondaryTile(String eyebrow, String title, String body, String styleClass, Runnable action) {
        VBox tile = FXComponents.actionTile(eyebrow, title, body, styleClass);
        tile.setStyle("-fx-cursor: hand; -fx-padding: 16;");
        // Prevent body label from stretching to fill extra vertical space
        if (!tile.getChildren().isEmpty()) {
            VBox.setVgrow(tile.getChildren().get(tile.getChildren().size() - 1), Priority.NEVER);
        }
        tile.setOnMouseClicked(event -> action.run());
        return tile;
    }

    private void showMoreSubmenu() {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("More Options");

        DialogPane dialogPane = alert.getDialogPane();
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }
        dialogPane.getStyleClass().add("card");
        dialogPane.setPrefWidth(600);

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));

        Label header = new Label("Additional Options");
        header.getStyleClass().add("section-heading");

        VBox grid = new VBox(12);

        HBox row1 = FXComponents.equalWidthRow(
            buildSecondaryTile("CONDITIONS", "Health Records", "Review your active diagnoses and medical history.", "action-tile-primary", () -> {
                alert.close();
                javafx.application.Platform.runLater(() -> showConditionsDialog());
            }),
            buildSecondaryTile("VACCINES", "Immunisation", "See your vaccination history and latest doses.", "action-tile-accent", () -> {
                alert.close();
                javafx.application.Platform.runLater(() -> showVaccinationsDialog());
            })
        );

        HBox row2 = FXComponents.equalWidthRow(
            buildSecondaryTile("TRAVEL CHECK", "Weather", "See local weather before you head out.", "action-tile-neutral", () -> {
                alert.close();
                javafx.application.Platform.runLater(() -> app.openWeather());
            }),
            buildSecondaryTile("PROFILE", "Care Summary", "View a local summary of your patient profile.", "action-tile-secondary", () -> {
                alert.close();
                javafx.application.Platform.runLater(() -> showCareSummaryDialog());
            })
        );

        VBox emergencyTile = buildSecondaryTile("URGENT", "Emergency Help", "Immediate guidance for medical emergencies.", "action-tile-danger", () -> {
            alert.close();
            javafx.application.Platform.runLater(() -> showEmergencyDialog());
        });

        Button closeButton = FXComponents.secondaryButton("Close this menu ");
        closeButton.setMaxWidth(Double.MAX_VALUE);
        closeButton.setPrefHeight(60);
        closeButton.setStyle(closeButton.getStyle() + "-fx-font-size: 16px; -fx-border-radius: 18; -fx-background-radius: 18;");
        closeButton.setOnAction(e -> alert.close());

        grid.getChildren().addAll(row1, row2, emergencyTile);
        content.getChildren().addAll(header, grid, closeButton);

        dialogPane.setContent(content);
        
        // JavaFX Dialogs require at least one ButtonType to enable closing via code
        dialogPane.getButtonTypes().add(ButtonType.CLOSE);
        javafx.scene.Node actualCloseButton = dialogPane.lookupButton(ButtonType.CLOSE);
        actualCloseButton.setManaged(false);
        actualCloseButton.setVisible(false);

        // Make dialog background transparent to show our card style
        alert.initStyle(StageStyle.TRANSPARENT);
        dialogPane.setStyle("-fx-background-color: transparent;");

        alert.showAndWait();
    }

    private void showConditionsDialog() {
        List<DashboardDataService.ConditionItem> conditions = DashboardDataService.getConditions(app.getIdentifier());
        String conditionText = conditions.isEmpty()
            ? "No conditions recorded"
            : conditions.stream()
                .map(item -> item.status().isBlank() ? item.name() : item.name() + " (" + item.status() + ")")
                .collect(Collectors.joining("\n"));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Conditions");
        alert.setHeaderText("Your recorded conditions");
        alert.setContentText(conditionText);
        alert.showAndWait();
    }

    private void showVaccinationsDialog() {
        Optional<DashboardDataService.VaccineItem> vaccine = DashboardDataService.getLatestVaccine(app.getIdentifier());
        String vaccineText = vaccine.isPresent()
            ? vaccine.get().name() + " on " + vaccine.get().date() + "\nProduct: " + vaccine.get().product()
            : "No vaccinations recorded";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Vaccinations");
        alert.setHeaderText("Your latest vaccination");
        alert.setContentText(vaccineText);
        alert.showAndWait();
    }

    private void showEmergencyDialog() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Emergency Guidance");
        alert.setHeaderText("Immediate help");
        alert.setContentText("If this is a medical emergency, call 999 now. For urgent medical advice that is not an emergency, call 111.");
        alert.showAndWait();
    }

    private void showCareSummaryDialog() {
        Optional<DashboardDataService.PatientSummary> summary = DashboardDataService.getPatientSummary(app.getIdentifier());
        List<DashboardDataService.ConditionItem> conditions = DashboardDataService.getConditions(app.getIdentifier());
        Optional<DashboardDataService.VaccineItem> vaccine = DashboardDataService.getLatestVaccine(app.getIdentifier());

        StringBuilder body = new StringBuilder();
        if (summary.isPresent()) {
            var patient = summary.get();
            body.append("Patient: ").append(patient.name()).append(" (").append(patient.patientId()).append(")\n");
            body.append("Age: ").append(patient.age()).append("\n");
            body.append("Address: ").append(patient.address()).append("\n");
            body.append("GP: ").append(patient.gpName()).append(" - ").append(patient.gpPractice()).append("\n");
            body.append("GP Address: ").append(patient.gpAddress()).append("\n");
            body.append("Contact: ").append(patient.telephone()).append("\n");
            body.append("Email: ").append(patient.email()).append("\n\n");
        } else {
            body.append("Patient details not available.\n\n");
        }

        body.append("Conditions: ");
        if (conditions.isEmpty()) {
            body.append("None recorded");
        } else {
            body.append(
                conditions.stream()
                    .map(condition -> condition.status().isBlank()
                        ? condition.name()
                        : condition.name() + " (" + condition.status() + ")")
                    .collect(Collectors.joining(", "))
            );
        }
        body.append("\n");

        body.append("Latest vaccine: ");
        if (vaccine.isPresent()) {
            body.append(vaccine.get().name()).append(" on ").append(vaccine.get().date());
        } else {
            body.append("Not recorded");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Care Summary");
        alert.setHeaderText("Local patient record");
        alert.setContentText(body.toString());
        alert.showAndWait();
    }
}
