package clientUI.patient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import clientUI.DashboardDataService;
import clientUI.FXComponents;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class    MedsScreen extends VBox {
    private final MainPage app;
    private final List<MedicationCardModel> localMedications = new ArrayList<>();

    public MedsScreen(MainPage app) {
        this.app = app;
        setStyle("-fx-background-color: transparent;");
        rebuild();
    }

    public void refresh() {
        rebuild();
    }

    private void rebuild() {
        getChildren().clear();

        ScrollPane scrollPane = new ScrollPane(buildContent());
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }

    private VBox buildContent() {
        List<MedicationCardModel> medications = loadMedications();
        medications.sort(Comparator.comparing((MedicationCardModel item) -> item.taken));

        int total = medications.size();
        int takenCount = (int) medications.stream().filter(item -> item.taken).count();
        int pendingCount = Math.max(total - takenCount, 0);

        VBox root = new VBox(16);
        root.getStyleClass().add("screen-content");

        root.getChildren().addAll(
            buildPlannerCard(total, takenCount, pendingCount),
            FXComponents.sectionIntro(
                "SCHEDULE",
                "Medication plan"
            )
        );

        if (medications.isEmpty()) {
            root.getChildren().add(FXComponents.emptyState(
                "No medication schedule was found for this patient.",
                "You can add a local medication below if you want to continue testing the interface."
            ));
        } else {
            for (MedicationCardModel medication : medications) {
                root.getChildren().add(buildMedicationCard(medication));
            }
        }

        root.getChildren().add(buildAddMedicationCard());
        return root;
    }

    private VBox buildPlannerCard(int total, int takenCount, int pendingCount) {
        VBox card = FXComponents.paddedCard(new Insets(20), "hero-card");
        card.setSpacing(14);

        Label title = new Label("Medication planner");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label(
            total == 0
                ? "No prescriptions are linked yet. Add one below if you want to test the layout."
                : "Pending medicines appear first so the next action is always clear."
        );
        subtitle.getStyleClass().add("hero-body");
        subtitle.setWrapText(true);

        card.getChildren().addAll(
            title,
            subtitle,
            FXComponents.equalWidthRow(
                FXComponents.metricCard("TOTAL", String.valueOf(total), "Medicines in your plan", "Local and synced items are shown together.", "metric-card-primary"),
                FXComponents.metricCard("COMPLETED", String.valueOf(takenCount), "Marked taken today", "These entries have already been logged.", "metric-card-good")
            ),
            FXComponents.metricCard(
                "NEXT ACTION",
                pendingCount == 0 ? "Nothing pending" : pendingCount + " still pending",
                pendingCount == 0 ? "You are up to date" : "Review the pending cards below",
                pendingCount == 0 ? "No extra action is needed right now." : "Tap the status button to update the local medication log.",
                pendingCount == 0 ? "metric-card-good" : "metric-card-accent"
            )
        );
        return card;
    }

    private VBox buildMedicationCard(MedicationCardModel medication) {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(12);

        Label nameLabel = new Label(medication.name);
        nameLabel.getStyleClass().add("section-heading");

        Label frequencyPill = FXComponents.statusPill(
            medication.frequency,
            medication.taken ? "status-pill-success" : "status-pill-neutral"
        );
        Label schedulePill = FXComponents.statusPill(medication.timeHint, "status-pill-neutral");

        HBox chipRow = new HBox(8, schedulePill, frequencyPill);
        chipRow.setAlignment(Pos.CENTER_LEFT);

        Button statusButton = new Button(medication.taken ? "Taken today" : "Mark as taken");
        statusButton.getStyleClass().addAll("status-toggle", medication.taken ? "status-toggle-done" : "status-toggle-pending");
        statusButton.setOnAction(event -> toggleMedication(medication));

        Button deleteButton = FXComponents.secondaryButton("Delete");
        deleteButton.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Medication");
            confirm.setHeaderText("Are you sure?");
            confirm.setContentText("This will remove the medication.");

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                deleteMedication(medication);
            }
        });

        HBox actions = new HBox(8, deleteButton, statusButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        HBox headerRow = new HBox(12, nameLabel, FXComponents.spacer(), actions);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label helper = new Label(
            medication.taken
                ? "This dose is already logged in the local medication history."
                : "Use the button to update the local log when the dose has been taken."
        );
        helper.getStyleClass().add("helper-text");
        helper.setWrapText(true);

        card.getChildren().addAll(
            headerRow,
            chipRow,
            FXComponents.infoRow("Dose", medication.dose),
            FXComponents.infoRow("Schedule", medication.timeHint),
            FXComponents.infoRow("Frequency", medication.frequency),
            helper
        );
        return card;
    }

    private VBox buildAddMedicationCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(10);

        Label title = new Label("Add a medication");
        title.getStyleClass().add("section-heading");

        Label body = new Label(
            "Create a local medication entry when a prescription is missing or when you want to test a new schedule."
        );
        body.getStyleClass().add("section-copy");
        body.setWrapText(true);

        Button addButton = new Button("Add Medication");
        addButton.getStyleClass().add("btn-primary");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setPrefHeight(48);
        addButton.setOnAction(event -> showAddDialog());

        card.getChildren().addAll(title, body, addButton);
        return card;
    }

    private void toggleMedication(MedicationCardModel medication) {
        medication.taken = !medication.taken;
        if (medication.prescriptionId != null && medication.patientId != null) {
            DashboardDataService.logMedicationStatus(
                medication.prescriptionId,
                medication.patientId,
                medication.taken
            );
        }
        rebuild();
    }

    private void deleteMedication(MedicationCardModel medication) {
        if (medication.prescriptionId != null && medication.patientId != null) {
            DashboardDataService.deleteMedication(
                medication.prescriptionId,
                medication.patientId
            );
        } else {
            localMedications.remove(medication);
        }
        rebuild();
    }

    private void showAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Medication");
        dialog.setHeaderText("Create a local medication entry");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Medication name");
        nameField.getStyleClass().add("app-field");

        TextField doseField = new TextField();
        doseField.setPromptText("e.g. 10mg");
        doseField.getStyleClass().add("app-field");

        ComboBox<String> frequencyBox = new ComboBox<>();
        frequencyBox.getItems().addAll(
"Once daily",
            "Twice daily",
            "Three times daily",
            "Once weekly",
            "As needed"
        );
        frequencyBox.setPromptText("Select Frequency");
        frequencyBox.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> dayBox = new ComboBox<>();
        dayBox.getItems().addAll(
"Select Day", "Monday", "Tuesday", "Wednesday", 
            "Thursday", "Friday", "Saturday", "Sunday"
        );
        dayBox.setPromptText("Select Day");
        dayBox.setMaxWidth(Double.MAX_VALUE);

        TextField time1Field = new TextField();
        time1Field.setPromptText("HH:mm");
        time1Field.getStyleClass().add("app-field");

        TextField time2Field = new TextField();
        time2Field.setPromptText("HH:mm");
        time2Field.getStyleClass().add("app-field");

        TextField time3Field = new TextField();
        time3Field.setPromptText("HH:mm");
        time3Field.getStyleClass().add("app-field");

        Label nameLabel = new Label("Name");
        nameLabel.getStyleClass().add("bold-text");
        Label doseLabel = new Label("Dose");
        doseLabel.getStyleClass().add("bold-text");
        Label frequencyLabel = new Label("Frequency");
        frequencyLabel.getStyleClass().add("bold-text");

        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(doseLabel, 0, 1);
        grid.add(doseField, 1, 1);
        grid.add(frequencyLabel, 0, 2);
        grid.add(frequencyBox, 1 , 2);

        Label dayLabel = new Label("Day");
        dayLabel.getStyleClass().add("bold-text");
        Label time1Label = new Label("Time 1");
        time1Label.getStyleClass().add("bold-text");
        Label time2Label = new Label("Time 2");
        time2Label.getStyleClass().add("bold-text");
        Label time3Label = new Label("Time 3");
        time3Label.getStyleClass().add("bold-text");

        grid.add(dayLabel, 0, 3);
        grid.add(dayBox, 1, 3);

        grid.add(time1Label, 0, 4);
        grid.add(time1Field, 1, 4);

        grid.add(time2Label, 0, 5);
        grid.add(time2Field, 1, 5);

        grid.add(time3Label, 0, 6);
        grid.add(time3Field, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(
            nameField.textProperty().isEmpty()
            .or(frequencyBox.valueProperty().isNull())
        );

        Runnable updateFields = () -> {
            String freq = frequencyBox.getValue()== null ? "" : frequencyBox.getValue().toLowerCase();

            boolean weekly = "once weekly".equals(freq);
            boolean once = "once daily".equals(freq);
            boolean twice = "twice daily".equals(freq);
            boolean three = "three times daily".equals(freq);
            boolean asNeeded = "as needed".equals(freq);

            dayBox.setDisable(!weekly);
            time1Field.setDisable(asNeeded);
            time2Field.setDisable(!(twice || three));
            time3Field.setDisable(!three);

            if (!weekly) {
                dayBox.setValue("Select Day");
            }
            if (asNeeded) {
                time1Field.clear();
                time2Field.clear();
                time3Field.clear();
            }
            if (once) {
                time2Field.clear();
                time3Field.clear();
            }
            if (twice) {
            time3Field.clear();
            }
        };
        frequencyBox.valueProperty().addListener((obs, oldVal, newVal) -> updateFields.run());
        updateFields.run();

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            String name = nameField.getText().trim();
            String dose = doseField.getText().trim().isEmpty() ? "Not set" : doseField.getText().trim();
            String frequency = frequencyBox.getValue()== null ? "" : frequencyBox.getValue().toLowerCase();
            String dayOfWeek = "Select Day".equals(dayBox.getValue()) ? null : dayBox.getValue();
            String time1 = time1Field.getText().trim();
            String time2 = time2Field.getText().trim();
            String time3 = time3Field.getText().trim();

            if (!time1.isEmpty() && !time1.matches("\\d{2}:\\d{2}")) {
                showError("Time 1 must be in HH:mm format (e.g. 09:10)");
                return;
            }
            if (!time2.isEmpty() && !time2.matches("\\d{2}:\\d{2}")) {
                showError("Time 2 must be in HH:mm format (e.g. 09:10)");
                return;
            }
            if (!time3.isEmpty() && !time3.matches("\\d{2}:\\d{2}")) {
                showError("Time 3 must be in HH:mm format (e.g. 09:10)");
                return;
            }

            if (!DashboardDataService.addMedication(
                app.getIdentifier(),
                name,
                dose,
                frequency,
                dayOfWeek,
                time1.isEmpty() ? null : time1,
                time2.isEmpty() ? null : time2,
                time3.isEmpty() ? null : time3
            )) {
                localMedications.add(new MedicationCardModel(
                    null,
                    null,
                    name,
                    dose,
                    frequency,
                    DashboardDataService.toTimeHint(frequency),
                    false
                ));
            }

            rebuild();
        });
        
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<MedicationCardModel> loadMedications() {
        List<MedicationCardModel> medications = new ArrayList<>();
        for (DashboardDataService.MedicationItem item : DashboardDataService.getMedications(app.getIdentifier())) {
            medications.add(new MedicationCardModel(
                item.prescriptionId(),
                item.patientId(),
                item.name(),
                item.dose(),
                item.frequency(),
                item.timeHint(),
                item.taken()
            ));
        }

        medications.addAll(localMedications);
        return medications;
    }

    private static final class MedicationCardModel {
        private final Integer prescriptionId;
        private final String patientId;
        private final String name;
        private final String dose;
        private final String frequency;
        private final String timeHint;
        private boolean taken;

        private MedicationCardModel(
            Integer prescriptionId,
            String patientId,
            String name,
            String dose,
            String frequency,
            String timeHint,
            boolean taken
        ) {
            this.prescriptionId = prescriptionId;
            this.patientId = patientId;
            this.name = name;
            this.dose = dose;
            this.frequency = frequency;
            this.timeHint = timeHint;
            this.taken = taken;
        }
    }
}
