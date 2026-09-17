package com.muni.client;

import com.muni.protocol.Hello;
import com.muni.protocol.IncomingRoomMessage;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.OutgoingRoomMessage;
import com.muni.protocol.Rejected;
import com.muni.protocol.Welcome;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

public final class MuniClient {

    private static final String HOST = "localhost";
    private static final int PORT = 6969;
    private static final String ROOM = "general";

    private volatile boolean shuttingDown = false;

    public static void main(String[] args) {
        new MuniClient().run();
    }

    private void run() {
        try (
                Socket socket = new Socket(HOST, PORT);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))) {
            performHandshake(in, out, consoleReader);

            System.out.println("Connected!");

            Thread receiverThread = new Thread(() -> receiveMessages(in), "server-receiver");
            receiverThread.start();

            sendMessages(out, consoleReader);

            shuttingDown = true;
            socket.close();

        } catch (ConnectException e) {
            System.err.println("Could not connect to server at " + HOST + ":" + PORT);

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid input: " + e.getMessage());

        } catch (IOException e) {
            if (!shuttingDown) {
                System.err.println("Client error: " + e.getMessage());
            }
        }
    }

    private void performHandshake(
            DataInputStream in,
            DataOutputStream out,
            BufferedReader consoleReader) throws IOException {
        System.out.print("Username: ");

        String username = consoleReader.readLine();

        MessageCodec.write(out, new Hello(username));
        out.flush();

        Message response = MessageCodec.read(in);

        if (response instanceof Welcome) {
            return;
        }

        if (response instanceof Rejected rejected) {
            throw new IOException("Connection rejected: " + rejected.reason());
        }

        throw new IOException("Expected Welcome from server");
    }

    private void sendMessages(
            DataOutputStream out,
            BufferedReader consoleReader) throws IOException {
        while (true) {
            System.out.print("> ");

            String text = consoleReader.readLine();

            if (text == null) {
                return;
            }

            OutgoingRoomMessage message = new OutgoingRoomMessage(ROOM, text);

            MessageCodec.write(out, message);
            out.flush();
        }
    }

    private void receiveMessages(DataInputStream in) {
        try {
            while (true) {
                Message message = MessageCodec.read(in);

                if (message instanceof IncomingRoomMessage roomMessage) {
                    System.out.println(
                            "\n[" + roomMessage.room() + "] "
                                    + roomMessage.sender()
                                    + ": "
                                    + roomMessage.text());
                    System.out.print("> ");

                } else if (message instanceof Rejected rejected) {
                    System.err.println("\nServer rejected message: " + rejected.reason());
                    System.out.print("> ");

                } else {
                    System.err.println("\nUnexpected message from server: " + message);
                    System.out.print("> ");
                }
            }

        } catch (EOFException e) {
            if (!shuttingDown) {
                System.out.println("\nServer disconnected.");
            }

        } catch (IOException e) {
            if (!shuttingDown) {
                System.err.println("\nReceiver error: " + e.getMessage());
            }
        }
    }
}
