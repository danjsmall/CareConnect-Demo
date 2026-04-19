package clientUI.patient;

import java.util.List;
import java.util.Optional;

import clientUI.DashboardDataService;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AppointmentPage {
    private final MainPage parent;
    private final String username;

    private VBox appointmentsBox;

    public AppointmentPage(MainPage parent, String username, int level) {
        this.parent = parent;
        this.username = username;
    }

    public void start(Stage stage) {
        stage.setTitle("Appointment Management");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f4f4f4;");

        Label title = new Label("Appointment Management - " + username);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        BorderPane.setAlignment(title, Pos.CENTER);
        BorderPane.setMargin(title, new Insets(20, 20, 10, 20));
        root.setTop(title);

        appointmentsBox = new VBox(12);
        appointmentsBox.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(appointmentsBox);
        scrollPane.setFitToWidth(true);
        BorderPane.setMargin(scrollPane, new Insets(10, 20, 10, 20));
        root.setCenter(scrollPane);

        Button addButton = new Button("Add Appointment");
        Button backButton = new Button("Back");

        addButton.setOnAction(e -> showAddAppointmentDialog());
        backButton.setOnAction(e -> parent.start(stage));

        HBox buttonBox = new HBox(10, addButton, backButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 20, 20, 20));
        root.setBottom(buttonBox);

        refreshAppointments();

        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }

    private void refreshAppointments() {
        appointmentsBox.getChildren().clear();

        List<DashboardDataService.AppointmentItem> appointments =
            DashboardDataService.getAppointments(username);

        if (appointments.isEmpty()) {
            Label empty = new Label("No appointments found.");
            empty.setStyle("-fx-font-size: 16px;");
            appointmentsBox.getChildren().add(empty);
            return;
        }

        for (DashboardDataService.AppointmentItem appt : appointments) {
            appointmentsBox.getChildren().add(buildAppointmentCard(appt));
        }
    }

    private VBox buildAppointmentCard(DashboardDataService.AppointmentItem appt) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #dddddd; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label type = new Label(appt.type());
        type.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        String rawTime = appt.appointmentTime().isBlank() ? "Not recorded" : appt.appointmentTime();

        Label date = new Label("Date: " + appt.date());
        Label time = new Label("Time: " + rawTime);
        Label location = new Label("Location: " + appt.location());
        Label reason = new Label("Reason: " + appt.reason());

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> deleteAppointment(appt));

        card.getChildren().addAll(type, date, time, location, reason, deleteButton);
        return card;
    }

    private void showAddAppointmentDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Appointment");
        dialog.setHeaderText("Create a new appointment");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField dateField = new TextField();
        dateField.setPromptText("YYYY-MM-DD");

        TextField timeField = new TextField();
        timeField.setPromptText("HH:mm");

        TextField typeField = new TextField();
        typeField.setPromptText("e.g. GP Review");

        TextField locationField = new TextField();
        locationField.setPromptText("e.g. NHS Clinic");

        TextField reasonField = new TextField();
        reasonField.setPromptText("e.g. Blood pressure review");

        grid.add(new Label("Date"), 0, 0);
        grid.add(dateField, 1, 0);
        grid.add(new Label("Time"), 0, 1);
        grid.add(timeField, 1, 1);
        grid.add(new Label("Type"), 0, 2);
        grid.add(typeField, 1, 2);
        grid.add(new Label("Location"), 0, 3);
        grid.add(locationField, 1, 3);
        grid.add(new Label("Reason"), 0, 4);
        grid.add(reasonField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }

            String date = dateField.getText().trim();
            String time = timeField.getText().trim();
            String type = typeField.getText().trim();
            String location = locationField.getText().trim();
            String reason = reasonField.getText().trim();

            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                showError("Date must be in YYYY-MM-DD format.");
                return;
            }

            if (!time.isEmpty() && !time.matches("\\d{2}:\\d{2}")) {
                showError("Time must be in HH:mm format.");
                return;
            }

            boolean added = DashboardDataService.addAppointment(
                username,
                date,
                time.isEmpty() ? null : time,
                type,
                location,
                reason
            );

            if (!added) {
                showError("Failed to add appointment.");
                return;
            }

            refreshAppointments();
        });
    }

    private void deleteAppointment(DashboardDataService.AppointmentItem appt) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Appointment");
        confirm.setHeaderText("Delete this appointment?");
        confirm.setContentText(appt.type() + " on " + appt.date());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = DashboardDataService.deleteAppointment(appt.appointmentId(), username);
        if (!deleted) {
            showError("Failed to delete appointment.");
            return;
        }

        refreshAppointments();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Appointment Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}