import java.util.ArrayList;
import java.util.List;

public class ChatRoom { // all features in the chat room
    private String id;
    private String name;
    private String avatarUrl; // آدرس عکس حساب یا گروه
    private int unreadCount; // تعداد پیام‌های جدید دریافتی
    private boolean isPinned; // قابلیت سنجاق (Pin) کردن
    private boolean isArchived; // پوشه آرشیو
    private List<ChatMessage> messages; // لیست کل پیام‌های این چت
    private String username;
    private boolean isGroup;
    private boolean isBlocked;
    private List<String> members;
    private List<ChatMessage> history;

    public ChatRoom(String id, String name, boolean isGroup) {
        this.id = id;
        this.name = name;
        this.unreadCount = 0;
        this.isPinned = false;
        this.isArchived = false;
        this.messages = new ArrayList<>();
        this.isGroup = isGroup;
        if (isGroup) {
            this.username = "";
        } else {
            this.username = id;
        }
        if (isGroup) {
            this.avatarUrl = "https://api.dicebear.com/7.x/identicon/svg?seed=" + id;
        } else {
            this.avatarUrl = "https://api.dicebear.com/7.x/bottts/svg?seed=" + name;
        }
        this.isBlocked = false;
        this.members = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    // متد کمکی برای دریافت آخرین پیام این اتاق چت
    public ChatMessage getLastMessage() {
        if (messages.isEmpty())
            return null;
        return messages.get(messages.size() - 1);
    }

    // متد اضافه کردن پیام جدید
    public void addMessage(ChatMessage msg) {
        this.messages.add(msg);
    }

    // متد اضافه کردن عضو جدید به گروه
    public void addMember(String user) {
        if (!members.contains(user))
            members.add(user);
    }

    // ترک گروه
    public void removeMember(String user) {
        members.remove(user);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public void setArchived(boolean archived) {
        isArchived = archived;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public List<String> getMembers() {
        return members;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }
}
