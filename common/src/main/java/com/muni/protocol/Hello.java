package com.muni.protocol;

public record Hello(String username) implements Message {

    public Hello {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("username cannot be blank");
    }

}
