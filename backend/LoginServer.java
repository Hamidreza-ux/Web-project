import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginServer {
    private String username;
    private String password;
    private static volatile LoginServer instance;

    private final Map<String, User> registeredMap = new HashMap<>(); // چون هنوز به دیتابیس وصل نشدیم اطلاعات کاربر رو
                                                                     // به صورت
                                                                     // موقت در اینجا ذخیره میکنیم
    private final Map<String, List<ChatRoom>> userChatsMap = new ConcurrentHashMap<>(); // لیست صفحه چت ها و نام کاربری
                                                                                        // کاربر

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

    public synchronized LoginResult authenticate(String username, String password) { // synchronized برای multy
                                                                                     // threading,
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

        List<ChatRoom> initialChats = new ArrayList<>();
        ChatRoom savedMessages = new ChatRoom("saved_messages", "Saved Messages", "assets/saved.png");
        savedMessages.addMessage(new ChatMessage("System", "به پیام‌های ذخیره شده خوش آمدید!", false));
        initialChats.add(savedMessages);
        userChatsMap.put(username, initialChats);

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

    public List<ChatRoom> getUserMainChats(String username, String searchQuery) { // recieve chat list
        List<ChatRoom> allChats = userChatsMap.getOrDefault(username, new ArrayList<>());
        List<ChatRoom> filteredChats = new ArrayList<>();

        for (ChatRoom chat : allChats) {
            // چت‌های آرشیو شده نباید در صفحه اصلی بیایند
            if (chat.isArchived())
                continue;

            // قابلیت جستجو بین گفتگوها (اگر کاربر چیزی سرچ کرده باشد)
            if (searchQuery != null && !searchQuery.isBlank()) {
                if (!chat.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                    continue; // اگر نام چت با سرچ نخواند، ردش کن
                }
            }
            filteredChats.add(chat);
        }

        filteredChats.sort((chat1, chat2) -> {
            // pin
            if (chat1.isPinned() && !chat2.isPinned())
                return -1;
            if (!chat1.isPinned() && chat2.isPinned())
                return 1;

            // date
            ChatMessage msg1 = chat1.getLastMessage();
            ChatMessage msg2 = chat2.getLastMessage();

            if (msg1 == null && msg2 == null)
                return 0;
            if (msg1 == null)
                return 1;
            if (msg2 == null)
                return -1;

            return msg2.getTimestamp().compareTo(msg1.getTimestamp());
        });

        return filteredChats;
    }

    // آرشیو
    public List<ChatRoom> getArchivedChats(String username) {
        List<ChatRoom> allChats = userChatsMap.getOrDefault(username, new ArrayList<>());
        List<ChatRoom> archived = new ArrayList<>();
        for (ChatRoom chat : allChats) {
            if (chat.isArchived()) {
                archived.add(chat);
            }
        }
        return archived;
    }

    public ChatRoom findChatRoom(String username, String chatId) {
        List<ChatRoom> chats = userChatsMap.get(username);
        if (chats != null) {
            for (ChatRoom chat : chats) {
                if (chat.getId().equals(chatId))
                    return chat;
            }
        }
        return null;
    }

    public String addMessageToChat(String username, String chatId, String content, boolean isFile) {
        User user = registeredMap.get(username);
        if (user == null)
            return "USER_NOT_FOUND";

        if (user.isSpamming())
            return "SPAM_DETECTED";

        if (content.length() > 1000)
            return "MESSAGE_TOO_LONG";

        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return "CHAT_NOT_FOUND";

        ChatMessage msg = new ChatMessage(username, content, isFile);
        room.addMessage(msg);
        room.setUnreadCount(0); 
        return "SUCCESS";
    }

    public boolean editMessage(String username, String chatId, String messageId, String newContent) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;

        for (ChatMessage msg : room.getMessages()) {
            if (msg.getId().equals(messageId) && msg.getSender().equals(username)) {
                msg.setContent(newContent);
                msg.setEdited(true);
                return true;
            }
        }
        return false;
    }

    public boolean deleteMessage(String username, String chatId, String messageId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;

        return room.getMessages().removeIf(msg -> msg.getId().equals(messageId) && msg.getSender().equals(username));
    }

    public boolean reportMessage(String username, String chatId, String messageId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;

        for (ChatMessage msg : room.getMessages()) {
            if (msg.getId().equals(messageId)) {
                msg.setReported(true);
                System.out.println("⚠️ پیام گزارش شد! فرستنده: " + msg.getSender() + " | محتوا: " + msg.getContent());
                return true;
            }
        }
        return false;
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
