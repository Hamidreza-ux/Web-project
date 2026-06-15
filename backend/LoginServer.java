import java.util.HashMap ;
import java.util.Map ;

public class LoginServer {
    private String username ;
    private String password ;
    private static volatile LoginServer instance;

    private final Map<String, User> registeredMap = new HashMap<>() ;

    private LoginServer() {
    }

    public static LoginServer getInstance() {
        if (instance == null) {
            synchronized (LoginServer.class) {
                if(instance == null) {
                    instance = new LoginServer() ;
                }
            }
        }
        return instance ;
    }

    public synchronized LoginResult authenticate(String username , String password ) {
        this.username = username;
        this.password = password;
        User user = registeredMap.get(this.username) ;

        if (user == null) { return LoginResult.USER_NOT_FOUND ; }

        else if (!user.passwordIsRight(this.password)) { return LoginResult.WRONG_PASSWORD ; }

        else if (user.isLocked()) { return LoginResult.ACCOUNT_LOCKED ; }

        else if (user.passwordIsRight(this.password)) { return LoginResult.SUCCESS ; }
        return null;
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

}
