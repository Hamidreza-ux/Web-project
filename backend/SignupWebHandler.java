import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignupWebHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {   // اجازه اولیه قبل پست
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try {
                InputStream is = exchange.getRequestBody();
                
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String username = parseJsonFieldWhithRegex(body, "username");
                String id = parseJsonFieldWhithRegex(body, "id");
                String password = parseJsonFieldWhithRegex(body, "password");
                String confirmPassword = parseJsonFieldWhithRegex(body, "confirmPassword");

                if (username.trim().isEmpty() || id.trim().isEmpty() || password.trim().isEmpty()
                        || confirmPassword.trim().isEmpty()) {
                    sendResponse(exchange, 400,
                            "{\"status\":\"error\", \"message\":\"پر کردن تمام فیلدها الزامی است.\"}");
                    return;
                }

                LoginServer loginServer = LoginServer.getInstance();
                SignupResult result = loginServer.register(username, id, password, confirmPassword);

                int statusCode;
                String jsonResponse;

                switch (result) {
                    case SignupResult.SUCCESS:
                        statusCode = 201;
                        jsonResponse = "{\"status\":\"success\", \"message\":\"ثبت‌نام با موفقیت انجام شد! در حال انتقال به صفحه لاگین...\"}";
                        break;
                    case SignupResult.PASSWORD_MISMATCH:
                        statusCode = 400;
                        jsonResponse = "{\"status\":\"error\", \"message\":\"رمز عبور و تکرار آن با یکدیگر مطابقت ندارند.\"}";
                        break;
                    case SignupResult.DUPLICATE_USERNAME:
                        statusCode = 409;   //conflict
                        jsonResponse = "{\"status\":\"error\", \"message\":\"این نام کاربری قبلاً توسط شخص دیگری انتخاب شده است.\"}";
                        break;
                    case SignupResult.DUPLICATE_ID:
                        statusCode = 409;
                        jsonResponse = "{\"status\":\"error\", \"message\":\"این آیدی تکراری است و متعلق به کاربر دیگری می‌باشد.\"}";
                        break;
                    case SignupResult.INVALID_PASSWORD:
                        statusCode = 422;
                        jsonResponse = "{\"status\":\"error\", \"message\":\"رمز عبور ضعیف است! باید شامل حداقل ۸ کاراکتر، حرف بزرگ، حرف کوچک، عدد، کاراکتر خاص بوده و شامل نام کاربری نباشد.\"}";
                        break;
                    default:
                        statusCode = 500;
                        jsonResponse = "{\"status\":\"error\", \"message\":\"خطای داخلی سرور\"}";
                }

                sendResponse(exchange, statusCode, jsonResponse);

            } catch (Exception e) {
                sendResponse(exchange, 500, "{\"status\":\"error\", \"message\":\"خطا در پردازش درخواست ثبت‌نام\"}");
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    public static String parseJsonFieldWhithRegex(String json, String field) {
        try {
            String regex = String.format("\"%s\"\\s*:\\s*\"([^\"]*)\"", field);
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
