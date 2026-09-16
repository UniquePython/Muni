package com.muni.protocol;

public record RoomMessage(String sender, String room, String text) implements Message {

    public RoomMessage {
        if (sender == null || sender.isBlank())
            throw new IllegalArgumentException("sender cannot be blank");

        if (room == null || room.isBlank())
            throw new IllegalArgumentException("room cannot be blank");

        if (text == null)
            throw new IllegalArgumentException("text cannot be null");
    }

}
