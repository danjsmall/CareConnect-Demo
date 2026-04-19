package clientUI;

import java.util.Optional;

import clientUI.patient.RemindersPage;
import clientUI.patient.VoiceChatPage;
import clientUI.patient.WeatherPage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class MainPage {
    private final String displayName;
    private final int level;
    private final String identifier;
    private Stage stage;
    private MainShell shell;
    private DashboardDataService.SettingsData settings;

    public MainPage(String displayName, int level, String identifier) {
        this.displayName = displayName;
        this.level = level;
        this.identifier = identifier;
        this.settings = DashboardDataService.getSettings();
    }

    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        reloadSettings();
        shell = new MainShell(this);
        shell.applySettings(settings);

        Scene scene = new Scene(shell, 1024, 768);
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setTitle("Care Connect Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }

    public String getUsername() {
        return identifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getLevel() {
        return level;
    }

    public DashboardDataService.SettingsData getSettings() {
        return settings;
    }

    public boolean uses24HourClock() {
        return "24hr".equalsIgnoreCase(settings.clockFormat());
    }

    public void refreshSettings() {
        reloadSettings();
        if (shell != null) {
            shell.applySettings(settings);
        }
    }

    public void previewBrightness(int brightness) {
        if (shell != null) {
            shell.applyBrightness(brightness);
        }
    }

    public void openMessaging() {
        new MessagingPage(this, identifier, level).start(stage);
    }

    public void openReminders() {
        new RemindersPage(this, identifier, level).start(stage);
    }

    public void openAppointments() {
        new clientUI.patient.AppointmentPage(this, identifier, level).start(stage);
    }

    public void openVoiceChat() {
        new VoiceChatPage(this, identifier, level).start(stage);
    }

    public void openWeather() {
        new WeatherPage(this, identifier, level).start(stage);
    }

    public void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Sign Out");
        alert.setHeaderText(null);
        alert.setContentText("Do you want to sign out of Care Connect?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new LoginGUI().start(stage);
        }
    }

    private void reloadSettings() {
        settings = DashboardDataService.getSettings();
    }
}
