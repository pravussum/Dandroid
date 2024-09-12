package net.mortalsilence.dandroid.comm;

import java.io.IOException;

public interface CommunicationController {
    void connect() throws IOException;

    void disconnect();

    byte[] sendRobustRequest(byte[] operation, byte[] register) throws IOException;

    byte[] sendRobustRequest(byte[] operation, byte[] register, byte[] value) throws IOException;
}
