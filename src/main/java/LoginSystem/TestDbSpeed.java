package LoginSystem;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDbSpeed {
    public static void main(String[] args) {
        // Path to the database
        String home = System.getProperty("user.home");
        String dbPath = home + File.separator + "health-assistant" + File.separator + "careconnect.db";
        File dbFile = new File(dbPath);

        System.out.println("Starting DatabaseManager.init()...");
        long start = System.currentTimeMillis();
        DatabaseManager.init();
        long end = System.currentTimeMillis();
        System.out.println("First init() took: " + (end - start) + " ms");

        // Check if data is there
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patients")) {
            if (rs.next()) {
                System.out.println("Patients count: " + rs.getInt(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\nStarting second DatabaseManager.init()...");
        start = System.currentTimeMillis();
        DatabaseManager.init();
        end = System.currentTimeMillis();
        System.out.println("Second init() took: " + (end - start) + " ms");
        
        System.out.println("\nDone.");
    }
}
