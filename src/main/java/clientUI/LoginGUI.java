package clientUI;

import LoginSystem.LoginSystem;
import clientUI.doctor.DoctorPage;
import clientUI.operator.OperatorPage;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginGUI extends Application {
    private TextField usernameField;
    private PasswordField passwordField;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("Care Connect");

        VBox root = new VBox(16);
        root.getStyleClass().add("login-shell");
        root.setAlignment(Pos.CENTER);

        root.getChildren().add(buildFormCard());

        Scene scene = new Scene(root, 1024, 768);
        var stylesheet = getClass().getResource("/styles.css");
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }

        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }

    private VBox buildFormCard() {
        VBox card = FXComponents.paddedCard(new Insets(20), "detail-card");
        card.setSpacing(14);
        card.setMaxWidth(420);

        Label title = new Label("Sign in");
        title.getStyleClass().add("section-heading");

        Label userLabel = new Label("Email");
        userLabel.getStyleClass().add("bold-text");

        usernameField = new TextField();
        usernameField.setPromptText("Enter your email");
        usernameField.getStyleClass().add("app-field");

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("bold-text");

        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("app-field");

        Button loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("btn-primary");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(event -> doLogin());

        Button registerButton = FXComponents.secondaryButton("Create Account");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setOnAction(event -> doRegister());

        HBox actions = new HBox(10, loginButton, registerButton);
        HBox.setHgrow(loginButton, Priority.ALWAYS);
        HBox.setHgrow(registerButton, Priority.ALWAYS);

        card.getChildren().addAll(
            title,
            userLabel,
            usernameField,
            passwordLabel,
            passwordField,
            actions
        );
        return card;
    }

    private void doLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter your email and password.");
            return;
        }

        LoginSystem.User u = LoginSystem.authenticate(user, pass);
        if (u != null) {
            String identifier = (u.patientId != null) ? u.patientId : user;
            String displayName = (u.displayName != null) ? u.displayName : user;
            if (u.level >= 3) {
                new OperatorPage(displayName, u.level, user).start(stage);
            } else if (u.level == 2) {
                new DoctorPage(displayName, u.level, user).start(stage);
            } else {
                new MainPage(displayName, u.level, identifier).start(stage);
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid email or password.");
        }
    }

    private void doRegister() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Required", "Enter an email and password to create an account.");
            return;
        }

        boolean registered = LoginSystem.register(user, pass);
        if (registered) {
            int level = LoginSystem.getLevel(user);
            showAlert(Alert.AlertType.INFORMATION, "Account Created", "User " + user + " was registered with access level " + level + ".");
        } else {
            showAlert(Alert.AlertType.ERROR, "Registration Failed", "That email is unavailable or invalid.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
