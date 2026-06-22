import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatInfoWebHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Username, X-Chat-Id, X-Get-History");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 204, "");
            return;
        }

        String username = exchange.getRequestHeaders().getFirst("X-Username");
        String chatId = exchange.getRequestHeaders().getFirst("X-Chat-Id");

        if (username == null || chatId == null) {
            sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"هدرهای الزامی وجود ندارند.\"}");
            return;
        }

        LoginServer loginServer = LoginServer.getInstance();
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            ChatRoom room = loginServer.findChatRoom(username, chatId);
            if (room == null) {
                sendResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"گفتگو یافت نشد.\"}");
                return;
            }

            String getHistory = exchange.getRequestHeaders().getFirst("X-Get-History");

            if ("true".equalsIgnoreCase(getHistory)) {
                StringBuilder json = new StringBuilder("{");

                json.append("\"deletedMessages\":[");
                List<ChatMessage> deleted = room.getHistory();
                for (int i = 0; i < deleted.size(); i++) {
                    ChatMessage m = deleted.get(i);
                    json.append("{\"sender\":\"").append(m.getSender()).append("\",\"content\":\"")
                            .append(m.getContent()).append("\"}");
                    if (i < deleted.size() - 1)
                        json.append(",");
                }
                json.append("],");

                json.append("\"editedMessages\":[");
                List<ChatMessage> activeMsgs = room.getMessages();
                int editedCount = 0;
                for (ChatMessage m : activeMsgs) {
                    if (m.isEdited()) {
                        if (editedCount > 0)
                            json.append(",");
                        json.append("{\"id\":\"").append(m.getId()).append("\",")
                                .append("\"currentContent\":\"").append(m.getContent()).append("\",")
                                .append("\"previousVersion\":\"").append(room.getHistory().get(0)).append("\"}");                                                       
                        editedCount++;
                    }
                }
                json.append("]}");

                sendResponse(exchange, 200, json.toString());
                return;
            }

            StringBuilder json = new StringBuilder("{");
            json.append("\"id\":\"").append(room.getId()).append("\",")
                    .append("\"name\":\"").append(room.getName()).append("\",")
                    .append("\"avatarUrl\":\"").append(room.getAvatarUrl()).append("\",")
                    .append("\"isGroup\":").append(room.isGroup()).append(",")
                    .append("\"isArchived\":").append(room.isArchived()).append(",");

            if (room.isGroup()) {
                json.append("\"memberCount\":").append(room.getMembers().size());
            } else {
                List<String> commonGroups = loginServer.getCommonGroups(username, room.getId());
                json.append("\"username\":\"").append(room.getUsername()).append("\",")
                        .append("\"isBlocked\":").append(room.isBlocked()).append(",")
                        .append("\"commonGroups\":")
                        .append(commonGroups.toString().replace("[", "[\"").replace("]", "\"]").replace(", ", "\",\""));
            }
            json.append("}");
            sendResponse(exchange, 200, json.toString());
        }

        else if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String action = parseJsonFieldWhithRegex(body, "action");

            boolean success = false;
            String message = "عملیات ناموفق";

            if ("block".equals(action)) {
                success = loginServer.toggleBlockUser(username, chatId);
                message = "وضعیت بلاک کاربر تغییر کرد.";
            } else if ("archive".equals(action)) {
                success = loginServer.toggleArchiveChat(username, chatId);
                message = "وضعیت آرشیو گفتگو تغییر کرد.";
            } else if ("leave".equals(action)) {
                success = loginServer.leaveGroup(username, chatId);
                message = "شما گروه را ترک کردید.";
            } else if ("add_member".equals(action)) {
                String newMember = parseJsonFieldWhithRegex(body, "newMember");
                success = loginServer.addMemberToGroup(username, chatId, newMember);
                message = "عضو جدید با موفقیت افزوده شد.";
            } else if ("edit_group".equals(action)) {
                String newName = parseJsonFieldWhithRegex(body, "newName");
                String newAvatar = parseJsonFieldWhithRegex(body, "newAvatar");
                success = loginServer.updateGroupInfo(username, chatId, newName, newAvatar);
                message = "اطلاعات گروه با موفقیت ویرایش شد.";
            }

            if (success) {
                sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"" + message + "\"}");
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"" + message + "\"}");
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

    private String parseJsonFieldWhithRegex(String json, String field) {
        try {
            String regex = String.format("\"%s\"\\s*:\\s*\"([^\"]*)\"", field);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(json);
            if (matcher.find())
                return matcher.group(1);
            return "";
        } catch (Exception e) {
            return "";
        }
    }
}
