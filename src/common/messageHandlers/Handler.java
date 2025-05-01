package common.messageHandlers;

import common.util.AbstractMsgVisitor;
import common.util.Deserializer;
import common.messages.Message;

public abstract class Handler<T extends Message> extends AbstractMsgVisitor implements Deserializer<T> {

}
