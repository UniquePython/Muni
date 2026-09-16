package com.muni.protocol;

public enum MessageType {
    HELLO(0),
    WELCOME(1),
    REJECTED(2),
    ROOM_MESSAGE(3);

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
