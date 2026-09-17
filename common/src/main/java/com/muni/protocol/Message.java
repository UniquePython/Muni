package com.muni.protocol;

public sealed interface Message permits Hello, Welcome, Rejected, OutgoingRoomMessage {
}
