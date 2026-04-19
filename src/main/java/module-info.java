module health.assistant {
    requires javafx.controls;
    requires javafx.web;
    requires java.sql;
    requires java.net.http;
    requires org.json;                  
    requires org.xerial.sqlitejdbc;     

    exports LoginSystem;
    exports clientUI;
    exports APIhandlers;

    opens LoginSystem to javafx.fxml, javafx.graphics;
    opens clientUI to javafx.graphics;
    opens APIhandlers to javafx.graphics;
    exports clientUI.patient;
    opens clientUI.patient to javafx.graphics;
    exports clientUI.doctor;
    opens clientUI.doctor to javafx.graphics;
    exports clientUI.operator;
    opens clientUI.operator to javafx.graphics;
}