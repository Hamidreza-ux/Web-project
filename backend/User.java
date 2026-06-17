public class User {
    private String username;
    private String password;
    private int failedAttempt;
    private String ID;

    public User(String username, String ID, String password) {
        this.username = username;
        this.ID = ID;
        this.password = password;
        failedAttempt = 0;
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

}
