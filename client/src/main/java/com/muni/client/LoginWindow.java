package com.muni.client;

import com.muni.protocol.Hello;
import com.muni.protocol.Message;
import com.muni.protocol.MessageCodec;
import com.muni.protocol.Rejected;
import com.muni.protocol.Welcome;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public final class LoginWindow extends JFrame {

    private static final String HOST = "localhost";
    private static final int PORT = 6969;

    private final JTextField usernameField = new JTextField(20);
    private final JButton connectButton = new JButton("Connect");
    private final JLabel statusLabel = new JLabel(" "); // reserved space for error messages

    public LoginWindow() {
        super("Muni - Login");

        JPanel formPanel = new JPanel(new FlowLayout());
        formPanel.add(new JLabel("Username:"));
        formPanel.add(usernameField);
        formPanel.add(connectButton);

        setLayout(new BorderLayout());
        add(formPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        connectButton.addActionListener(e -> onConnectClicked());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack(); // sizes the window to fit its components' preferred sizes
        setLocationRelativeTo(null); // centers the window on screen
    }

    private void onConnectClicked() {
        String username = usernameField.getText();

        if (username.isBlank()) {
            statusLabel.setText("Username cannot be blank.");
            return;
        }

        connectButton.setEnabled(false);
        statusLabel.setText("Connecting...");

        SwingWorker<HandshakeResult, Void> worker = new SwingWorker<>() {
            @Override
            protected HandshakeResult doInBackground() {
                Socket socket = null;
                boolean ownershipTransferred = false;

                try {
                    socket = new Socket(HOST, PORT);

                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    MessageCodec.write(out, new Hello(username));
                    out.flush();

                    Message response = MessageCodec.read(in);

                    if (response instanceof Welcome) {
                        HandshakeResult.Success result = new HandshakeResult.Success(socket, in, out, username);

                        ownershipTransferred = true;
                        return result;
                    }

                    if (response instanceof Rejected rejected) {
                        return new HandshakeResult.Failure(rejected.reason());
                    }

                    return new HandshakeResult.Failure("Unexpected response from server");

                } catch (IOException e) {
                    return new HandshakeResult.Failure("Connection failed: " + e.getMessage());

                } catch (IllegalArgumentException e) {
                    return new HandshakeResult.Failure("Invalid input: " + e.getMessage());

                } finally {
                    if (!ownershipTransferred && socket != null) {
                        try {
                            socket.close();
                        } catch (IOException _) {
                            // Already handling the original failure.
                        }
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    HandshakeResult result = get();

                    switch (result) {
                        case HandshakeResult.Success success -> {
                            dispose();

                            System.out.println("Connected as: " + success.username());

                            // TODO: construct and show the main chat window
                        }

                        case HandshakeResult.Failure failure -> {
                            connectButton.setEnabled(true);
                            statusLabel.setText(failure.reason());
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    connectButton.setEnabled(true);
                    statusLabel.setText("Connection interrupted");

                } catch (java.util.concurrent.ExecutionException e) {
                    connectButton.setEnabled(true);
                    statusLabel.setText("Unexpected connection error: " + e.getCause());
                }
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
