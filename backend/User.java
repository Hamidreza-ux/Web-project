import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    private int failedAttempt;
    private String ID;
    private final List<Long> messageTimestamps;
    private boolean darkMode;
    private final List<String> contacts;

    public User(String username, String ID, String password) {
        this.username = username;
        this.ID = ID;
        this.password = password;
        failedAttempt = 0;
        this.messageTimestamps = new ArrayList<>();
        this.darkMode = false;
        this.contacts = new ArrayList<>();
    }

    public boolean passwordIsRight(String newPassword) {
        try {
            if (isLocked()) {
                return false;
            }
            if (this.password.equals(newPassword)) {
                failedAttempt = 0;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        failedAttempt++;
        return false;
    }

    public synchronized boolean isSpamming() {
        long currentTimeMillis = Instant.now().toEpochMilli();

        messageTimestamps.removeIf(timestamp -> (currentTimeMillis - timestamp) > 1000);

        if (messageTimestamps.size() >= 5) {
            return true;
        }

        messageTimestamps.add(currentTimeMillis);
        return false;
    }

    public boolean isLocked() {
        return failedAttempt >= 5;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getFailedAttempt() {
        return failedAttempt;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public List<String> getContacts() {
        return contacts;
    }

}
