import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessage { // message features
    private String sender;
    private String content;
    private LocalDateTime timestamp; // message time
    private String id;
    private boolean isFile;
    private boolean isEdited;
    private boolean isReported;

    public ChatMessage(String sender, String content, boolean isFile) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now(); // system current time
        this.id = UUID.randomUUID().toString(); // generate Id
        this.isFile = isFile;
        this.isEdited = false;
        this.isReported = false;
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isFile() {
        return isFile;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public boolean isReported() {
        return isReported;
    }

    public void setReported(boolean reported) {
        isReported = reported;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
