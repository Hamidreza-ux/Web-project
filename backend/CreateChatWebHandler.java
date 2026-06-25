import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;

public class CreateChatWebHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Username");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 204, "");
            return;
        }

        String username = exchange.getRequestHeaders().getFirst("X-Username");
        if (username == null || username.trim().isEmpty()) {
            sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"کاربر احراز هویت نشده است.\"}");
            return;
        }

        LoginServer loginServer = LoginServer.getInstance();
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            User user = loginServer.getRegisteredMap().get(username);
            if (user == null) {
                sendResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"کاربر یافت نشد.\"}");
                return;
            }

            List<String> contactsList = user.getContacts();
            StringBuilder json = new StringBuilder("[");

            for (int i = 0; i < contactsList.size(); i++) {
                String contactUsername = contactsList.get(i);
                User contactUser = loginServer.getRegisteredMap().get(contactUsername);

                String avatar = "https://api.dicebear.com/7.x/bottts/svg?seed=" + contactUsername;
                String uniqueId = contactUsername;

                if (contactUser != null) {
                    uniqueId = contactUser.getID(); // آیدی منحصربه‌فرد برای نمایش
                }

                json.append("{")
                        .append("\"username\":\"").append(contactUsername).append("\",")
                        .append("\"uniqueId\":\"").append(uniqueId).append("\",")
                        .append("\"avatarUrl\":\"").append(avatar).append("\"")
                        .append("}");

                if (i < contactsList.size() - 1)
                    json.append(",");
            }
            json.append("]");

            sendResponse(exchange, 200, json.toString());
        }

        else if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String action = parseJsonFieldWhithRegex(body, "action");

            if ("add_contact".equals(action)) {
                String targetId = parseJsonFieldWhithRegex(body, "targetId");
                if (targetId == null || targetId.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"آیدی کاربری وارد نشده است.\"}");
                    return;
                }

                String result = loginServer.addContactByUniqueId(username, targetId);

                if ("SUCCESS".equals(result)) {
                    sendResponse(exchange, 200,
                            "{\"status\":\"success\",\"message\":\"مخاطب با موفقیت اضافه شد و چت فعال گردید.\"}");
                } else if ("TARGET_NOT_FOUND".equals(result)) {
                    sendResponse(exchange, 404, "{\"status\":\"error\",\"message\":\"کاربری با این آیدی یافت نشد.\"}");
                } else if ("CANNOT_ADD_SELF".equals(result)) {
                    sendResponse(exchange, 400,
                            "{\"status\":\"error\",\"message\":\"شما نمی‌توانید آیدی خودتان را اضافه کنید.\"}");
                } else if ("ALREADY_CONTACT".equals(result)) {
                    sendResponse(exchange, 409,
                            "{\"status\":\"error\",\"message\":\"این کاربر از قبل در لیست مخاطبین شما وجود دارد.\"}");
                } else {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در اضافه کردن مخاطب.\"}");
                }
            }

            else if ("create_group".equals(action)) {
                String groupName = parseJsonFieldWhithRegex(body, "groupName");
                if (groupName == null || groupName.trim().isEmpty()) {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"نام گروه نباید خالی باشد.\"}");
                    return;
                }

                String membersRaw = parseJsonFieldWhithRegex(body, "members");
                List<String> memberList = new ArrayList<>();
                if (!membersRaw.isEmpty()) {
                    for (String m : membersRaw.split(",")) {
                        if (!m.trim().isEmpty())
                            memberList.add(m.trim());
                    }
                }

                boolean ok = loginServer.createNewGroup(username, groupName, memberList);
                if (ok) {
                    sendResponse(exchange, 201,
                            "{\"status\":\"success\",\"message\":\"گروه جدید با موفقیت ساخته شد.\"}");
                } else {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در ساخت گروه.\"}");
                }
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
