package clientUI.doctor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import LoginSystem.DatabaseManager;
import clientUI.LoginGUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.GridPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ScrollPane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DoctorPage {

    private final String doctorEmail;
    private final String doctorName;
    private final int level;
    private Stage stage;

    private TableView<PatientRow> patientTable;
    private TextField searchField;
    private VBox detailContent;
    private Label detailTitle;

    public DoctorPage(String doctorName, int level, String doctorEmail) {
        this.doctorName  = doctorName;
        this.level       = level;
        this.doctorEmail = doctorEmail;
    }

    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("CareConnect — Doctor Dashboard");

        BorderPane root = new BorderPane();
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildMainContent());

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
        header.setStyle("-fx-background-color: #1a6b9a;");

        Label title = new Label("CareConnect");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label roleTag = new Label("DOCTOR");
        roleTag.setStyle("-fx-background-color: #0d4f75; -fx-text-fill: #b8ddf5; -fx-padding: 3 8 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
        HBox.setMargin(roleTag, new Insets(0, 0, 0, 10));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userLabel = new Label(doctorName);
        userLabel.setStyle("-fx-text-fill: #d0e8f7; -fx-font-size: 13px;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #0d4f75; -fx-text-fill: white; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> handleLogout());
        HBox.setMargin(logoutBtn, new Insets(0, 0, 0, 12));

        header.getChildren().addAll(title, roleTag, spacer, userLabel, logoutBtn);
        return header;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(8);
        sidebar.setPadding(new Insets(20, 10, 20, 10));
        sidebar.setPrefWidth(190);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dde3ea; -fx-border-width: 0 1 0 0;");

        sidebar.getChildren().addAll(
            sideBtn("Patient Search", () -> clearDetail()),
            sideBtn("My Patients",    () -> loadMyPatients())
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

    private SplitPane buildMainContent() {
        VBox left = new VBox(14);
        left.setPadding(new Insets(20));

        Label heading = new Label("Patient Search");
        heading.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        searchField = new TextField();
        searchField.setPromptText("Name, Patient ID or Email...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.setStyle("-fx-background-color: #1a6b9a; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        searchBtn.setOnAction(e -> runSearch(searchField.getText().trim()));
        searchField.setOnAction(e -> runSearch(searchField.getText().trim()));

        HBox searchBar = new HBox(8, searchField, searchBtn);

        patientTable = buildPatientTable();

        left.getChildren().addAll(heading, searchBar, patientTable);
        VBox.setVgrow(patientTable, Priority.ALWAYS);

        VBox right = new VBox(10);
        right.setPadding(new Insets(20));
        detailTitle = new Label("Select a patient to view details");
        detailTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");
        detailContent = new VBox(8);
        ScrollPane detailScroll = new ScrollPane(detailContent);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(detailScroll, Priority.ALWAYS);
        right.getChildren().addAll(detailTitle, new Separator(), detailScroll);

        SplitPane split = new SplitPane(left, right);
        split.setDividerPositions(0.45);
        return split;
    }

    @SuppressWarnings("unchecked")
    private TableView<PatientRow> buildPatientTable() {
        TableView<PatientRow> table = new TableView<>();
        table.setPlaceholder(new Label("Search for a patient above"));

        TableColumn<PatientRow, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().id));
        idCol.setPrefWidth(90);

        TableColumn<PatientRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().name));
        nameCol.setPrefWidth(180);

        TableColumn<PatientRow, String> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().age));
        ageCol.setPrefWidth(50);

        TableColumn<PatientRow, String> gpCol = new TableColumn<>("GP");
        gpCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().gp));
        gpCol.setPrefWidth(150);

        table.getColumns().addAll(idCol, nameCol, ageCol, gpCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) showPatientDetail(sel.id);
        });

        return table;
    }

    private void clearDetail() {
        searchField.clear();
        patientTable.getItems().clear();
        detailTitle.setText("Select a patient to view details");
        detailContent.getChildren().clear();
    }

    private void loadMyPatients() {
        patientTable.getItems().clear();
        detailContent.getChildren().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, age, gp_name FROM patients WHERE gp_name LIKE ? LIMIT 100");
            ps.setString(1, "%" + doctorName + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                patientTable.getItems().add(new PatientRow(
                    rs.getString("id"), rs.getString("name"),
                    String.valueOf(rs.getInt("age")), rs.getString("gp_name")));
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void runSearch(String query) {
        if (query.isEmpty()) return;
        patientTable.getItems().clear();
        String like = "%" + query + "%";
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, age, gp_name FROM patients WHERE name LIKE ? OR id LIKE ? OR email LIKE ? LIMIT 50");
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                patientTable.getItems().add(new PatientRow(
                    rs.getString("id"), rs.getString("name"),
                    String.valueOf(rs.getInt("age")), rs.getString("gp_name")));
            }
        } catch (SQLException ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showPatientDetail(String patientId) {
        detailContent.getChildren().clear();
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM patients WHERE id = ? LIMIT 1");
            ps.setString(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { detailTitle.setText("Patient not found"); return; }

            detailTitle.setText(rs.getString("name") + "  [" + patientId + "]");

            // 1. Profile Section
            VBox profileBox = card("Profile");
            String age = str(rs, "age");
            String gender = str(rs, "gender");
            String addr = str(rs, "address_line1");
            String city = str(rs, "city");
            String postcode = str(rs, "postcode");
            String tel = str(rs, "telephone");
            String email = str(rs, "email");
            String gp = str(rs, "gp_name");
            String gpAddr = str(rs, "gp_address");

            String fullAddr = addr + (city.equals("—") ? "" : ", " + city);

            profileBox.getChildren().addAll(
                row("Age",        age),
                row("Gender",     gender),
                row("Address",    fullAddr),
                row("Postcode",   postcode),
                row("Telephone",  tel),
                row("Email",      email),
                row("GP",         gp),
                row("GP Address", gpAddr)
            );

            // Add Edit button if doctor is assigned
            if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                Button editBtn = new Button("Edit Patient Details");
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                editBtn.setOnAction(e -> showEditPatientDialog(patientId));
                profileBox.getChildren().add(editBtn);
            }

            detailContent.getChildren().add(profileBox);

            // 2. Conditions Section
            VBox condHeader = new VBox(5);
            Label condTitle = new Label("Conditions");
            condTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a6b9a;");
            condHeader.getChildren().add(condTitle);
            
            if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                Button addCondBtn = new Button("+ Add Condition");
                addCondBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                addCondBtn.setOnAction(e -> showConditionDialog(patientId, null));
                condHeader.getChildren().add(addCondBtn);
            }

            VBox condBox = card("");
            condBox.getChildren().add(condHeader);
            
            PreparedStatement cps = conn.prepareStatement(
                "SELECT DISTINCT condition_id, condition_name, status, since_date FROM patient_conditions WHERE patient_id = ?");
            cps.setString(1, patientId);
            ResultSet crs = cps.executeQuery();
            boolean hasCond = false;
            while (crs.next()) {
                hasCond = true;
                HBox row = row(str(crs, "condition_name"), str(crs, "status"));
                if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                    int condId = crs.getInt("condition_id");
                    String cName = crs.getString("condition_name");
                    String cStatus = crs.getString("status");
                    String cDate = crs.getString("since_date");
                    
                    Button editBtn = new Button("Edit");
                    editBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #f39c12; -fx-text-fill: white;");
                    editBtn.setOnAction(e -> showConditionDialog(patientId, new ConditionData(condId, cName, cStatus, cDate)));
                    
                    Button delBtn = new Button("Delete");
                    delBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                    delBtn.setOnAction(e -> deleteRecord("patient_conditions", "condition_id", condId, patientId));
                    
                    HBox actions = new HBox(4, editBtn, delBtn);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(actions, Priority.ALWAYS);
                    row.getChildren().add(actions);
                }
                condBox.getChildren().add(row);
            }
            if (!hasCond) condBox.getChildren().add(new Label("No conditions on record"));
            detailContent.getChildren().add(condBox);

            // 3. Prescriptions Section
            VBox rxHeader = new VBox(5);
            Label rxTitle = new Label("Prescriptions");
            rxTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a6b9a;");
            rxHeader.getChildren().add(rxTitle);
            
            if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                Button addRxBtn = new Button("+ Add Prescription");
                addRxBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                addRxBtn.setOnAction(e -> showPrescriptionDialog(patientId, null));
                rxHeader.getChildren().add(addRxBtn);
            }

            VBox rxBox = card("");
            rxBox.getChildren().add(rxHeader);
            
            PreparedStatement rps = conn.prepareStatement(
                "SELECT DISTINCT prescription_id, drug, dose, frequency, indication, start_date, prescriber FROM patient_prescriptions WHERE patient_id = ?");
            rps.setString(1, patientId);
            ResultSet rrs = rps.executeQuery();
            boolean hasRx = false;
            while (rrs.next()) {
                hasRx = true;
                VBox entry = new VBox(2);
                String drugStr = str(rrs, "drug");
                String doseStr = str(rrs, "dose");
                Label drugLabel = new Label(drugStr + (doseStr.equals("—") ? "" : " (" + doseStr + ")"));
                drugLabel.setStyle("-fx-font-weight: bold;");
                Label detail = new Label("Freq: " + str(rrs, "frequency") + " | For: " + str(rrs, "indication"));
                detail.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                entry.getChildren().addAll(drugLabel, detail);
                
                HBox row = new HBox(entry);
                row.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(entry, Priority.ALWAYS);

                if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                    int rxId = rrs.getInt("prescription_id");
                    String drug = rrs.getString("drug");
                    String dose = rrs.getString("dose");
                    String freq = rrs.getString("frequency");
                    String ind = rrs.getString("indication");
                    String start = rrs.getString("start_date");
                    String prescriber = rrs.getString("prescriber");
                    
                    Button editBtn = new Button("Edit");
                    editBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #f39c12; -fx-text-fill: white;");
                    editBtn.setOnAction(e -> showPrescriptionDialog(patientId, new PrescriptionData(rxId, drug, dose, freq, ind, start, prescriber)));
                    
                    Button delBtn = new Button("Delete");
                    delBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                    delBtn.setOnAction(e -> deleteRecord("patient_prescriptions", "prescription_id", rxId, patientId));
                    
                    HBox actions = new HBox(4, editBtn, delBtn);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    row.getChildren().add(actions);
                }
                
                rxBox.getChildren().addAll(row, new Separator());
            }
            if (!hasRx) rxBox.getChildren().add(new Label("No prescriptions on record"));
            detailContent.getChildren().add(rxBox);

            // 4. Vaccines Section
            VBox vacHeader = new VBox(5);
            Label vacTitle = new Label("Vaccines");
            vacTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a6b9a;");
            vacHeader.getChildren().add(vacTitle);
            
            if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                Button addVacBtn = new Button("+ Add Vaccine Record");
                addVacBtn.setStyle("-fx-font-size: 10px; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                addVacBtn.setOnAction(e -> showVaccineDialog(patientId, null));
                vacHeader.getChildren().add(addVacBtn);
            }

            VBox vacBox = card("");
            vacBox.getChildren().add(vacHeader);
            
            PreparedStatement vps = conn.prepareStatement(
                "SELECT DISTINCT record_id, vaccine, vaccine_date, product FROM patient_vaccine_records WHERE patient_id = ?");
            vps.setString(1, patientId);
            ResultSet vrs = vps.executeQuery();
            boolean hasVac = false;
            while (vrs.next()) {
                hasVac = true;
                String vacName = str(vrs, "vaccine");
                String date = str(vrs, "vaccine_date");
                String prod = str(vrs, "product");
                HBox row = row(vacName, date + (prod.equals("—") ? "" : " (" + prod + ")"));
                
                if (gp.toLowerCase().contains(doctorName.toLowerCase())) {
                    int recId = vrs.getInt("record_id");
                    String v = vrs.getString("vaccine");
                    String vd = vrs.getString("vaccine_date");
                    String p = vrs.getString("product");
                    
                    Button editBtn = new Button("Edit");
                    editBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #f39c12; -fx-text-fill: white;");
                    editBtn.setOnAction(e -> showVaccineDialog(patientId, new VaccineData(recId, v, vd, p)));
                    
                    Button delBtn = new Button("Delete");
                    delBtn.setStyle("-fx-font-size: 9px; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                    delBtn.setOnAction(e -> deleteRecord("patient_vaccine_records", "record_id", recId, patientId));
                    
                    HBox actions = new HBox(4, editBtn, delBtn);
                    actions.setAlignment(Pos.CENTER_RIGHT);
                    HBox.setHgrow(actions, Priority.ALWAYS);
                    row.getChildren().add(actions);
                }
                vacBox.getChildren().add(row);
            }
            if (!hasVac) vacBox.getChildren().add(new Label("No vaccine records found"));
            detailContent.getChildren().add(vacBox);

        } catch (SQLException ex) {
            showError("Could not load patient: " + ex.getMessage());
        }
    }

    private void showEditPatientDialog(String patientId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM patients WHERE id = ?");
            ps.setString(1, patientId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Patient Details");
            dialog.setHeaderText("Updating details for " + rs.getString("name"));

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField nameField = new TextField(rs.getString("name"));
            TextField ageField = new TextField(String.valueOf(rs.getInt("age")));
            TextField genderField = new TextField(rs.getString("gender"));
            TextField addrField = new TextField(rs.getString("address_line1"));
            TextField cityField = new TextField(rs.getString("city"));
            TextField postcodeField = new TextField(rs.getString("postcode"));
            TextField telField = new TextField(rs.getString("telephone"));
            TextField emailField = new TextField(rs.getString("email"));
            TextField gpField = new TextField(rs.getString("gp_name"));
            TextField gpAddrField = new TextField(rs.getString("gp_address"));

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Age:"), 0, 1);
            grid.add(ageField, 1, 1);
            grid.add(new Label("Gender:"), 0, 2);
            grid.add(genderField, 1, 2);
            grid.add(new Label("Address:"), 0, 3);
            grid.add(addrField, 1, 3);
            grid.add(new Label("City:"), 0, 4);
            grid.add(cityField, 1, 4);
            grid.add(new Label("Postcode:"), 0, 5);
            grid.add(postcodeField, 1, 5);
            grid.add(new Label("Telephone:"), 0, 6);
            grid.add(telField, 1, 6);
            grid.add(new Label("Email:"), 0, 7);
            grid.add(emailField, 1, 7);
            grid.add(new Label("GP Name:"), 0, 8);
            grid.add(gpField, 1, 8);
            grid.add(new Label("GP Address:"), 0, 9);
            grid.add(gpAddrField, 1, 9);

            dialog.getDialogPane().setContent(grid);

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == saveButtonType) {
                try (PreparedStatement updatePs = conn.prepareStatement(
                    "UPDATE patients SET name=?, age=?, gender=?, address_line1=?, city=?, postcode=?, telephone=?, email=?, gp_name=?, gp_address=? WHERE id=?")) {
                    updatePs.setString(1, nameField.getText());
                    updatePs.setInt(2, Integer.parseInt(ageField.getText()));
                    updatePs.setString(3, genderField.getText());
                    updatePs.setString(4, addrField.getText());
                    updatePs.setString(5, cityField.getText());
                    updatePs.setString(6, postcodeField.getText());
                    updatePs.setString(7, telField.getText());
                    updatePs.setString(8, emailField.getText());
                    updatePs.setString(9, gpField.getText());
                    updatePs.setString(10, gpAddrField.getText());
                    updatePs.setString(11, patientId);
                    updatePs.executeUpdate();

                    showPatientDetail(patientId); // Refresh view
                } catch (NumberFormatException nfe) {
                    showError("Invalid age format.");
                }
            }
        } catch (SQLException ex) {
            showError("Error updating patient: " + ex.getMessage());
        }
    }

    private void showConditionDialog(String patientId, ConditionData data) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(data == null ? "Add Condition" : "Edit Condition");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(data == null ? "" : data.name);
        TextField statusField = new TextField(data == null ? "" : data.status);
        TextField dateField = new TextField(data == null ? "" : data.date);
        dateField.setPromptText("YYYY-MM-DD");

        grid.add(new Label("Condition Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Status:"), 0, 1);
        grid.add(statusField, 1, 1);
        grid.add(new Label("Since Date:"), 0, 2);
        grid.add(dateField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String sql = (data == null) 
                ? "INSERT INTO patient_conditions (condition_name, status, since_date, patient_id) VALUES (?,?,?,?)"
                : "UPDATE patient_conditions SET condition_name=?, status=?, since_date=? WHERE condition_id=?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nameField.getText());
                ps.setString(2, statusField.getText());
                ps.setString(3, dateField.getText());
                if (data == null) ps.setString(4, patientId);
                else ps.setInt(4, data.id);
                ps.executeUpdate();
                showPatientDetail(patientId);
            } catch (SQLException ex) { showError("Error saving condition: " + ex.getMessage()); }
        }
    }

    private void showPrescriptionDialog(String patientId, PrescriptionData data) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(data == null ? "Add Prescription" : "Edit Prescription");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField drugF = new TextField(data == null ? "" : data.drug);
        TextField doseF = new TextField(data == null ? "" : data.dose);
        TextField freqF = new TextField(data == null ? "" : data.freq);
        TextField indF = new TextField(data == null ? "" : data.ind);
        TextField startF = new TextField(data == null ? "" : data.start);
        TextField prescF = new TextField(data == null ? doctorName : data.prescriber);

        grid.add(new Label("Drug:"), 0, 0); grid.add(drugF, 1, 0);
        grid.add(new Label("Dose:"), 0, 1); grid.add(doseF, 1, 1);
        grid.add(new Label("Frequency:"), 0, 2); grid.add(freqF, 1, 2);
        grid.add(new Label("Indication:"), 0, 3); grid.add(indF, 1, 3);
        grid.add(new Label("Start Date:"), 0, 4); grid.add(startF, 1, 4);
        grid.add(new Label("Prescriber:"), 0, 5); grid.add(prescF, 1, 5);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String sql = (data == null)
                ? "INSERT INTO patient_prescriptions (drug, dose, frequency, indication, start_date, prescriber, patient_id) VALUES (?,?,?,?,?,?,?)"
                : "UPDATE patient_prescriptions SET drug=?, dose=?, frequency=?, indication=?, start_date=?, prescriber=? WHERE prescription_id=?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, drugF.getText());
                ps.setString(2, doseF.getText());
                ps.setString(3, freqF.getText());
                ps.setString(4, indF.getText());
                ps.setString(5, startF.getText());
                ps.setString(6, prescF.getText());
                if (data == null) ps.setString(7, patientId);
                else ps.setInt(7, data.id);
                ps.executeUpdate();
                showPatientDetail(patientId);
            } catch (SQLException ex) { showError("Error saving prescription: " + ex.getMessage()); }
        }
    }

    private void showVaccineDialog(String patientId, VaccineData data) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(data == null ? "Add Vaccine Record" : "Edit Vaccine Record");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField vacF = new TextField(data == null ? "" : data.vaccine);
        TextField dateF = new TextField(data == null ? "" : data.date);
        TextField prodF = new TextField(data == null ? "" : data.product);

        grid.add(new Label("Vaccine:"), 0, 0); grid.add(vacF, 1, 0);
        grid.add(new Label("Date:"), 0, 1); grid.add(dateF, 1, 1);
        grid.add(new Label("Product:"), 0, 2); grid.add(prodF, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String sql = (data == null)
                ? "INSERT INTO patient_vaccine_records (vaccine, vaccine_date, product, patient_id) VALUES (?,?,?,?)"
                : "UPDATE patient_vaccine_records SET vaccine=?, vaccine_date=?, product=? WHERE record_id=?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, vacF.getText());
                ps.setString(2, dateF.getText());
                ps.setString(3, prodF.getText());
                if (data == null) ps.setString(4, patientId);
                else ps.setInt(4, data.id);
                ps.executeUpdate();
                showPatientDetail(patientId);
            } catch (SQLException ex) { showError("Error saving vaccine: " + ex.getMessage()); }
        }
    }

    private void deleteRecord(String table, String idCol, int id, String patientId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this record?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE " + idCol + " = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    showPatientDetail(patientId);
                } catch (SQLException ex) { showError("Error deleting: " + ex.getMessage()); }
            }
        });
    }

    private static class ConditionData {
        int id; String name, status, date;
        ConditionData(int id, String n, String s, String d) { this.id=id; this.name=n; this.status=s; this.date=d; }
    }
    private static class PrescriptionData {
        int id; String drug, dose, freq, ind, start, prescriber;
        PrescriptionData(int id, String d, String ds, String f, String i, String s, String p) {
            this.id=id; this.drug=d; this.dose=ds; this.freq=f; this.ind=i; this.start=s; this.prescriber=p;
        }
    }
    private static class VaccineData {
        int id; String vaccine, date, product;
        VaccineData(int id, String v, String d, String p) { this.id=id; this.vaccine=v; this.date=d; this.product=p; }
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

    private VBox card(String title) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        box.getChildren().addAll(lbl, new Separator());
        return box;
    }

    private HBox row(String key, String val) {
        HBox hb = new HBox(10);
        Label k = new Label(key + ":");
        k.setStyle("-fx-font-weight: bold; -fx-min-width: 110px; -fx-text-fill: #555;");
        String finalVal = (val == null || val.trim().isEmpty()) ? "—" : val;
        Label v = new Label(finalVal);
        v.setStyle("-fx-text-fill: #333;");
        hb.getChildren().addAll(k, v);
        return hb;
    }

    private String str(ResultSet rs, String col) {
        try {
            String s = rs.getString(col);
            if (s == null || s.trim().isEmpty() || s.equalsIgnoreCase("null")) return "—";
            return s;
        }
        catch (SQLException e) { return "—"; }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    public static class PatientRow {
        public final String id, name, age, gp;
        public PatientRow(String id, String name, String age, String gp) {
            this.id = id; this.name = name; this.age = age;
            this.gp = gp != null ? gp : "—";
        }
    }
}