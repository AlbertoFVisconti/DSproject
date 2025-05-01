package common.util;

public class NotLeaderException extends IllegalStateException {
    public NotLeaderException(String message) {
        super(message);
    }
}
