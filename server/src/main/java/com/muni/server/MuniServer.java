package com.muni.server;

import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.RoomMessage;
import com.muni.protocol.Welcome;
import com.muni.protocol.Hello;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public final class MuniServer {

    public static void main(String[] args) throws IOException {
        int port = 6969; // arbitrary choice for now

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);

            Socket clientSocket = serverSocket.accept();
            SocketAddress clientSocketAddress = clientSocket.getRemoteSocketAddress();
            System.out.println("Client connected: " + clientSocketAddress);

            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                Message firstMessage = MessageCodec.read(in);
                if (firstMessage instanceof Hello hello) {
                    System.out.println("Username: " + hello.username());
                    MessageCodec.write(out, new Welcome());
                } else {
                    throw new IOException("Expected Hello message");
                }

                try {
                    while (true) {
                        Message message = MessageCodec.read(in);

                        System.out.println("Received: " + message);

                        if (message instanceof RoomMessage roomMessage) {
                            System.out.println(
                                    "[" + roomMessage.room() + "] " + roomMessage.sender() + ": " + roomMessage.text());

                            MessageCodec.write(out, roomMessage);
                            out.flush();
                        }
                    }
                } catch (EOFException e) {
                    System.out.println("Client " + clientSocketAddress + " disconnected");
                }

            }
        }
    }
}
