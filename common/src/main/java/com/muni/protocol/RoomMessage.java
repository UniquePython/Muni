package com.muni.protocol;

public record RoomMessage(String sender, String room, String text) implements Message {
}
