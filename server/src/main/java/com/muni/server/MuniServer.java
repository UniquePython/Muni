package com.muni.server;

import com.muni.protocol.Hello;
import com.muni.protocol.IncomingRoomMessage;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.Rejected;
import com.muni.protocol.OutgoingRoomMessage;
import com.muni.protocol.Welcome;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

public final class MuniServer {

    private static final int PORT = 6969;

    private final ExecutorService clientPool = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new MuniServer().run();
    }

    private void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                clientPool.submit(() -> handleClient(clientSocket));
            }

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            clientPool.shutdown();
        }
    }

    private void handleClient(Socket clientSocket) {
        SocketAddress address = clientSocket.getRemoteSocketAddress();
        ClientSession session = null;

        try (
                clientSocket;
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            session = performHandshake(in, out, address);

            if (session == null) {
                return;
            }

            String username = session.username();

            while (true) {
                Message message = MessageCodec.read(in);

                if (!(message instanceof OutgoingRoomMessage roomMessage)) {
                    MessageCodec.write(out, new Rejected("invalid message; expected ROOM_MESSAGE"));
                    out.flush();
                    return;
                }

                IncomingRoomMessage toDeliver = new IncomingRoomMessage(roomMessage.room(), username,
                        roomMessage.text());
                System.out.println("Broadcasting to [" + toDeliver.room() + "] " + toDeliver.sender());
                broadcast(toDeliver);
            }

        } catch (EOFException e) {
            System.out.println("Client " + address + " disconnected");

        } catch (IOException e) {
            System.err.println("Client " + address + " error: " + e.getMessage());

        } finally {
            if (session != null) {
                sessions.remove(session.username(), session);
            }
        }
    }

    private ClientSession performHandshake(DataInputStream in, DataOutputStream out, SocketAddress address)
            throws IOException {
        Message message = MessageCodec.read(in);

        if (!(message instanceof Hello hello)) {
            MessageCodec.write(out, new Rejected("invalid handshake; expected HELLO"));
            out.flush();
            return null;
        }

        String username = hello.username();

        ClientSession session = new ClientSession(username, out);

        if (sessions.putIfAbsent(username, session) != null) {
            MessageCodec.write(out, new Rejected("username already taken"));
            out.flush();
            return null;
        }

        System.out.println("Client " + address + " registered as: " + username);

        MessageCodec.write(out, new Welcome());
        out.flush();

        return session;
    }

    private void broadcast(IncomingRoomMessage message) {
        for (ClientSession recipient : sessions.values()) {
            try {
                recipient.send(message);
            } catch (IOException e) {
                System.err.println("Failed to send message to " + recipient.username() + ": " + e.getMessage());
            }
        }
    }
}
