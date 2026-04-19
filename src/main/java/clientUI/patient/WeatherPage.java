package clientUI.patient;

import java.util.Map;

import APIhandlers.OpenStreetMap;
import APIhandlers.OpenWeatherMap;
import clientUI.DashboardDataService;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class WeatherPage {
    private MainPage parent;
    private String username;
    private int level;

    private Label tempLabel;
    private Label subtitleLabel;
    private Label windLabel;
    private Label humidityLabel;
    private WebView mapView;

    public WeatherPage(MainPage parent, String username, int level) {
        this.parent = parent;
        this.username = username;
        this.level = level;
    }

    public void start(Stage stage) {
        stage.setTitle("Weather Test");

        HBox root = new HBox(0);
        root.setStyle("-fx-background-color: #f4f4f4;");
        root.setPrefSize(1024, 768);

        // Left panel - Weather info
        VBox leftPanel = new VBox(15);
        leftPanel.setPadding(new Insets(20));
        leftPanel.setStyle("-fx-background-color: #ffffff;");
        leftPanel.setPrefWidth(600);

        // Input section
        HBox inputBox = new HBox(10);
        inputBox.setAlignment(Pos.CENTER);
        TextField addressField = new TextField();
        addressField.setPromptText("Enter an address");
        addressField.setPrefWidth(100);
        Button getButton = new Button("Go");
        inputBox.getChildren().addAll(addressField, getButton);

        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Weather display
        tempLabel = new Label("--°C");
        tempLabel.setFont(Font.font("Arial", FontWeight.BOLD, 58));
        tempLabel.setStyle("-fx-text-fill: #333;");

        subtitleLabel = new Label("(condition) in (city)");
        subtitleLabel.setFont(Font.font("Arial", 28));
        subtitleLabel.setStyle("-fx-text-fill: #666;");

        windLabel = new Label("Winds -- mph");
        windLabel.setFont(Font.font("Arial", 26));

        humidityLabel = new Label("Humidity --%");
        humidityLabel.setFont(Font.font("Arial", 26));

        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> parent.start(stage));

        leftPanel.getChildren().addAll(inputBox, tempLabel, subtitleLabel, windLabel, humidityLabel, spacer, backBtn);

        // Right panel - Map
        mapView = new WebView();

        root.getChildren().addAll(leftPanel, mapView);
        HBox.setHgrow(mapView, Priority.ALWAYS);

        // Button action
        getButton.setOnAction(e -> {
            String address = addressField.getText().trim();
            if (address.isEmpty()) {
                showAlert("Error", "Please enter an address.");
                return;
            }
            try {
                Object[] coords = OpenWeatherMap.getCoords(address);
                double lat = (double) coords[0];
                double lon = (double) coords[1];
                String city = (String) coords[2];
                OpenWeatherMap owm = new OpenWeatherMap();
                Map<String, Object> weather = owm.getWeather(lat, lon);
                Float temp = (Float) weather.get("temperature");
                String condition = (String) weather.get("conditionTitle");
                Integer windSpeed = (Integer) weather.get("windSpeed");
                Integer humidity = (Integer) weather.get("humidity");
                tempLabel.setText(temp != null ? temp.intValue() + "°C" : "N/A");
                subtitleLabel.setText((condition != null ? condition : "unknown") + " in " + city);
                windLabel.setText("Winds " + (windSpeed != null ? windSpeed : 0) + " mph");
                humidityLabel.setText("Humidity " + (humidity != null ? humidity : 0) + "%");
                showMapAtLocation(lat, lon);
            } catch (Exception ex) {
                showAlert("Error", "Failed to get weather: " + ex.getMessage());
            }
        });

        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        
        mapView.getEngine().setUserAgent("HealthAssistant/1.0 (JavaFX WebView; OpenStreetMap)");
        
        stage.show();
        showDefaultMap();
        loadPatientDefaultWeather();
    }

    private void showDefaultMap() {
        showMapAtLocation(0, 0);
    }

    private void showMapAtLocation(double lat, double lon) {
        OpenStreetMap osm = new OpenStreetMap();
        String html = osm.getMapHTML(lat, lon);
        mapView.getEngine().loadContent(html);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadPatientDefaultWeather() {
        try {
            var weatherOpt = DashboardDataService.getCurrentWeather(username);
            if (weatherOpt.isEmpty()) {
                subtitleLabel.setText("No default patient location found");
                return;
            }

            var weather = weatherOpt.get();

            tempLabel.setText(weather.temperature() != null
                ? weather.temperature().intValue() + "°C"
                : "N/A");

            subtitleLabel.setText(weather.condition() + " in " + weather.location());
            windLabel.setText("Winds " + weather.windSpeed() + " mph");
            humidityLabel.setText("Humidity " + weather.humidity() + "%");

            Object[] coords = OpenWeatherMap.getCoords(weather.location());
            double lat = (double) coords[0];
            double lon = (double) coords[1];
            showMapAtLocation(lat, lon);

        } catch (Exception e) {
            subtitleLabel.setText("Default weather unavailable");
        }
    }
}
