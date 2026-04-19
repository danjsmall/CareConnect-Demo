package LoginSystem;

public class LoginSystem {
    // Note: This class previously used a users.txt file for storage, 
    // but now uses DatabaseManager (SQLite) instead.

    public static class User {
        public String password; //Stored hash/plaintext (depending on use)
        public String salt; //Random salt byte that is used for hashing to stop same passwords being stored the same
        public int level;
        public String displayName;
        public String patientId;

        public User(String password, int level) {
            this.password = password;
            this.level = level;
            this.salt = null;
        }

        public User(String password, String salt, int level) {
            this.password = password;
            this.salt = salt;
            this.level = level;
        }

        public User(String password, String salt, int level, String displayName, String patientId) {
            this.password = password;
            this.salt = salt;
            this.level = level;
            this.displayName = displayName;
            this.patientId = patientId;
        }
    }


    //Initialize & load users
    public static synchronized void init() {
        // Initialization now handled primarily by DatabaseManager.init()
        // which loads default accounts from user_accounts_inserts.sql
    }

    //To register a user 
public static synchronized boolean register(String email, String password, String patientId) {
    if (email == null || password == null) return false;
    email = email.trim();
    if (email.isEmpty() || email.contains(":")) return false;

    boolean isAdmin = "root".equalsIgnoreCase(email) || "admin".equalsIgnoreCase(email) || "admin@healthassistant.com".equalsIgnoreCase(email);
    int    level = isAdmin ? 3 : 1;
    String role  = isAdmin ? "admin" : "patient";


    byte[] salt    = generateSalt();
    String saltB64 = java.util.Base64.getEncoder().encodeToString(salt);
    String hash    = hashPassword(password, salt);

    


    return DatabaseManager.insertUser(email, hash, saltB64, role, level, patientId);
}

public static synchronized boolean register(String email, String password) {
    return register(email, password, null);
}

    //Authenticate and return User object (or null if failed)
  public static synchronized User authenticate(String email, String password) {
    if (email == null || password == null) return null;

    DatabaseManager.UserRow row = DatabaseManager.findUser(email);
    if (row == null) return null;

    if (row.salt != null) {
        byte[] salt = java.util.Base64.getDecoder().decode(row.salt);
        String hash = hashPassword(password, salt);
        if (!hash.equals(row.passwordHash)) return null;
    } else {
        if (!row.passwordHash.equals(password)) return null;
        byte[] newSalt  = generateSalt();
        String saltB64  = java.util.Base64.getEncoder().encodeToString(newSalt);
        String newHash  = hashPassword(password, newSalt);
        DatabaseManager.updatePasswordHash(email, newHash, saltB64);
    }

    DatabaseManager.recordLogin(email);
    String displayName = DatabaseManager.getNameByEmail(email, row.role);
    return new User(row.passwordHash, row.salt, row.level, displayName, row.patientId);
}

    //Checks stored level for a username, or -1 if not found
public static synchronized int getLevel(String email) {
    DatabaseManager.UserRow row = DatabaseManager.findUser(email);
    return row == null ? -1 : row.level;
}
    //Password hashing algorithm below (PBKDF2 algorithm used)
    private static byte[] generateSalt() {
        try {
            java.security.SecureRandom sr = java.security.SecureRandom.getInstanceStrong();
            byte[] salt = new byte[16];
            sr.nextBytes(salt);
            return salt;
        } catch (java.security.NoSuchAlgorithmException e) {
            byte[] salt = new byte[16];
            new java.util.Random().nextBytes(salt);
            return salt;
        }
    }

    //Hashes the password with salt using PBKDF2(HMAC-SHA256, 100000 iterations, 256-bit output), if fails stores in plaintext and message in console (debugging purposes)
    private static String hashPassword(String password, byte[] salt) {
        final int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                javax.crypto.SecretKeyFactory skf = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 100_000, 256);
                byte[] hash = skf.generateSecret(spec).getEncoded();
                return java.util.Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    //After final try send out terminal warning and store as plaintext
                    System.err.println("[WARNING] password hashing failed after " + maxRetries + " attempts; storing plaintext (insecure)");
                    return password;
                }
                //Otherwise retry
            }
        }
        //Couldnt even do it, shoot out error (this shouldnt happen - fail safe code)
        System.err.println("[WARNING] password hashing failed unexpectedly; storing plaintext (insecure)");
        return password;
    }

    private LoginSystem() {} //To prevent creation of LoginSystem instances, because all methods are static
}