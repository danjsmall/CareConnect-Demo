package clientUI.operator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import LoginSystem.DatabaseManager;
import LoginSystem.LoginSystem;
import clientUI.LoginGUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class OperatorPage {

    private final String operatorName;
    private final int    level;
    private final String operatorEmail;
    private Stage stage;
    private BorderPane contentArea;

    public OperatorPage(String operatorName, int level, String operatorEmail) {
        this.operatorName  = operatorName;
        this.level         = level;
        this.operatorEmail = operatorEmail;
    }

    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("CareConnect — Operator Dashboard");

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());

        contentArea = new BorderPane();
        root.setCenter(contentArea);

        showOverview();

        Scene scene = new Scene(root, 1024, 768);
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setResizable(false);
        stage.show();
    }

    private HBox buildHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #7b2d8b;");

        Label title = new Label("CareConnect");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label roleTag = new Label("OPERATOR");
        roleTag.setStyle("-fx-background-color: #5a1f6b; -fx-text-fill: #e5c5f5; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox.setMargin(roleTag, new Insets(0, 0, 0, 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(operatorName);
        userLabel.setStyle("-fx-text-fill: #e5c5f5; -fx-font-size: 13px;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #5a1f6b; -fx-text-fill: white; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> handleLogout());
        HBox.setMargin(logoutBtn, new Insets(0, 0, 0, 12));

        header.getChildren().addAll(title, roleTag, spacer, userLabel, logoutBtn);
        return header;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dde3ea; -fx-border-width: 0 1 0 0;");

        sidebar.getChildren().addAll(
            sideBtn("Overview",         () -> showOverview()),
            sideBtn("Patient Manager",  () -> showPatientManager()),
            sideBtn("Doctor Manager",   () -> showDoctorManager()),
            sideBtn("Operator Manager", () -> showOperatorManager()),
            sideBtn("Account Manager",  () -> showAccountManager())
        );
        return sidebar;
    }

    private Button sideBtn(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 10 14; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 13px;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void showOverview() {
        VBox view = new VBox(24);
        view.setPadding(new Insets(30));

        Label heading = new Label("Overview");
        heading.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        HBox stats = new HBox(20);
        stats.getChildren().addAll(
            statCard("Patients",  queryCount("SELECT COUNT(*) FROM patients"),       "#1a6b9a"),
            statCard("Doctors",   queryCount("SELECT COUNT(*) FROM doctors"),        "#27ae60"),
            statCard("Accounts",  queryCount("SELECT COUNT(*) FROM user_passwords"), "#7b2d8b")
        );

        Label recentHeading = new Label("Recent Logins");
        recentHeading.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        TableView<String[]> loginTable = new TableView<>();
        loginTable.setMaxHeight(240);

        TableColumn<String[], String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        emailCol.setPrefWidth(260);

        TableColumn<String[], String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        roleCol.setPrefWidth(90);

        TableColumn<String[], String> loginCol = new TableColumn<>("Last Login");
        loginCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        loginCol.setPrefWidth(180);

        loginTable.getColumns().addAll(emailCol, roleCol, loginCol);
        loginTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT email, role, last_login_at FROM user_passwords WHERE last_login_at IS NOT NULL ORDER BY last_login_at DESC LIMIT 20");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                loginTable.getItems().add(new String[]{
                    rs.getString("email"), rs.getString("role"), rs.getString("last_login_at")});
            }
        } catch (SQLException ex) { }

        view.getChildren().addAll(heading, stats, recentHeading, loginTable);

        ScrollPane scroll = new ScrollPane(view);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.setCenter(scroll);
    }

    private VBox statCard(String label, String count, String colour) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 30, 20, 30));
        card.setStyle("-fx-background-color: " + colour + "; -fx-background-radius: 10;");
        Label num = new Label(count);
        num.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.85);");
        card.getChildren().addAll(num, lbl);
        return card;
    }

    private String queryCount(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() ? String.valueOf(rs.getInt(1)) : "?";
        } catch (SQLException e) { return "?"; }
    }

    private void showPatientManager() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(20));

        Label heading = new Label("Patient Manager");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by name, ID or email...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #7b2d8b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");

        HBox searchBar = new HBox(8, searchField, searchBtn);

        TableView<String[]> table = buildPatientTable();
        loadAllPatients(table);

        searchBtn.setOnAction(e -> filterPatients(table, searchField.getText().trim()));
        searchField.setOnAction(e -> filterPatients(table, searchField.getText().trim()));

        Button editBtn = new Button("Edit Selected Patient");
        editBtn.setStyle("-fx-background-color: #1a6b9a; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { showError("Select a patient first."); return; }
            showEditPatientDialog(row[0], table);
        });

        view.getChildren().addAll(heading, searchBar, table, editBtn);
        VBox.setVgrow(table, Priority.ALWAYS);
        contentArea.setCenter(view);
    }

    @SuppressWarnings("unchecked")
    private TableView<String[]> buildPatientTable() {
        TableView<String[]> table = new TableView<>();
        String[] cols   = {"ID", "Name", "Age", "City", "Email", "GP"};
        int[]    widths = {90, 180, 50, 110, 200, 160};
        for (int i = 0; i < cols.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(cols[i]);
            col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().length > idx ? d.getValue()[idx] : ""));
            col.setPrefWidth(widths[i]);
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void loadAllPatients(TableView<String[]> table) {
        table.getItems().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, age, city, email, gp_name FROM patients ORDER BY name LIMIT 200");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString("id"), rs.getString("name"),
                    String.valueOf(rs.getInt("age")), rs.getString("city"),
                    rs.getString("email"), rs.getString("gp_name")});
            }
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }
    }

    private void filterPatients(TableView<String[]> table, String q) {
        if (q.isEmpty()) { loadAllPatients(table); return; }
        table.getItems().clear();
        String like = "%" + q + "%";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, age, city, email, gp_name FROM patients WHERE name LIKE ? OR id LIKE ? OR email LIKE ? LIMIT 100");
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString("id"), rs.getString("name"),
                    String.valueOf(rs.getInt("age")), rs.getString("city"),
                    rs.getString("email"), rs.getString("gp_name")});
            }
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }
    }

    private void showEditPatientDialog(String patientId, TableView<String[]> table) {
        String[] current = {"", "", "", ""};
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT name, address_line1, city, telephone FROM patients WHERE id = ? LIMIT 1");
            ps.setString(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                current[0] = rs.getString("name");
                current[1] = rs.getString("address_line1");
                current[2] = rs.getString("city");
                current[3] = rs.getString("telephone");
            }
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Patient — " + patientId);
        dialog.setHeaderText("Modify patient details.");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(current[0]);
        TextField addrField = new TextField(current[1] != null ? current[1] : "");
        TextField cityField = new TextField(current[2] != null ? current[2] : "");
        TextField telField  = new TextField(current[3] != null ? current[3] : "");

        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Address:"),   0, 1); grid.add(addrField, 1, 1);
        grid.add(new Label("City:"),      0, 2); grid.add(cityField, 1, 2);
        grid.add(new Label("Telephone:"), 0, 3); grid.add(telField,  1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "UPDATE patients SET name = ?, address_line1 = ?, city = ?, telephone = ? WHERE id = ?");
                ps.setString(1, nameField.getText().trim());
                ps.setString(2, addrField.getText().trim());
                ps.setString(3, cityField.getText().trim());
                ps.setString(4, telField.getText().trim());
                ps.setString(5, patientId);
                ps.executeUpdate();
                showInfo("Saved", "Patient record updated.");
                loadAllPatients(table);
            } catch (SQLException ex) { showError("Save failed: " + ex.getMessage()); }
        }
    }

    private void showEditDoctorDialog(String email, TableView<String[]> table) {
        String currentName = "";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM doctors WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentName = rs.getString("name");
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Doctor");
        dialog.setHeaderText("Update details for doctor.");

        TextField emailField = new TextField(email);
        emailField.setPromptText("Email Address");
        TextField nameField = new TextField(currentName);
        nameField.setPromptText("Full Name");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newEmail = emailField.getText().trim();
            String newName = nameField.getText().trim();

            if (newEmail.isEmpty() || newName.isEmpty()) {
                showError("Email and Name cannot be empty.");
                return;
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    // 1. Update doctors table
                    PreparedStatement psDoc = conn.prepareStatement("UPDATE doctors SET name = ?, email = ? WHERE email = ?");
                    psDoc.setString(1, newName);
                    psDoc.setString(2, newEmail);
                    psDoc.setString(3, email);
                    psDoc.executeUpdate();

                    // 2. Update user_passwords table if it exists
                    PreparedStatement psPass = conn.prepareStatement("UPDATE user_passwords SET email = ? WHERE email = ?");
                    psPass.setString(1, newEmail);
                    psPass.setString(2, email);
                    psPass.executeUpdate();

                    conn.commit();
                    showInfo("Saved", "Doctor record updated.");
                    showDoctorManager(); // Refresh view
                } catch (SQLException ex) {
                    conn.rollback();
                    showError("Save failed: " + ex.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }
        }
    }

    private void showEditOperatorDialog(String email, TableView<String[]> table) {
        String currentName = "";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT name FROM administrators WHERE email = ?");
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) currentName = rs.getString("name");
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Operator");
        dialog.setHeaderText("Update details for operator.");

        TextField emailField = new TextField(email);
        emailField.setPromptText("Email Address");
        TextField nameField = new TextField(currentName);
        nameField.setPromptText("Full Name");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String newEmail = emailField.getText().trim();
            String newName = nameField.getText().trim();

            if (newEmail.isEmpty() || newName.isEmpty()) {
                showError("Email and Name cannot be empty.");
                return;
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    PreparedStatement psAdm = conn.prepareStatement("UPDATE administrators SET name = ?, email = ? WHERE email = ?");
                    psAdm.setString(1, newName);
                    psAdm.setString(2, newEmail);
                    psAdm.setString(3, email);
                    psAdm.executeUpdate();

                    PreparedStatement psPass = conn.prepareStatement("UPDATE user_passwords SET email = ? WHERE email = ?");
                    psPass.setString(1, newEmail);
                    psPass.setString(2, email);
                    psPass.executeUpdate();

                    conn.commit();
                    showInfo("Saved", "Operator record updated.");
                    showOperatorManager();
                } catch (SQLException ex) {
                    conn.rollback();
                    showError("Save failed: " + ex.getMessage());
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }
        }
    }

    private void showOperatorManager() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(20));

        Label heading = new Label("Operator Manager");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<String[]> table = new TableView<>();

        TableColumn<String[], String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        emailCol.setPrefWidth(260);

        TableColumn<String[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        nameCol.setPrefWidth(200);

        TableColumn<String[], String> lastCol = new TableColumn<>("Last Login");
        lastCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        lastCol.setPrefWidth(180);

        table.getColumns().addAll(emailCol, nameCol, lastCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT a.email, a.name, u.last_login_at FROM administrators a LEFT JOIN user_passwords u ON a.email = u.email ORDER BY a.name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString("email"), rs.getString("name"),
                    rs.getString("last_login_at") != null ? rs.getString("last_login_at") : "Never"});
            }
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }

        view.getChildren().addAll(heading, table);

        Button editBtn = new Button("Edit Selected Operator");
        editBtn.setStyle("-fx-background-color: #1a6b9a; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { showError("Select an operator first."); return; }
            showEditOperatorDialog(row[0], table);
        });

        view.getChildren().addAll(editBtn);
        VBox.setVgrow(table, Priority.ALWAYS);
        contentArea.setCenter(view);
    }

    private void showDoctorManager() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(20));

        Label heading = new Label("Doctor Manager");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TableView<String[]> table = new TableView<>();

        TableColumn<String[], String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        emailCol.setPrefWidth(260);

        TableColumn<String[], String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        nameCol.setPrefWidth(200);

        TableColumn<String[], String> lastCol = new TableColumn<>("Last Login");
        lastCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        lastCol.setPrefWidth(180);

        table.getColumns().addAll(emailCol, nameCol, lastCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT d.email, d.name, u.last_login_at FROM doctors d LEFT JOIN user_passwords u ON d.email = u.email ORDER BY d.name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                table.getItems().add(new String[]{
                    rs.getString("email"), rs.getString("name"),
                    rs.getString("last_login_at") != null ? rs.getString("last_login_at") : "Never"});
            }
        } catch (SQLException ex) { showError("DB error: " + ex.getMessage()); }

        view.getChildren().addAll(heading, table);

        Button editBtn = new Button("Edit Selected Doctor");
        editBtn.setStyle("-fx-background-color: #1a6b9a; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) { showError("Select a doctor first."); return; }
            showEditDoctorDialog(row[0], table);
        });

        view.getChildren().addAll(editBtn);
        VBox.setVgrow(table, Priority.ALWAYS);
        contentArea.setCenter(view);
    }

    private void showAccountManager() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(20));

        Label heading = new Label("Account Manager");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.setPadding(new Insets(14));
        form.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-background-radius: 6;");

        TextField     emailField   = new TextField();   emailField.setPromptText("user@example.com");
        PasswordField passField    = new PasswordField(); passField.setPromptText("Password");
        TextField     displayField = new TextField();   displayField.setPromptText("Full name");
        TextField     patIdField   = new TextField();   patIdField.setPromptText("Patient ID (patients only)");
        ComboBox<String> roleBox   = new ComboBox<>();
        roleBox.getItems().addAll("patient", "doctor", "operator");
        roleBox.setValue("patient");

        form.add(new Label("Email:"),        0, 0); form.add(emailField,   1, 0);
        form.add(new Label("Password:"),     0, 1); form.add(passField,    1, 1);
        form.add(new Label("Display Name:"), 0, 2); form.add(displayField, 1, 2);
        form.add(new Label("Role:"),         0, 3); form.add(roleBox,      1, 3);
        form.add(new Label("Patient ID:"),   0, 4); form.add(patIdField,   1, 4);

        Button createBtn = new Button("Create Account");
        createBtn.setStyle("-fx-background-color: #7b2d8b; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        form.add(createBtn, 1, 5);

        TableView<String[]> accountTable = new TableView<>();
        TableColumn<String[], String> aEmail = new TableColumn<>("Email");
        aEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        aEmail.setPrefWidth(240);
        TableColumn<String[], String> aRole = new TableColumn<>("Role");
        aRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        aRole.setPrefWidth(90);
        TableColumn<String[], String> aCreated = new TableColumn<>("Created");
        aCreated.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        aCreated.setPrefWidth(160);
        accountTable.getColumns().addAll(aEmail, aRole, aCreated);
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Runnable refreshTable = () -> {
            accountTable.getItems().clear();
            try (Connection conn = DatabaseManager.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "SELECT email, role, created_at FROM user_passwords ORDER BY created_at DESC");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    accountTable.getItems().add(new String[]{
                        rs.getString("email"), rs.getString("role"), rs.getString("created_at")});
                }
            } catch (SQLException ex) { }
        };
        refreshTable.run();

        createBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String pass  = passField.getText();
            String role  = roleBox.getValue();
            String patId = patIdField.getText().trim();
            if (email.isEmpty() || pass.isEmpty()) { showError("Email and password are required."); return; }
            boolean ok;
            if ("patient".equals(role)) {
                ok = LoginSystem.register(email, pass, patId.isEmpty() ? null : patId);
            } else {
                int lvl = "operator".equals(role) ? 3 : 2;
                ok = registerRoleAccount(email, pass, role, lvl, displayField.getText().trim());
            }
            if (ok) {
                showInfo("Created", "Account for " + email + " (" + role + ") created.");
                emailField.clear(); passField.clear(); displayField.clear(); patIdField.clear();
                refreshTable.run();
            } else {
                showError("Failed — email may already exist.");
            }
        });

        view.getChildren().addAll(heading, form, accountTable);
        VBox.setVgrow(accountTable, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane(view);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.setCenter(scroll);
    }

    private boolean registerRoleAccount(String email, String password, String role, int level, String displayName) {
        try {
            java.security.SecureRandom sr = java.security.SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16]; sr.nextBytes(salt);
            String saltB64 = java.util.Base64.getEncoder().encodeToString(salt);
            javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 100_000, 256);
            String hash = java.util.Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());

            boolean inserted = DatabaseManager.insertUser(email, hash, saltB64, role, level, null);
            if (!inserted) return false;

            String name = displayName.isEmpty() ? email : displayName;
            String profileSql = "doctor".equals(role)
                ? "INSERT OR IGNORE INTO doctors (email, name) VALUES (?, ?)"
                : "INSERT OR IGNORE INTO administrators (email, name) VALUES (?, ?)";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(profileSql)) {
                ps.setString(1, email); ps.setString(2, name);
                ps.executeUpdate();
            }
            return true;
        } catch (Exception ex) { return false; }
    }

    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Logout");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new LoginGUI().start(stage);
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

