package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final int    groupId;
    private final int    senderId;
    private final String timestamp;

    public Message(int senderId, int groupId, String text, String timestamp) {
        this.senderId = senderId;
        this.groupId = groupId;
        this.text    = text;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] User " + senderId + ": " + text;
    }

    public String getText()    { return text; }
    public int    getGroupId() { return groupId; }
    public int    getSenderId() { return senderId; }
    public String getTimestamp() { return timestamp; }
}