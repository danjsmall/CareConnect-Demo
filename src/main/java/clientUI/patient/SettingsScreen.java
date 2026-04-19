package clientUI.patient;

import clientUI.DashboardDataService;
import clientUI.FXComponents;
import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class SettingsScreen extends VBox {
    private final MainPage app;

    private int volume;
    private int brightness;
    private String textSize;
    private String theme;
    private int screenTimeout;
    private String clockFormat;

    public SettingsScreen(MainPage app) {
        this.app = app;

        DashboardDataService.SettingsData settings = DashboardDataService.getSettings();
        volume = settings.volume();
        brightness = settings.brightness();
        textSize = settings.textSize();
        theme = settings.theme();
        screenTimeout = settings.screenTimeout();
        clockFormat = settings.clockFormat();

        rebuild();
    }

    public void refresh() {
        DashboardDataService.SettingsData settings = DashboardDataService.getSettings();
        volume = settings.volume();
        brightness = settings.brightness();
        textSize = settings.textSize();
        theme = settings.theme();
        screenTimeout = settings.screenTimeout();
        clockFormat = settings.clockFormat();
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
        VBox root = new VBox(16);
        root.getStyleClass().add("screen-content");

        root.getChildren().addAll(
            buildHeroCard(),
            FXComponents.equalWidthRow(
                FXComponents.metricCard("VOLUME", volume + "%", "Notification audio", "Adjust sound prompts and alerts.", "metric-card-primary"),
                FXComponents.metricCard(
                    "DISPLAY",
                    prettyTheme(),
                    "Clock: " + clockFormat,
                    "Brightness: " + brightness + "%",
                    "metric-card-neutral"
                )
            ),
            FXComponents.sectionIntro(
                "DEVICE",
                "Comfort and visibility"
            ),
            buildDeviceCard(),
            FXComponents.sectionIntro(
                "APPEARANCE",
                "Display preferences"
            ),
            buildDisplayCard(),
            buildAccountCard()
        );
        return root;
    }

    private VBox buildHeroCard() {
        VBox card = FXComponents.paddedCard(new Insets(20), "hero-card");
        card.setSpacing(14);

        Label title = new Label("Device settings");
        title.getStyleClass().add("hero-title");

        Label subtitle = new Label("Current preferences are stored locally on this device for a simpler experience.");
        subtitle.getStyleClass().add("hero-body");
        subtitle.setWrapText(true);

        StackPane micButton = FXComponents.micButton(false, 46);
        micButton.setOnMouseClicked(event -> app.openVoiceChat());

        HBox header = new HBox(12);
        VBox textBlock = new VBox(4, title, subtitle);
        textBlock.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBlock, Priority.ALWAYS);
        header.getChildren().addAll(textBlock, micButton);

        FlowPane chips = new FlowPane();
        chips.setHgap(8);
        chips.setVgap(8);
        chips.getChildren().addAll(
            FXComponents.statusPill(prettyTheme(), "status-pill-neutral"),
            FXComponents.statusPill(clockFormat, "status-pill-neutral")
        );

        card.getChildren().addAll(header, chips);
        return card;
    }

    private VBox buildDeviceCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(16);

        Label volumeValue = new Label(volume + "%");
        volumeValue.getStyleClass().add("metric-title");
        Slider volumeSlider = FXComponents.appSlider(0, 100, volume);
        volumeSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            volume = (int) Math.round(newValue.doubleValue());
            volumeValue.setText(volume + "%");
            persistSettings();
        });

        Label brightnessValue = new Label(brightness + "%");
        brightnessValue.getStyleClass().add("metric-title");
        Slider brightnessSlider = FXComponents.appSlider(0, 100, brightness);
        brightnessSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            brightness = (int) Math.round(newValue.doubleValue());
            brightnessValue.setText(brightness + "%");
            app.previewBrightness(brightness);
            persistSettings();
        });

        card.getChildren().addAll(
            settingBlock("Volume", "Adjust notification sounds and spoken prompts.", volumeValue, volumeSlider),
            new Separator(),
            settingBlock("Brightness", "Increase or reduce screen brightness for comfort.", brightnessValue, brightnessSlider)
        );
        return card;
    }

    private VBox buildDisplayCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(16);

        HBox clockPills = FXComponents.pillGroup(
            new String[]{"12hr", "24hr"},
            "24hr".equalsIgnoreCase(clockFormat) ? 1 : 0,
            (index, value) -> {
                clockFormat = value;
                persistSettings();
                app.refreshSettings();
            }
        );

        HBox themePills = FXComponents.pillGroup(
            new String[]{"Light", "Dark"},
            "dark".equalsIgnoreCase(theme) ? 1 : 0,
            (index, value) -> {
                theme = value.toLowerCase();
                persistSettings();
                app.refreshSettings();
            }
        );

        card.getChildren().addAll(
            controlBlock("Clock format", "Choose between 12-hour and 24-hour time.", clockPills),
            new Separator(),
            controlBlock("Theme", "Select the appearance used on this device.", themePills)
        );
        return card;
    }

    private VBox buildAccountCard() {
        VBox card = FXComponents.paddedCard(new Insets(18), "detail-card");
        card.setSpacing(12);

        Label title = new Label("Account and support");
        title.getStyleClass().add("section-heading");

        Label subtitle = new Label(
            "Signed in as " + app.getDisplayName() + ". Use voice assistant for hands-free help, or sign out when finished."
        );
        subtitle.getStyleClass().add("section-copy");
        subtitle.setWrapText(true);

        Label identifier = FXComponents.statusPill(app.getIdentifier(), "status-pill-neutral");

        HBox actions = FXComponents.equalWidthRow(
            buildActionButton("Voice Assistant", false, app::openVoiceChat),
            buildActionButton("Log Out", true, app::logout)
        );

        card.getChildren().addAll(title, subtitle, identifier, actions);
        return card;
    }

    private Button buildActionButton(String text, boolean primary, Runnable action) {
        Button button = primary ? new Button(text) : FXComponents.secondaryButton(text);
        if (primary) {
            button.getStyleClass().add("btn-primary");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> action.run());
        return button;
    }

    private VBox settingBlock(String title, String description, Label valueLabel, Slider slider) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-heading");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("helper-text");
        descriptionLabel.setWrapText(true);

        HBox header = new HBox(12, titleLabel, FXComponents.spacer(), valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        return new VBox(8, header, descriptionLabel, slider);
    }

    private VBox controlBlock(String title, String description, HBox control) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-heading");

        Label descriptionLabel = new Label(description);
        descriptionLabel.getStyleClass().add("helper-text");
        descriptionLabel.setWrapText(true);

        return new VBox(8, titleLabel, descriptionLabel, control);
    }

    private String prettyTheme() {
        return "dark".equalsIgnoreCase(theme) ? "Dark theme" : "Light theme";
    }

    private void persistSettings() {
        DashboardDataService.saveSettings(new DashboardDataService.SettingsData(
            volume,
            brightness,
            textSize,
            theme,
            screenTimeout,
            clockFormat
        ));
    }
}
