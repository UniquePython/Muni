package com.muni.client;

import com.muni.protocol.Hello;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.RoomMessage;
import com.muni.protocol.Welcome;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public final class MuniClient {

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int port = 6969;
        String room = "general";

        try (Socket socket = new Socket(host, port);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.print("Username: ");
            String username = consoleReader.readLine();

            MessageCodec.write(out, new Hello(username));
            out.flush();

            // Read Welcome
            Message response = MessageCodec.read(in);

            if (!(response instanceof Welcome)) {
                throw new IOException("Expected Welcome from server");
            }

            System.out.println("Connected!");

            while (true) {
                System.out.print("> ");
                String text = consoleReader.readLine();

                if (text == null) {
                    break;
                }

                RoomMessage message = new RoomMessage(username, room, text);

                MessageCodec.write(out, message);
                out.flush();

                response = MessageCodec.read(in);

                System.out.println("Received: " + response);
            }
        }
    }
}
