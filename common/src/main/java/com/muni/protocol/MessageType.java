package com.muni.protocol;

public enum MessageType {
    HELLO(0),
    WELCOME(1),
    REJECTED(2),
    OUTGOING_ROOM_MESSAGE(3),
    INCOMING_ROOM_MESSAGE(4);

    private final int tag;

    MessageType(int tag) {
        this.tag = tag;
    }

    public int tag() {
        return tag;
    }

    public static MessageType fromTag(int tag) {
        for (MessageType type : values()) {
            if (type.tag == tag) {
                return type;
            }
        }

        throw new IllegalArgumentException("unknown message tag: " + tag);
    }
}
