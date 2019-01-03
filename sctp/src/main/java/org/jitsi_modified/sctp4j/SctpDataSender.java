package org.jitsi_modified.sctp4j;

import java.io.IOException;

/**
 * Interface the sctp socket uses when data has been processed by the stack and wants to send
 * it out.
 */
public interface SctpDataSender {
    /**
     *
     * @param data
     * @param offset
     * @param length
     * @return 0 if the packet was successfully sent, -1 otherwise
     */
    int send(byte[] data, int offset, int length);
}
