package com.muni.client;

import com.muni.protocol.IncomingRoomMessage;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.OutgoingRoomMessage;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public final class ChatWindow extends JFrame {

    private static final String ROOM = "general";

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final String username;

    private final JTextArea chatArea = new JTextArea();
    private final JTextField inputField = new JTextField();
    private final JButton sendButton = new JButton("Send");

    private volatile boolean shuttingDown = false;

    public ChatWindow(HandshakeResult.Success handshake) {
        super("Muni - " + handshake.username());
        this.socket = handshake.socket();
        this.in = handshake.in();
        this.out = handshake.out();
        this.username = handshake.username();

        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> onSend());
        inputField.addActionListener(e -> onSend());

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onClose();
            }
        });

        setSize(500, 400);
        setLocationRelativeTo(null);

        startReceiving();
    }

    private void onSend() {
        String text = inputField.getText();
        if (text.isBlank()) {
            return;
        }
        inputField.setText("");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws IOException {
                MessageCodec.write(out, new OutgoingRoomMessage(ROOM, text));
                out.flush();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    chatArea.append("Failed to send message: " + e.getCause() + "\n");
                }
            }
        };
        worker.execute();
    }

    private void startReceiving() {
        Thread receiverThread = new Thread(() -> {
            try {
                while (true) {
                    Message message = MessageCodec.read(in);

                    if (message instanceof IncomingRoomMessage roomMessage) {
                        SwingUtilities.invokeLater(() -> chatArea.append("[" + roomMessage.room() + "] "
                                + roomMessage.sender() + ": " + roomMessage.text() + "\n"));
                    }
                }
            } catch (EOFException e) {
                SwingUtilities.invokeLater(() -> chatArea.append("Server disconnected.\n"));
            } catch (IOException e) {
                if (!shuttingDown) {
                    SwingUtilities.invokeLater(() -> chatArea.append("Connection error: " + e.getMessage() + "\n"));
                }
            }
        }, "server-receiver");
        receiverThread.start();
    }

    private void onClose() {
        shuttingDown = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        dispose();
        System.exit(0);
    }
}
