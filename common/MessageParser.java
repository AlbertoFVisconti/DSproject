package common;

public class MessageParser {
    public static MessageType parseType(String line) {
        String type = line.split(":")[0];
        try {
            return MessageType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
