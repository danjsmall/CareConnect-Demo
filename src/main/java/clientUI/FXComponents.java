package clientUI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public final class FXComponents {
    public static final Color PRIMARY = Color.web("#0f766e");
    public static final Color GREEN = Color.web("#15803d");
    public static final Color RED = Color.web("#b42318");
    public static final Color AMBER = Color.web("#c77d11");

    private FXComponents() {
    }

    public static VBox card(double topPad, double rightPad, double bottomPad, double leftPad) {
        VBox box = new VBox();
        box.getStyleClass().add("card");
        box.setPadding(new Insets(topPad, rightPad, bottomPad, leftPad));
        return box;
    }

    public static VBox paddedCard(Insets padding, String... extraClasses) {
        VBox box = new VBox();
        box.getStyleClass().add("card");
        box.getStyleClass().addAll(extraClasses);
        box.setPadding(padding);
        return box;
    }

    public static HBox cardWithBar(Color barColor, Node content) {
        Rectangle bar = new Rectangle(5, 1);
        bar.setFill(barColor);
        bar.setArcWidth(4);
        bar.setArcHeight(4);
        bar.heightProperty().bind(((Region) content).heightProperty());

        HBox outer = new HBox(bar, content);
        outer.getStyleClass().add("card");
        outer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
        return outer;
    }

    public static StackPane micButton(boolean blue, double size) {
        Circle bg = new Circle(size / 2);
        bg.getStyleClass().add(blue ? "mic-btn-blue" : "mic-btn-light");

        Label micIcon = new Label("🎤");
        micIcon.setStyle("-fx-font-size: " + (size * 0.6) + "px; -fx-text-fill: " + (blue ? "white" : PRIMARY.toString()) + ";");

        StackPane button = new StackPane(bg, micIcon);
        button.setPrefSize(size, size);
        button.setMaxSize(size, size);
        button.setMinSize(size, size);
        button.setCursor(javafx.scene.Cursor.HAND);
        return button;
    }


    public static HBox pillGroup(String[] options, int defaultIndex, PillSelectionListener listener) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Button[] buttons = new Button[options.length];
        for (int i = 0; i < options.length; i++) {
            Button button = new Button(options[i]);
            final int index = i;
            button.getStyleClass().add(i == defaultIndex ? "pill-btn-active" : "pill-btn");
            buttons[i] = button;
            button.setOnAction(event -> {
                for (Button sibling : buttons) {
                    sibling.getStyleClass().removeAll("pill-btn-active", "pill-btn");
                    sibling.getStyleClass().add("pill-btn");
                }
                button.getStyleClass().removeAll("pill-btn-active", "pill-btn");
                button.getStyleClass().add("pill-btn-active");
                if (listener != null) {
                    listener.onSelect(index, options[index]);
                }
            });
            row.getChildren().add(button);
        }
        return row;
    }

    public static Slider appSlider(int min, int max, int value) {
        Slider slider = new Slider(min, max, value);
        slider.getStyleClass().add("app-slider");
        slider.setMaxWidth(Double.MAX_VALUE);
        return slider;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        VBox.setVgrow(region, Priority.ALWAYS);
        return region;
    }

    public static VBox sectionIntro(String eyebrow, String title) {
        Label eyebrowLabel = new Label(eyebrow);
        eyebrowLabel.getStyleClass().add("section-kicker");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-heading");

        return new VBox(2, eyebrowLabel, titleLabel);
    }

    public static VBox metricCard(String eyebrow, String value, String title, String helper, String... extraClasses) {
        VBox card = paddedCard(new Insets(16), "metric-card");
        card.getStyleClass().addAll(extraClasses);
        card.setSpacing(6);

        Label eyebrowLabel = new Label(eyebrow);
        eyebrowLabel.getStyleClass().add("metric-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");
        valueLabel.setWrapText(true);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("metric-title");
        titleLabel.setWrapText(true);

        Label helperLabel = new Label(helper);
        helperLabel.getStyleClass().add("metric-helper");
        helperLabel.setWrapText(true);

        card.getChildren().addAll(eyebrowLabel, valueLabel, titleLabel, helperLabel);
        VBox.setVgrow(helperLabel, Priority.ALWAYS);
        return card;
    }

    public static VBox actionTile(String eyebrow, String title, String body, String... extraClasses) {
        VBox card = paddedCard(new Insets(16), "action-tile");
        card.getStyleClass().addAll(extraClasses);
        card.setSpacing(6);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setMaxWidth(Double.MAX_VALUE);

        Label eyebrowLabel = new Label(eyebrow);
        eyebrowLabel.getStyleClass().add("tile-kicker");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("tile-title");
        titleLabel.setWrapText(true);

        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("tile-body");
        bodyLabel.setWrapText(true);

        card.getChildren().addAll(eyebrowLabel, titleLabel, bodyLabel);
        VBox.setVgrow(bodyLabel, Priority.ALWAYS);
        return card;
    }

    public static HBox infoRow(String label, String value) {
        Label keyLabel = new Label(label);
        keyLabel.getStyleClass().add("info-row-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-row-value");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(10, keyLabel, valueLabel);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    public static Label statusPill(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("status-pill", styleClass);
        return label;
    }

    public static Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("btn-secondary");
        return button;
    }

    public static HBox equalWidthRow(Node... nodes) {
        HBox row = new HBox(12);
        for (Node node : nodes) {
            row.getChildren().add(node);
            HBox.setHgrow(node, Priority.ALWAYS);
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
        return row;
    }

    public static VBox emptyState(String title, String body) {
        VBox state = paddedCard(new Insets(18), "detail-card");
        state.setSpacing(8);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("empty-title");

        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("empty-body");
        bodyLabel.setWrapText(true);

        state.getChildren().addAll(titleLabel, bodyLabel);
        return state;
    }

    @FunctionalInterface
    public interface PillSelectionListener {
        void onSelect(int index, String value);
    }
}
