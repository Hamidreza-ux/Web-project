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

    public ChatRoom(String id, String name, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.unreadCount = 0;
        this.isPinned = false;
        this.isArchived = false;
        this.messages = new ArrayList<>();
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
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

    public List<ChatMessage> getMessages() {
        return messages;
    }
}
