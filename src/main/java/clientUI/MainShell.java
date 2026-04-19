package clientUI;

import clientUI.patient.HomeScreen;
import clientUI.patient.MedsScreen;
import clientUI.patient.SettingsScreen;
import clientUI.patient.VitalsScreen;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainShell extends BorderPane {
    private final MainPage app;
    private final StackPane screenArea;
    private final NavButton[] navButtons;
    private final ColorAdjust brightnessAdjust = new ColorAdjust();

    private final HomeScreen homeScreen;
    private final MedsScreen medsScreen;
    private final VitalsScreen vitalsScreen;
    private final SettingsScreen settingsScreen;

    public MainShell(MainPage app) {
        this.app = app;
        setPadding(new Insets(0, 0, 8, 0));
        getStyleClass().add("theme-light");

        homeScreen = new HomeScreen(app, () -> activateScreen(3));
        medsScreen = new MedsScreen(app);
        vitalsScreen = new VitalsScreen(app);
        settingsScreen = new SettingsScreen(app);

        screenArea = new StackPane(homeScreen, medsScreen, vitalsScreen, settingsScreen);
        screenArea.setAlignment(Pos.TOP_LEFT);
        setCenter(screenArea);

        HBox nav = buildNav();
        navButtons = (NavButton[]) nav.getUserData();
        setBottom(nav);
        BorderPane.setMargin(nav, new Insets(0, 14, 8, 14));

        activateScreen(0);
    }

    public void applySettings(DashboardDataService.SettingsData settings) {
        getStyleClass().removeAll("theme-light", "theme-dark");
        getStyleClass().add("dark".equalsIgnoreCase(settings.theme()) ? "theme-dark" : "theme-light");
        applyBrightness(settings.brightness());
        refreshScreens();
    }

    public void applyBrightness(int brightness) {
        double normalizedBrightness = Math.max(-0.45, Math.min(0.25, (brightness - 55) / 125.0));
        brightnessAdjust.setBrightness(normalizedBrightness);
        setEffect(brightnessAdjust);
    }

    private HBox buildNav() {
        HBox bar = new HBox();
        bar.getStyleClass().add("nav-bar");
        bar.setPadding(new Insets(6, 8, 8, 8));
        bar.setPrefHeight(76);
        bar.setAlignment(Pos.CENTER);

        String[][] tabs = {
            {"⌂", "Home"},
            {"Rx", "Meds"},
            {"♥", "Vitals"},
            {"⚙", "Settings"}
        };
        Node[] screens = {homeScreen, medsScreen, vitalsScreen, settingsScreen};
        NavButton[] buttons = new NavButton[tabs.length];

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            buttons[i] = new NavButton(tabs[i][0], tabs[i][1]);
            buttons[i].setOnMouseClicked(event -> activateScreen(index));
            buttons[i].setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(buttons[i], Priority.ALWAYS);
            bar.getChildren().add(buttons[i]);
        }

        bar.setUserData(buttons);
        return bar;
    }

    private void activateScreen(int index) {
        Node[] screens = {homeScreen, medsScreen, vitalsScreen, settingsScreen};
        for (int i = 0; i < screens.length; i++) {
            boolean visible = i == index;
            screens[i].setVisible(visible);
            screens[i].setManaged(visible);
            navButtons[i].setActive(visible);
        }

        switch (index) {
            case 0 -> homeScreen.refresh();
            case 1 -> medsScreen.refresh();
            case 2 -> vitalsScreen.refresh();
            case 3 -> settingsScreen.refresh();
            default -> {
            }
        }
    }

    private void refreshScreens() {
        homeScreen.refresh();
        medsScreen.refresh();
        vitalsScreen.refresh();
        settingsScreen.refresh();
    }

    private static final class NavButton extends VBox {
        private final Label iconLabel;
        private final Label textLabel;

        private NavButton(String icon, String text) {
            setAlignment(Pos.CENTER);
            setSpacing(4);
            setPadding(new Insets(8, 4, 8, 4));
            setStyle("-fx-background-radius: 14;");
            setCursor(javafx.scene.Cursor.HAND);

            iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("nav-icon");

            textLabel = new Label(text);
            textLabel.getStyleClass().add("nav-btn-label");

            getChildren().addAll(iconLabel, textLabel);
        }

        private void setActive(boolean active) {
            if (active) {
                iconLabel.getStyleClass().remove("nav-icon-active");
                iconLabel.getStyleClass().add("nav-icon-active");
                textLabel.getStyleClass().removeAll("nav-btn-label", "nav-btn-label-active");
                textLabel.getStyleClass().add("nav-btn-label-active");
                setStyle("-fx-background-color: #eef8f6; -fx-background-radius: 14;");
            } else {
                iconLabel.getStyleClass().remove("nav-icon-active");
                textLabel.getStyleClass().removeAll("nav-btn-label", "nav-btn-label-active");
                textLabel.getStyleClass().add("nav-btn-label");
                setStyle("-fx-background-color: transparent; -fx-background-radius: 14;");
            }
        }
    }
}
