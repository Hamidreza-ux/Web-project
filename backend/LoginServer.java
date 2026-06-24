import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LoginServer {
    private String username;
    private String password;
    private static volatile LoginServer instance;

    private final Map<String, User> registeredMap = new ConcurrentHashMap<>();
    private final Map<String, List<ChatRoom>> userChatsMap = new ConcurrentHashMap<>();

    private static final String DB_DIR = "database/";
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

    public synchronized LoginResult authenticate(String username, String password) {
        User user = registeredMap.get(username);

        if (user == null) {
            return LoginResult.USER_NOT_FOUND;
        }

        if (user.isLocked()) {
            return LoginResult.ACCOUNT_LOCKED;
        }

        if (!user.passwordIsRight(password)) {
            return LoginResult.WRONG_PASSWORD;
        }

        return LoginResult.SUCCESS;
    }

    // FIX 1: ترتیب validation اصلاح شد — initialChats فقط بعد از تأیید همه شرایط ساخته می‌شود
    // FIX 2: چک تکراری ALREADY_EXISTS که هرگز اجرا نمی‌شد حذف شد
    public synchronized SignupResult register(String username, String id, String password, String confirmPassword) {

        if (!password.equals(confirmPassword)) {
            return SignupResult.PASSWORD_MISMATCH;
        }

        if (registeredMap.containsKey(username)) {
            return SignupResult.DUPLICATE_USERNAME;
        }

        for (User u : registeredMap.values()) {
            if (u.getUsername().equalsIgnoreCase(username) && u.getID().equalsIgnoreCase(id)) {
                return SignupResult.ALREADY_EXISTS;
            }
        }

        for (User u : registeredMap.values()) {
            if (u.getID().equalsIgnoreCase(id)) {
                return SignupResult.DUPLICATE_ID;
            }
        }

        if (!isPasswordValid(password, username)) {
            return SignupResult.INVALID_PASSWORD;
        }

        // همه چک‌ها پاس شدند — حالا کاربر و چت اولیه را بساز
        User newUser = new User(username, id, password);
        registeredMap.put(username, newUser);
        saveUsersToFile();

        List<ChatRoom> initialChats = new ArrayList<>();
        ChatRoom savedMessages = new ChatRoom("saved_messages", "Saved Messages", "assets/saved.png", false);
        savedMessages.addMessage(new ChatMessage("System", "به پیام‌های ذخیره شده خوش آمدید!", false));
        initialChats.add(savedMessages);
        userChatsMap.put(username, initialChats);

        return SignupResult.SUCCESS;
    }

    private boolean isPasswordValid(String password, String username) {
        if (password.contains(username)) {
            return false;
        }
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$";
        return password.matches(regex);
    }

    public List<ChatRoom> getUserMainChats(String username, String searchQuery) {
        List<ChatRoom> allChats = userChatsMap.getOrDefault(username, new ArrayList<>());
        List<ChatRoom> filteredChats = new ArrayList<>();

        for (ChatRoom chat : allChats) {
            if (chat.isArchived())
                continue;

            if (searchQuery != null && !searchQuery.isBlank()) {
                if (!chat.getName().toLowerCase().contains(searchQuery.toLowerCase())) {
                    continue;
                }
            }
            filteredChats.add(chat);
        }

        filteredChats.sort((chat1, chat2) -> {
            if (chat1.isPinned() && !chat2.isPinned())
                return -1;
            if (!chat1.isPinned() && chat2.isPinned())
                return 1;

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
            room.getMessages().remove(targetMsg);
            room.getHistory().add(targetMsg);
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
                System.out.println("پیام گزارش شد! فرستنده: " + msg.getSender() + " | محتوا: " + msg.getContent());
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
        room.setBlocked(!room.isBlocked());
        return true;
    }

    public boolean toggleArchiveChat(String username, String chatId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null)
            return false;
        room.setArchived(!room.isArchived());
        return true;
    }

    public boolean updateGroupInfo(String username, String chatId, String newName, String newAvatar) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        if (newName != null && !newName.isBlank())
            room.setName(newName);
        if (newAvatar != null && !newAvatar.isBlank())
            room.setAvatarUrl(newAvatar);
        return true;
    }

    public boolean leaveGroup(String username, String chatId) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        room.removeMember(username);
        // حذف گروه از لیست چت‌های شخصی کاربر
        List<ChatRoom> userChats = userChatsMap.get(username);
        if (userChats != null)
            userChats.removeIf(r -> r.getId().equals(chatId));
        return true;
    }

    public boolean addMemberToGroup(String username, String chatId, String newMemberUsername) {
        ChatRoom room = findChatRoom(username, chatId);
        if (room == null || !room.isGroup())
            return false;
        if (!registeredMap.containsKey(newMemberUsername))
            return false;
        room.addMember(newMemberUsername);
        // اضافه کردن گروه به لیست چت‌های شخصی عضو جدید
        List<ChatRoom> memberChats = userChatsMap.computeIfAbsent(newMemberUsername, k -> new ArrayList<>());
        boolean alreadyIn = memberChats.stream().anyMatch(r -> r.getId().equals(chatId));
        if (!alreadyIn)
            memberChats.add(room);
        return true;
    }

    // FIX 3: updateProfile اکنون واقعاً نام کاربری را آپدیت می‌کند
    public boolean updateProfile(String username, String newName, String newAvatarUrl) {
        User user = registeredMap.get(username);
        if (user == null)
            return false;

        if (newName != null && !newName.isBlank()) {
            user.setUsername(newName);
        }
        return true;
    }

    public String updateId(String username, String newId) {
        User currentUser = registeredMap.get(username);
        if (currentUser == null)
            return "USER_NOT_FOUND";

        if (newId == null || newId.isBlank())
            return "INVALID_ID";

        for (User u : registeredMap.values()) {
            if (u.getID().equals(newId) && !u.getUsername().equals(username)) {
                return "DUPLICATE_ID";
            }
        }

        currentUser.setID(newId);
        saveUsersToFile();
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

        registeredMap.remove(username);
        userChatsMap.remove(username);
        saveUsersToFile();
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
        saveUsersToFile();

        return "SUCCESS";
    }

    private void createPrivateChatRoom(String user1, String user2) {
        List<ChatRoom> user1Chats = userChatsMap.computeIfAbsent(user1, k -> new ArrayList<>());

        for (ChatRoom room : user1Chats) {
            if (!room.isGroup() && room.getId().equals(user2))
                return;
        }

        ChatRoom roomForUser1 = new ChatRoom(user2, user2, "assets/default_avatar.png", false);
        user1Chats.add(roomForUser1);

        List<ChatRoom> user2Chats = userChatsMap.computeIfAbsent(user2, k -> new ArrayList<>());
        ChatRoom roomForUser2 = new ChatRoom(user1, user1, "assets/default_avatar.png", false);
        user2Chats.add(roomForUser2);
    }

    public boolean createNewGroup(String creatorUsername, String groupName, List<String> initialMembers) {
        if (groupName == null || groupName.isBlank())
            return false;

        String groupId = "group_" + System.currentTimeMillis();
        ChatRoom newGroup = new ChatRoom(groupId, groupName, "assets/group_avatar.png", true);
        newGroup.addMember(creatorUsername);

        if (initialMembers != null) {
            for (String member : initialMembers) {
                if (registeredMap.containsKey(member))
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
        System.out.println("\n--- لیست پیام‌های گزارش شده به ادمین ---");
        int count = 0;
        Set<String> seenIds = new HashSet<>();
        for (List<ChatRoom> chats : userChatsMap.values()) {
            for (ChatRoom room : chats) {
                for (ChatMessage msg : room.getMessages()) {
                    if (msg.isReported() && seenIds.add(msg.getId())) {
                        count++;
                        System.out.printf("[%d] چت: %s | فرستنده: %s | محتوا: \"%s\"\n",
                                count, room.getName(), msg.getSender(), msg.getContent());
                    }
                }
            }
        }
        if (count == 0) {
            System.out.println("هیچ پیام گزارش شده‌ای وجود ندارد.");
        }
        System.out.println("---------------------------------------");
    }

    private synchronized void saveUsersToFile() {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(USERS_FILE), StandardCharsets.UTF_8))) {
            for (User u : registeredMap.values()) {
                String contactsStr = String.join(",", u.getContacts());
                writer.printf("%s|%s|%s|%b|%s\n",
                        u.getUsername(), u.getPassword(), u.getID(), u.isDarkMode(), contactsStr);
            }
        } catch (IOException e) {
            System.err.println("خطا در ذخیره فایل کاربران: " + e.getMessage());
        }
    }

    public synchronized void saveChatHistoryToFile(String roomId, List<ChatMessage> messages) {
        File chatFile = new File(CHATS_DIR + roomId + ".txt");
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(chatFile), StandardCharsets.UTF_8))) {
            for (ChatMessage msg : messages) {
                String encryptedContent = CryptoHelper.encrypt(msg.getContent());
                writer.printf("%s|%b|%s\n", msg.getSender(), msg.isReported(), encryptedContent);
            }
        } catch (IOException e) {
            System.err.println("خطا در ذخیره تاریخچه چت: " + e.getMessage());
        }
    }

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

                String uname = parts[0];
                String pass = parts[1];
                String userId = parts[2];
                boolean isDarkMode = Boolean.parseBoolean(parts[3]);

                User user = new User(uname, userId, pass);
                user.setDarkMode(isDarkMode);

                if (parts.length == 5 && !parts[4].isEmpty()) {
                    for (String contact : parts[4].split(",")) {
                        if (!contact.isBlank())
                            user.getContacts().add(contact);
                    }
                }
                registeredMap.put(uname, user);
            }
        } catch (IOException e) {
            System.err.println("خطا در لود کاربران: " + e.getMessage());
        }

        // بارگذاری چت‌های خصوصی از فایل
        for (User user : registeredMap.values()) {
            List<ChatRoom> rooms = userChatsMap.computeIfAbsent(user.getUsername(), k -> new ArrayList<>());

            // اضافه کردن Saved Messages برای هر کاربر
            boolean hasSaved = rooms.stream().anyMatch(r -> r.getId().equals("saved_messages"));
            if (!hasSaved) {
                ChatRoom savedMessages = new ChatRoom("saved_messages", "Saved Messages", "assets/saved.png", false);
                rooms.add(0, savedMessages);
            }

            for (String contact : user.getContacts()) {
                String roomId = generatePrivateRoomId(user.getUsername(), contact);
                // جلوگیری از اضافه شدن تکراری
                boolean exists = rooms.stream().anyMatch(r -> r.getId().equals(roomId));
                if (!exists) {
                    ChatRoom room = new ChatRoom(roomId, contact, "assets/default_avatar.png", false);
                    loadChatMessagesFromFile(room);
                    rooms.add(room);
                }
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

    public void addMessageToRoom(String roomId, ChatMessage message) {
        for (List<ChatRoom> chats : userChatsMap.values()) {
            for (ChatRoom room : chats) {
                if (room.getId().equals(roomId)) {
                    room.getMessages().add(message);
                    saveChatHistoryToFile(roomId, room.getMessages());
                    return;
                }
            }
        }
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