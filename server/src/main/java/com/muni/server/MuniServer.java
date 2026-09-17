package com.muni.server;

import com.muni.protocol.Hello;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.Rejected;
import com.muni.protocol.RoomMessage;
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

public final class MuniServer {

    private static final int PORT = 6969;

    private final ExecutorService clientPool = Executors.newCachedThreadPool();

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

        try (
                clientSocket;
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
            String username = performHandshake(in, out, address);

            if (username == null) {
                return;
            }

            while (true) {
                Message message = MessageCodec.read(in);

                if (!(message instanceof RoomMessage roomMessage)) {
                    MessageCodec.write(out, new Rejected("invalid message; expected ROOM_MESSAGE"));
                    out.flush();
                    return;
                }

                if (!username.equals(roomMessage.sender())) {
                    MessageCodec.write(out, new Rejected("sender does not match username"));
                    out.flush();
                    return;
                }

                System.out.println("[" + roomMessage.room() + "] " + roomMessage.sender() + ": " + roomMessage.text());
            }

        } catch (EOFException e) {
            System.out.println("Client " + address + " disconnected");

        } catch (IOException e) {
            System.err.println("Client " + address + " error: " + e.getMessage());
        }
    }

    private String performHandshake(
            DataInputStream in,
            DataOutputStream out,
            SocketAddress address) throws IOException {
        Message message = MessageCodec.read(in);

        if (!(message instanceof Hello hello)) {
            MessageCodec.write(out, new Rejected("invalid handshake; expected HELLO"));
            out.flush();
            return null;
        }

        String username = hello.username();

        System.out.println("Client " + address + " registered as: " + username);

        MessageCodec.write(out, new Welcome());
        out.flush();

        return username;
    }
}
