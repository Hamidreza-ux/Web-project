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
        ChatRoom savedMessages = new ChatRoom("saved_messages", "Saved Messages", "assets/saved.png", false);
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

    public boolean updateProfile(String username, String newName, String newAvatarUrl) {
        User user = registeredMap.get(username);
        if (user == null)
            return false;

        if (newName != null && !newName.isBlank()) {
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

        registeredMap.remove(username);
        userChatsMap.remove(username);

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

        ChatRoom roomForUser1 = new ChatRoom(user2, user2, "assets/default_avatar.png", false);
        user1Chats.add(roomForUser1);

        List<ChatRoom> user2Chats = userChatsMap.computeIfAbsent(user2, k -> new ArrayList<>());
        ChatRoom roomForUser2 = new ChatRoom(user1, user1, "assets/default_avatar.png", false);
        user2Chats.add(roomForUser2);
    }

    public boolean createNewGroup(String creatorUsername, String groupName, List<String> initialMembers) {
        String groupId = "group_" + System.currentTimeMillis();

        ChatRoom newGroup = new ChatRoom(groupId, groupName, "assets/group_avatar.png", true);
        newGroup.addMember(creatorUsername);

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
        System.out.println("\n--- لیست پیام‌های گزارش شده به ادمین ---");
        int count = 0;
        for (List<ChatRoom> chats : userChatsMap.values()) {
            for (ChatRoom room : chats) {
                for (ChatMessage msg : room.getMessages()) {
                    if (msg.isReported()) {
                        count++;
                        System.out.printf("[%d] چت: %s | فرستنده اسپم: %s | محتوای پیام: \"%s\"\n",
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
