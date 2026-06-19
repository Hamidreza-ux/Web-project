import java.util.HashMap;
import java.util.Map;

public class LoginServer {
    private String username;
    private String password;
    private static volatile LoginServer instance;

    private final Map<String, User> registeredMap = new HashMap<>(); // چون هنوز به دیتابیس وصل نشدیم اطلاعات رو به صورت
                                                                     // موقت در اینجا ذخیره میکنیم

    private LoginServer() {
    }

    public static LoginServer getInstance() {
        if (instance == null) {
            synchronized (LoginServer.class) {
                if (instance == null) {
                    instance = new LoginServer();
                }
            }
        }
        return instance;
    }

    public synchronized LoginResult authenticate(String username, String password) {    //synchronized برای multy threading,
        this.username = username;
        this.password = password;
        User user = registeredMap.get(this.username);

        if (user == null) {
            return LoginResult.USER_NOT_FOUND;
        }

        else if (!user.passwordIsRight(this.password)) {
            return LoginResult.WRONG_PASSWORD;
        }

        else if (user.isLocked()) {
            return LoginResult.ACCOUNT_LOCKED;
        }

        else if (user.passwordIsRight(this.password)) {
            return LoginResult.SUCCESS;
        }
        return null;
    }

    public synchronized SignupResult register(String username, String id, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return SignupResult.PASSWORD_MISMATCH;
        }
        if (registeredMap.containsKey(username)) {
            return SignupResult.DUPLICATE_USERNAME;
        }
        for (User u : registeredMap.values()) {
            if (u.getID().equalsIgnoreCase(id)) {
                return SignupResult.DUPLICATE_ID;
            }
        }
        if (!isPasswordValid(password, username)) {
            return SignupResult.INVALID_PASSWORD;
        }
        User newUser = new User(username, id, password);
        registeredMap.put(username, newUser);

        return SignupResult.SUCCESS;
    }

    private boolean isPasswordValid(String password, String username) {
        if (password.contains(username)) {
            return false;
        }
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$";
        return password.matches(regex);
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
