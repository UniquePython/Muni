package com.muni.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public sealed interface HandshakeResult permits HandshakeResult.Success, HandshakeResult.Failure {

	record Success(Socket socket, DataInputStream in, DataOutputStream out, String username)
			implements HandshakeResult {

		public Success {
			if (socket == null)
				throw new IllegalArgumentException("socket cannot be null");

			if (in == null)
				throw new IllegalArgumentException("in cannot be null");

			if (out == null)
				throw new IllegalArgumentException("out cannot be null");

			if (username == null || username.isBlank())
				throw new IllegalArgumentException("username cannot be blank");
		}

	}

	record Failure(String reason) implements HandshakeResult {

		public Failure {
			if (reason == null || reason.isBlank())
				throw new IllegalArgumentException("reason cannot be blank");
		}

	}
}
