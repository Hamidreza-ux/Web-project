import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.*;

public class ChatPageWebHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Username, X-Chat-Id, X-Search-Msg");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 204, "");
            return;
        }

        String username = exchange.getRequestHeaders().getFirst("X-Username");
        String chatId = exchange.getRequestHeaders().getFirst("X-Chat-Id");

        if (username == null || chatId == null) {
            sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"هدرهای الزامی فرستاده نشده‌اند.\"}");
            return;
        }

        LoginServer loginServer = LoginServer.getInstance();

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ChatRoom room = loginServer.findChatRoom(username, chatId);
            if (room == null) {
                sendResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"چت یافت نشد.\"}");
                return;
            }

            String searchMsg = exchange.getRequestHeaders().getFirst("X-Search-Msg");

            StringBuilder json = new StringBuilder("[");
            List<ChatMessage> messages = room.getMessages();
            int addedCount = 0;

            for (ChatMessage msg : messages) {
                if (searchMsg != null && !searchMsg.isBlank()) {
                    if (!msg.getContent().toLowerCase().contains(searchMsg.toLowerCase())) {
                        continue;
                    }
                }

                if (addedCount > 0)
                    json.append(",");
                json.append("{")
                        .append("\"id\":\"").append(msg.getId()).append("\",")
                        .append("\"sender\":\"").append(msg.getSender()).append("\",")
                        .append("\"content\":\"").append(msg.getContent()).append("\",")
                        .append("\"isFile\":").append(msg.isFile()).append(",")
                        .append("\"isEdited\":").append(msg.isEdited()).append(",")
                        .append("\"isReported\":").append(msg.isReported())
                        .append("}");
                addedCount++;
            }
            json.append("]");
            sendResponse(exchange, 200, json.toString());
        }

        else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String content = parseJsonFieldWhithRegex(body, "content");
            boolean isFile = "true".equalsIgnoreCase(parseJsonFieldWhithRegex(body, "isFile"));

            String result = loginServer.addMessageToChat(username, chatId, content, isFile);

            if ("SUCCESS".equals(result)) {
                sendResponse(exchange, 201, "{\"status\":\"success\",\"message\":\"پیام ارسال شد.\"}");
            } else if ("SPAM_DETECTED".equals(result)) {
                //429 یعنی درخواست بیش از حد
                sendResponse(exchange, 429,
                        "{\"status\":\"error\",\"message\":\"اسپم ممنوع! حداکثر ۵ پیام در ثانیه.\"}");
            } else if ("MESSAGE_TOO_LONG".equals(result)) {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"طول پیام بیش از حد مجاز است.\"}");
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در ارسال پیام.\"}");
            }
        }

        else if ("PUT".equalsIgnoreCase(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String messageId = parseJsonFieldWhithRegex(body, "messageId");
            String action = parseJsonFieldWhithRegex(body, "action");

            if ("report".equals(action)) {
                boolean ok = loginServer.reportMessage(username, chatId, messageId);
                if (ok)
                    sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"گزارش ثبت شد.\"}");
                else
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در ثبت گزارش.\"}");
            } else {
                String newContent = parseJsonFieldWhithRegex(body, "newContent");
                boolean ok = loginServer.editMessage(username, chatId, messageId, newContent);
                if (ok)
                    sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"پیام ویرایش شد.\"}");
                else
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در ویرایش پیام.\"}");
            }
        }

        else if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String messageId = parseJsonFieldWhithRegex(body, "messageId");

            boolean ok = loginServer.deleteMessage(username, chatId, messageId);
            if (ok) {
                sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"پیام حذف شد.\"}");
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"شما مجاز به حذف این پیام نیستید.\"}");
            }
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
