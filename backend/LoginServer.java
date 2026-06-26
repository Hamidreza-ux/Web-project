import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;

public class LoginServer {
    private String username;
    private String password;
    private static volatile LoginServer instance; // valotile باعث سرعت در دیدن دیتای جدید می شود(multy threading)

    private final Map<String, User> registeredMap = new HashMap<>(); // چون هنوز به دیتابیس وصل نشدیم اطلاعات کاربر رو
                                                                     // به صورت
                                                                     // موقت در اینجا ذخیره میکنیم
    private final Map<String, List<ChatRoom>> userChatsMap = new ConcurrentHashMap<>(); // لیست صفحه چت ها و نام کاربری
                                                                                        // کاربر
    private static final String DB_DIR = "database/"; // برای پایگاه داده متنی این کار رو کردم
    private static final String USERS_FILE = DB_DIR + "users.txt";
    private static final String CHATS_DIR = DB_DIR + "chats/";

    private LoginServer() {
        try {
            File dbFolder = new File(DB_DIR);
            File chatsFolder = new File(CHATS_DIR);
            if (!dbFolder.exists())
                dbFolder.mkdir();
            if (!chatsFolder.exists())
                chatsFolder.mkdir();

            // لود کردن اطلاعات از روی دیسک به محض روشن شدن سرور
            loadDataFromFiles();
        } catch (Exception e) {
            System.out.println("خطا در ایجاد پوشه دیتابیس: " + e.getMessage());
        }
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
        if (registeredMap.containsKey(username)) {
            return SignupResult.ALREADY_EXISTS;
        }
        List<ChatRoom> initialChats = new ArrayList<>();
        ChatRoom savedMessages = new ChatRoom("saved_messages", "Saved Messages", false);
        savedMessages.addMessage(new ChatMessage("System", "به پیام‌های ذخیره شده خوش آمدید!", false));
        initialChats.add(savedMessages);
        userChatsMap.put(username, initialChats);

        User newUser = new User(username, id, password);
        registeredMap.put(username, newUser);
        saveUsersToFile();

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

        // مرتب سازی بر اساس...
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
                room.getHistory().add(msg);
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

        ChatMessage targetMsg = null;
        for (ChatMessage msg : room.getMessages()) {
            if (msg.getId().equals(messageId) && msg.getSender().equals(username)) {
                targetMsg = msg;
                break;
            }
        }

        if (targetMsg != null) {
            room.getHistory().add(targetMsg);
            room.getMessages().remove(targetMsg);
            return true;
        }
        return false;
    }

    public boolean reportMessage(String username, String chatId, String messageId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;

        for (ChatMessage msg : room.getMessages()) {
            if (msg.getId().equals(messageId)) {
                msg.setReported(true);
                System.out.println(" پیام گزارش شد! فرستنده: " + msg.getSender() + " | محتوا: " + msg.getContent());
                return true;
            }
        }
        return false;
    }

    public List<String> getCommonGroups(String user1, String user2) {
        List<String> commonGroups = new ArrayList<>();
        List<ChatRoom> chats = userChatsMap.get(user1);
        if (chats != null) {
            for (ChatRoom chat : chats) {
                if (chat.isGroup() && chat.getMembers().contains(user2)) {
                    commonGroups.add(chat.getName());
                }
            }
        }
        return commonGroups;
    }

