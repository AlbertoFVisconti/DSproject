package common.util;

import common.messages.Message;

public interface Deserializer<T extends Message> {
    T deserialize(String payload);
}
