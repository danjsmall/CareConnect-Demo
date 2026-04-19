package clientUI.patient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import clientUI.DashboardDataService;
import clientUI.FXComponents;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class VitalsScreen extends VBox {
    private final MainPage app;

    private double height = 175;
    private double weight = 72;

    public VitalsScreen(MainPage app) {
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
        double bmi = weight / ((height / 100.0) * (height / 100.0));
        String bmiText = String.format(Locale.ENGLISH, "%.1f", bmi);
        String bmiStatus = bmiStatus(bmi);
        String bmiClass = bmiClass(bmi);

        VBox root = new VBox(16);
        root.getStyleClass().add("screen-content");

        root.getChildren().addAll(
            buildHeroCard(bmiStatus),
            FXComponents.sectionIntro(
                "CURRENT READING",
                "Latest measurements"
            ),
            FXComponents.equalWidthRow(
                FXComponents.metricCard("HEIGHT", (int) height + " cm", "Body height", "Used in the BMI calculation.", "metric-card-neutral"),
                FXComponents.metricCard("WEIGHT", (int) weight + " kg", "Current weight", "Update this when a new reading is taken.", "metric-card-primary")
            ),
            FXComponents.metricCard(
                "BODY MASS INDEX",
                bmiText,
                bmiStatus,
                bmiGuidance(bmiStatus),
                bmiClass
            ),
            buildReadingCard(),
            FXComponents.sectionIntro(
                "RECORDS",
                "Diagnoses"
            ),
            buildDiagnosisSection()
        );
        return root;
    }

    private VBox buildHeroCard(String bmiStatus) {
        VBox card = FXComponents.paddedCard(new Insets(20), "hero-card");
        card.setSpacing(14);

        Label title = new Label("Health snapshot");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label(
            "Review the current reading, understand the BMI status, and log a new measurement when values change."
        );
        subtitle.getStyleClass().add("hero-body");
        subtitle.setWrapText(true);

        Label date = new Label(
            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMM, yyyy", Locale.ENGLISH))
        );
        date.getStyleClass().add("hero-kicker");

        StackPane micButton = FXComponents.micButton(true, 48);
        micButton.setOnMouseClicked(event -> app.openVoiceChat());

        HBox header = new HBox(12);
        VBox textBlock = new VBox(4, date, title, subtitle);
        textBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBlock, Priority.ALWAYS);
        header.getChildren().addAll(textBlock, micButton);

        HBox statusRow = new HBox(8,
            FXComponents.statusPill("BMI status", "status-pill-neutral"),
            FXComponents.statusPill(bmiStatus, bmiStatus.equals("Normal") ? "status-pill-success" : "status-pill-warning")
        );
        statusRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(header, statusRow);
        return card;
    }

    private VBox buildReadingCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(12);

        Label title = new Label("Take a new reading");
        title.getStyleClass().add("section-heading");

        Label body = new Label(
            "Update height and weight to refresh the BMI card. This is useful for demonstrations and local testing."
        );
        body.getStyleClass().add("section-copy");
        body.setWrapText(true);

        Button button = new Button("Update Reading");
        button.getStyleClass().add("btn-primary");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(48);
        button.setOnAction(event -> showReadingDialog());

        card.getChildren().addAll(title, body, button);
        return card;
    }

    private VBox buildDiagnosisSection() {
        VBox content = new VBox(12);
        List<DashboardDataService.ConditionItem> conditions = DashboardDataService.getConditions(app.getIdentifier());
        if (conditions.isEmpty()) {
            content.getChildren().add(FXComponents.emptyState(
                "No diagnoses are stored for this patient yet.",
                "The vitals panel can still be used for local reading updates and UI testing."
            ));
        } else {
            for (DashboardDataService.ConditionItem condition : conditions) {
                content.getChildren().add(buildDiagnosisCard(condition));
            }
        }
        return content;
    }

    private VBox buildDiagnosisCard(DashboardDataService.ConditionItem condition) {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(10);

        Label nameLabel = new Label(condition.name());
        nameLabel.getStyleClass().add("section-heading");

        Label statusPill = FXComponents.statusPill(
            condition.status().isBlank() ? "Recorded" : condition.status(),
            condition.status().isBlank() ? "status-pill-neutral" : "status-pill-success"
        );

        HBox topRow = new HBox(12, nameLabel, FXComponents.spacer(), statusPill);
        topRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
            topRow,
            FXComponents.infoRow("Since", condition.sinceDate().isBlank() ? "Not recorded" : condition.sinceDate()),
            FXComponents.infoRow(
                "Summary",
                condition.sinceDate().isBlank()
                    ? "No onset date was recorded for this diagnosis."
                    : "Recorded since " + condition.sinceDate() + "."
            )
        );
        return card;
    }

    private void showReadingDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New Reading");
        dialog.setHeaderText("Update the current local reading");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        Label heightLabel = new Label("Height (cm)");
        heightLabel.getStyleClass().add("bold-text");
        Spinner<Integer> heightSpinner = new Spinner<>(100, 250, (int) height);
        heightSpinner.setEditable(true);

        Label weightLabel = new Label("Weight (kg)");
        weightLabel.getStyleClass().add("bold-text");
        Spinner<Integer> weightSpinner = new Spinner<>(30, 300, (int) weight);
        weightSpinner.setEditable(true);

        grid.add(heightLabel, 0, 0);
        grid.add(heightSpinner, 1, 0);
        grid.add(weightLabel, 0, 1);
        grid.add(weightSpinner, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                height = heightSpinner.getValue();
                weight = weightSpinner.getValue();
                rebuild();
            }
        });
    }

    private String bmiStatus(double bmi) {
        if (bmi < 18.5) {
            return "Underweight";
        }
        if (bmi < 25) {
            return "Normal";
        }
        if (bmi < 30) {
            return "Overweight";
        }
        return "Obese";
    }

    private String bmiGuidance(String status) {
        return switch (status) {
            case "Underweight" -> "A BMI below 18.5 is usually considered under the healthy range.";
            case "Normal" -> "A BMI between 18.5 and 24.9 is usually considered within the healthy range.";
            case "Overweight" -> "A BMI between 25 and 29.9 is usually considered above the healthy range.";
            default -> "A BMI of 30 or above is usually considered in the obese range.";
        };
    }

    private String bmiClass(double bmi) {
        if (bmi < 18.5) {
            return "metric-card-warn";
        }
        if (bmi < 25) {
            return "metric-card-good";
        }
        if (bmi < 30) {
            return "metric-card-warn";
        }
        return "metric-card-danger";
    }
}
