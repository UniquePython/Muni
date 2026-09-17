package com.muni.protocol;

public record OutgoingRoomMessage(String room, String text) implements Message {

    public OutgoingRoomMessage {
        if (room == null || room.isBlank())
            throw new IllegalArgumentException("room cannot be blank");

        if (text == null)
            throw new IllegalArgumentException("text cannot be null");
    }

}
