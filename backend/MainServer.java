import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class MainServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/auth/login", new LoginWebHandler());
        server.createContext("/api/auth/signup", new SignupWebHandler());
        server.createContext("/api/chats", new MainPageWebHandler());
        server.createContext("/api/message", new ChatPageWebHandler());
        server.createContext("/api/chat-info", new ChatInfoWebHandler());

        server.setExecutor(null);
        System.out.println("Server started on port 8080...");
        server.start();
    }
}
