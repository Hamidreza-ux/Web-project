import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public class LoginWebHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // اجازه برای دسترسی به قسمت فرانت
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // 204 یعنی no content یعنی تایید شدن
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                String username = parseJsonFieldWhithRegex(body, "username");
                String password = parseJsonFieldWhithRegex(body, "password"); // استخراج username , password,

                /*
                 * if (username.isBlank() || password.isBlank()) {
                 * sendResponse(exchange, 400,
                 * "{\"status\":\"error\", \"message\":\"نام کاربری و رمز عبور نمی‌توانند خالی باشند.\"}"
                 * );
                 * return;
                 * }
                 */

                LoginServer loginServer = LoginServer.getInstance();
                LoginResult result = loginServer.authenticate(username, password);

                int statusCode;
                String jsonResponse;

                switch (result) {
                    case LoginResult.SUCCESS:
                        statusCode = 200; // ok
                        jsonResponse = "{\"status\":\"success\", \"message\":\"ورود با موفقیت انجام شد!\"}";
                        break;
                    case LoginResult.USER_NOT_FOUND:
                        statusCode = 404; // not found
                        jsonResponse = "{\"status\":\"error\", \"message\":\"کاربری با این نام کاربری یافت نشد.\"}";
                        break;
                    case LoginResult.WRONG_PASSWORD:
                        statusCode = 401; // unauthorized
                        jsonResponse = "{\"status\":\"error\", \"message\":\"رمز عبور اشتباه است.\"}";
                        break;
                    case LoginResult.ACCOUNT_LOCKED:
                        statusCode = 403; // forbidden
                        jsonResponse = "{\"status\":\"error\", \"message\":\"حساب شما به دلیل ۵ بار ورود اشتباه موقتاً مسدود شده است.\"}";
                        break;
                    default:
                        statusCode = 500; // server error
                        jsonResponse = "{\"status\":\"error\", \"message\":\"خطای داخلی سرور\"}";
                }

                sendResponse(exchange, statusCode, jsonResponse);

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"status\":\"error\", \"message\":\"خطا در پردازش درخواست ورود\"}");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }

    }

    private void sendResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException { // متدی
                                                                                                               // برای
                                                                                                               // ارسال
                                                                                                               // پاسخ
                                                                                                               // به
                                                                                                               // فرانت
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private String parseJsonFieldWhithRegex(String json, String field) { // متن جیسون رو استخراج میکند
        try {
            String regex = String.format("\"%s\":\"([^\"]+)\"", field);
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(json);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }
}
