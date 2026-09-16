package com.muni.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class MessageCodec {

    private MessageCodec() {
    }

    public static void write(DataOutputStream out, Message message) throws IOException {
        switch (message) {
            case Hello h -> {
                out.writeByte(MessageType.HELLO.tag());
                StringCodec.write(out, h.username());
            }
            case Welcome _ -> {
                out.writeByte(MessageType.WELCOME.tag());
            }
            case Rejected r -> {
                out.writeByte(MessageType.REJECTED.tag());
                StringCodec.write(out, r.reason());
            }
            case RoomMessage m -> {
                out.writeByte(MessageType.ROOM_MESSAGE.tag());
                StringCodec.write(out, m.room());
                StringCodec.write(out, m.sender());
                StringCodec.write(out, m.text());
            }
        }
    }

    public static Message read(DataInputStream in) throws IOException {
        int tag = in.readUnsignedByte();

        MessageType type = MessageType.fromTag(tag);

        return switch (type) {
            case HELLO -> {
                String username = StringCodec.read(in);
                yield new Hello(username);
            }

            case WELCOME -> new Welcome();

            case REJECTED -> {
                String reason = StringCodec.read(in);
                yield new Rejected(reason);
            }

            case ROOM_MESSAGE -> {
                String room = StringCodec.read(in);
                String sender = StringCodec.read(in);
                String text = StringCodec.read(in);

                yield new RoomMessage(sender, room, text);
            }
        };
    }
}
