import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class MainServer {
    public static void main(String[] args) throws Exception {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

            server.createContext("/api/auth/login", new LoginWebHandler());
            server.createContext("/api/auth/signup", new SignupWebHandler());
            server.createContext("/api/chats", new MainPageWebHandler());
            server.createContext("/api/message", new ChatPageWebHandler());
            server.createContext("/api/chat-info", new ChatInfoWebHandler());
            server.createContext("/api/settings", new SettingsWebHandler());
            server.createContext("/api/create-chat", new CreateChatWebHandler());

            server.setExecutor(null);
            System.out.println("Server started on port 8080...");
            server.start();

            AdminCLI adminPanel = new AdminCLI();
            Thread adminThread = new Thread(adminPanel);
            adminThread.start();
        } catch (Exception e) {
            System.out.println("خطا در راه اندازی سیستم!" + e.getMessage());
        }
    }
}
