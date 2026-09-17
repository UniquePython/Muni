package com.muni.server;

import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;

import java.io.DataOutputStream;
import java.io.IOException;

final class ClientSession {

    private final String username;
    private final DataOutputStream out;

    ClientSession(String username, DataOutputStream out) {
        this.username = username;
        this.out = out;
    }

    String username() {
        return username;
    }

    synchronized void send(Message message) throws IOException {
        MessageCodec.write(out, message);
        out.flush();
    }
}
