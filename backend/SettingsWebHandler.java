import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public class SettingsWebHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-Username");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String username = exchange.getRequestHeaders().getFirst("X-Username");
        if (username == null || username.isBlank()) {
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

            String json = "{"
                    + "\"username\":\"" + user.getUsername() + "\","
                    + "\"id\":\"" + user.getID() + "\","
                    + "\"avatarUrl\":\"" + user.getAvatarURL() + "\","
                    + "\"isDarkMode\":" + user.isDarkMode()
                    + "}";
            sendResponse(exchange, 200, json);
        }

        else if ("POST".equalsIgnoreCase(method)) {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String action = parseJsonFieldWhithRegex(body, "action");

            if ("update_profile".equals(action)) {
                String newAvatarUrl = parseJsonFieldWhithRegex(body, "newAvatarUrl"); // اگر خالی باشد یعنی عکس حذف شده

                loginServer.updateProfile(username, newAvatarUrl);
                sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"پروفایل آپدیت شد.\"}");
            }

            else if ("change_id".equals(action)) {
                String newId = parseJsonFieldWhithRegex(body, "newId");
                if (newId == null || newId.isBlank()) {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"آیدی نمی‌تواند خالی باشد.\"}");
                    return;
                }

                String result = loginServer.updateId(username, newId);
                if ("SUCCESS".equals(result)) {
                    sendResponse(exchange, 200,
                            "{\"status\":\"success\",\"message\":\"آیدی کاربری با موفقیت تغییر کرد.\"}");
                } else if ("DUPLICATE_ID".equals(result)) {
                    sendResponse(exchange, 409,
                            "{\"status\":\"error\",\"message\":\"این آیدی کاربری قبلاً توسط شخص دیگری انتخاب شده است.\"}");
                } else {
                    sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در تغییر آیدی.\"}");
                }
            }

            else if ("toggle_theme".equals(action)) {
                boolean darkMode = "true".equalsIgnoreCase(parseJsonFieldWhithRegex(body, "darkMode"));
                loginServer.toggleTheme(username, darkMode);
                sendResponse(exchange, 200, "{\"status\":\"success\",\"message\":\"تم تغییر کرد.\"}");
            }
        }

        else if ("DELETE".equalsIgnoreCase(method)) {
            boolean isDeleted = loginServer.deleteUserAccount(username);
            if (isDeleted) {
                sendResponse(exchange, 200,
                        "{\"status\":\"success\",\"message\":\"حساب کاربری شما با موفقیت و برای همیشه حذف شد.\"}");
            } else {
                sendResponse(exchange, 400, "{\"status\":\"error\",\"message\":\"خطا در حذف حساب کاربری.\"}");
            }
        }

        else {
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
