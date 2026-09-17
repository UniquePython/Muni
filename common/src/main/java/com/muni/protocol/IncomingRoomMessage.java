package com.muni.protocol;

public record IncomingRoomMessage(String room, String sender, String text) implements Message {

    public IncomingRoomMessage {
        if (room == null || room.isBlank())
            throw new IllegalArgumentException("room cannot be blank");

        if (sender == null || sender.isBlank())
            throw new IllegalArgumentException("sender cannot be blank");

        if (text == null)
            throw new IllegalArgumentException("text cannot be null");
    }

}