import java.time.LocalDateTime;

public class ChatMessage {     // message features
    private String sender; 
    private String content;
    private LocalDateTime timestamp; //message time

    public ChatMessage(String sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now(); //system current time
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public String getContent() { return content; }
    public String getSender() { return sender; }
}
