package com.muni.protocol;

// Convert reason to enum or sealed inteface or sth later

public record Rejected(String reason) implements Message {

    public Rejected {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("reason cannot be blank");
    }

}
