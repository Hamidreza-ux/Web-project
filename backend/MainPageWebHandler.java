import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class MainPageWebHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // اجازه دادن به فرانت اند
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Username, X-Search");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        //دریافت لیست چت ها
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                String username = exchange.getRequestHeaders().getFirst("X-Username");
                String searchQuery = exchange.getRequestHeaders().getFirst("X-Search"); 
                String isArchivePage = exchange.getRequestHeaders().getFirst("X-Get-Archive"); 

                if (username == null || username.isBlank()) {
                    sendResponse(exchange, 400, "{\"status\":\"error\", \"message\":\"نام کاربری احراز نشده است.\"}");
                    return;
                }

                LoginServer loginServer = LoginServer.getInstance();
                List<ChatRoom> chats;

                if ("true".equalsIgnoreCase(isArchivePage)) {
                    chats = loginServer.getArchivedChats(username);
                } else {
                    chats = loginServer.getUserMainChats(username, searchQuery);
                }

                // ساخت ساختار متنی JSON به صورت دستی برای لیست چت‌ها
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[");
                for (int i = 0; i < chats.size(); i++) {
                    ChatRoom chat = chats.get(i);
                    ChatMessage lastMsg = chat.getLastMessage();
                    String lastMsgContent = (lastMsg != null) ? lastMsg.getContent() : "";

                    jsonBuilder.append("{")
                            .append("\"id\":\"").append(chat.getId()).append("\",")
                            .append("\"name\":\"").append(chat.getName()).append("\",")
                            .append("\"avatarUrl\":\"").append(chat.getAvatarUrl()).append("\",")
                            .append("\"unreadCount\":").append(chat.getUnreadCount()).append(",")
                            .append("\"isPinned\":").append(chat.isPinned()).append(",")
                            .append("\"isArchived\":").append(chat.isArchived()).append(",")
                            .append("\"lastMessage\":\"").append(lastMsgContent).append("\"")
                            .append("}");

                    if (i < chats.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");

                sendResponse(exchange, 200, jsonBuilder.toString());

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"status\":\"error\", \"message\":\"خطا در بارگذاری چت‌ها\"}");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}