    public boolean toggleBlockUser(String username, String chatId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || room.isGroup())
            return false;
        room.setBlocked(!room.isBlocked()); // معکوس کردن وضعیت بلاک
        return true;
    }

    public boolean toggleArchiveChat(String username, String chatId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;
        room.setArchived(!room.isArchived()); // معکوس کردن وضعیت آرشیو
        return true;
    }

    public boolean updateGroupInfo(String username, String chatId, String newName, String newAvatar) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        room.setName(newName);
        room.setAvatarUrl(newAvatar);
        return true;
    }

    public boolean leaveGroup(String username, String chatId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        room.removeMember(username);
        return true;
    }

    public boolean addMemberToGroup(String username, String chatId, String newMemberUsername) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        room.addMember(newMemberUsername);
        return true;
    }

    public boolean updateProfile(String username, String newAvatarUrl) {
        User user = registeredMap.get(username);
        if (user == null)
            return false;

        if (newAvatarUrl != null && !newAvatarUrl.isBlank()) {
            user.setAvatarURL(newAvatarUrl);
        }
        return true;
    }

    public String updateId(String username, String newId) {
        User currentUser = registeredMap.get(username);
        if (currentUser == null)
            return "USER_NOT_FOUND";

        for (User u : registeredMap.values()) {
            if (u.getID().equals(newId) && !u.getUsername().equals(username)) {
                return "DUPLICATE_ID";
            }
        }

        currentUser.setID(newId);
        return "SUCCESS";
    }

    public boolean toggleTheme(String username, boolean darkMode) {
        User user = registeredMap.get(username);
        if (user == null)
            return false;
        user.setDarkMode(darkMode);
        return true;
    }

    public boolean deleteUserAccount(String username) {
        if (!registeredMap.containsKey(username))
            return false;

        userChatsMap.remove(username);
        registeredMap.remove(username);

        return true;
    }

    public String addContactByUniqueId(String username, String targetUniqueId) {
        User currentUser = registeredMap.get(username);
        if (currentUser == null)
            return "USER_NOT_FOUND";

        User targetUser = null;
        for (User u : registeredMap.values()) {
            if (u.getID().equals(targetUniqueId)) {
                targetUser = u;
                break;
            }
        }

        if (targetUser == null)
            return "TARGET_NOT_FOUND";

        if (targetUser.getUsername().equals(username))
            return "CANNOT_ADD_SELF";

        if (currentUser.getContacts().contains(targetUser.getUsername())) {
            return "ALREADY_CONTACT";
        }

        currentUser.getContacts().add(targetUser.getUsername());
        createPrivateChatRoom(username, targetUser.getUsername());

        return "SUCCESS";
    }

    private void createPrivateChatRoom(String user1, String user2) {
        List<ChatRoom> user1Chats = userChatsMap.computeIfAbsent(user1, k -> new ArrayList<>());

        for (ChatRoom room : user1Chats) {
            if (!room.isGroup() && room.getId().equals(user2))
                return;
        }

        ChatRoom roomForUser1 = new ChatRoom(user2, user2, false);
        user1Chats.add(roomForUser1);

        List<ChatRoom> user2Chats = userChatsMap.computeIfAbsent(user2, k -> new ArrayList<>());
        ChatRoom roomForUser2 = new ChatRoom(user1, user1, false);
        user2Chats.add(roomForUser2);
    }

    public boolean createNewGroup(String id, String username, String groupName, List<String> initialMembers) {

        ChatRoom newGroup = new ChatRoom(id, groupName, true);
        newGroup.addMember(username);

        if (initialMembers != null) {
            for (String member : initialMembers) {
                newGroup.addMember(member);
            }
        }

        for (String member : newGroup.getMembers()) {
            List<ChatRoom> chats = userChatsMap.computeIfAbsent(member, k -> new ArrayList<>());
            chats.add(newGroup);
        }

        return true;
    }

    public boolean adminDeleteGroup(String groupId) {
        boolean found = false;
        for (List<ChatRoom> chats : userChatsMap.values()) {
            found |= chats.removeIf(room -> room.isGroup() && room.getId().equals(groupId));
        }
        return found;
    }

    public void printReportedMessages() {
        System.out.println("\n--- list of reports ---");
        int count = 0;
        for (List<ChatRoom> chats : userChatsMap.values()) {
            for (ChatRoom room : chats) {
                for (ChatMessage msg : room.getMessages()) {
                    if (msg.isReported()) {
                        count++;
                        System.out.printf("[%d] chat: %s | spam sender: %s | message content: \"%s\"\n",
                                count, room.getName(), msg.getSender(), msg.getContent());
                    }
                }
            }
        }
        if (count == 0) {
            System.out.println("find nothing!");
        }
        System.out.println("---------------------------------------");
    }

    // ذخیره کل کاربران سیستم در فایل متنی users.txt
    private synchronized void saveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            for (User u : registeredMap.values()) {
                String contactsStr = String.join(",", u.getContacts());
                // رمزنگاری
                String encryptedPassword = CryptoHelper.encrypt(u.getPassword());

                writer.printf("%s|%s|%s|%b|%s\n",
                        u.getUsername(), encryptedPassword, u.getID(), u.isDarkMode(), contactsStr);
            }
        } catch (IOException e) {
            System.err.println("خطا در ذخیره فایل کاربران: " + e.getMessage());
        }
    }

    // ذخیره چت ها در فایل
    public synchronized void saveChatHistoryToFile(String roomId, List<ChatMessage> messages) {
        File chatFile = new File(CHATS_DIR + roomId + ".txt");
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(chatFile), StandardCharsets.UTF_8))) {
            for (ChatMessage msg : messages) {
                // رمرنگاری
                String encryptedContent = CryptoHelper.encrypt(msg.getContent());
                writer.printf("%s|%b|%s\n", msg.getSender(), msg.isReported(), encryptedContent);
            }
        } catch (IOException e) {
            System.err.println("خطا در ذخیره تاریخچه چت: " + e.getMessage());
        }
    }

    // لوذ کردن کاربران
    private void loadDataFromFiles() {
        File uFile = new File(USERS_FILE);
        if (!uFile.exists())
            return;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(uFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4)
                    continue;

                String username = parts[0];
                String encryptedPassword = parts[1];
                String userId = parts[2];
                boolean isDarkMode = Boolean.parseBoolean(parts[3]);

                // رمزگشایی
                String plainPassword = CryptoHelper.decrypt(encryptedPassword);

                User user = new User(username, userId, plainPassword);
                user.setDarkMode(isDarkMode);

                if (parts.length == 5 && !parts[4].isEmpty()) {
                    for (String contact : parts[4].split(",")) {
                        user.getContacts().add(contact);
                    }
                }
                registeredMap.put(username, user);
            }
        } catch (IOException e) {
            System.err.println("خطا در لود کاربران: " + e.getMessage());
        }

        for (User user : registeredMap.values()) {
            List<ChatRoom> rooms = userChatsMap.computeIfAbsent(user.getUsername(), k -> new ArrayList<>());
            for (String contactJson : user.getContacts()) {
                String roomId = generatePrivateRoomId(user.getUsername(), contactJson);
                ChatRoom room = new ChatRoom(roomId, contactJson, false);

                loadChatMessagesFromFile(room);
                rooms.add(room);
            }
        }
    }

    private void loadChatMessagesFromFile(ChatRoom room) {
        File chatFile = new File(CHATS_DIR + room.getId() + ".txt");
        if (!chatFile.exists())
            return;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(chatFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 3)
                    continue;

                String sender = parts[0];
                boolean isReported = Boolean.parseBoolean(parts[1]);
                String encryptedContent = parts[2];
                // رمزگشایی
                String plainText = CryptoHelper.decrypt(encryptedContent);

                ChatMessage msg = new ChatMessage(sender, plainText, false);
                if (isReported)
                    msg.setReported(true);
                room.getMessages().add(msg);
            }
        } catch (IOException e) {
            System.err.println("خطا در لود پیام‌های چت: " + e.getMessage());
        }
    }

    private String generatePrivateRoomId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
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

    public Map<String, User> getRegisteredMap() {
        return registeredMap;
    }

    public Map<String, List<ChatRoom>> getUserChatsMap() {
        return userChatsMap;
    }

}
