public class User {
    private String username ;
    private String password ;
    private int failedAttempt ;

    public User(String username , String password ) {
        this.username = username ;
        this.password = password ;
        failedAttempt = 0 ;
    }

    public boolean passwordIsRight (String newPassword) {
        try {
            if(this.password.equals(newPassword) ) {
                if (failedAttempt < 5)
                    return true ;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        failedAttempt++ ;
        return false ;
    }

    public boolean isLocked() {
        return failedAttempt >= 5 ;
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
    
}
