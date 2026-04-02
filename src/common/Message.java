package common;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String text;
    private final int    groupId;

    public Message(int groupId, String text) {
        this.groupId = groupId;
        this.text    = text;
    }

    @Override
    public String toString() {
        return "[groupe " + groupId + "] " + text;
    }

    public String getText()    { return text; }
    public int    getGroupId() { return groupId; }
}