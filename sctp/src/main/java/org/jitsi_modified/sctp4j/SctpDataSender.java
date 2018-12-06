package org.jitsi_modified.sctp4j;

import java.io.IOException;

public interface SctpDataSender {
    int send(byte[] data, boolean ordered, int sid, int ppid)
            throws IOException;
}
