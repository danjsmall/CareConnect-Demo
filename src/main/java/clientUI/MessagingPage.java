package clientUI;

import clientUI.MainPage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MessagingPage {
    private MainPage parent;
    private String username;
    private int level;

    public MessagingPage(MainPage parent, String username, int level) {
        this.parent = parent;
        this.username = username;
        this.level = level;
    }

    public void start(Stage stage) {
        stage.setTitle("Messaging with NHS Page");

        BorderPane root = new BorderPane();

        VBox center = new VBox(20);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));

        Label header = new Label("Messaging with NHS (blank page)");
        header.setStyle("-fx-font-size: 18px;");

        center.getChildren().add(header);
        root.setCenter(center);

        VBox bottom = new VBox(10);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10));

        Button backBtn = new Button("Back");
        backBtn.setOnAction(e -> parent.start(stage));
        bottom.getChildren().add(backBtn);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }
}
