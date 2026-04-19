package clientUI;

import LoginSystem.DatabaseManager;
import LoginSystem.LoginSystem;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        System.out.println("Loading Care Connect from Main.java");

        //starts back end
        LoginSystem.init();
        DatabaseManager.init();

        //starts JavaFX GUI
        Application.launch(LoginGUI.class, args);
    }
}