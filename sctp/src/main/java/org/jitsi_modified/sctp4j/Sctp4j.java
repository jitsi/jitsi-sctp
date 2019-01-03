package org.jitsi_modified.sctp4j;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents the first layer of the actual API on top of the bare SctpJni layer.  It
 * manages the created sockets and muxes all data received from the usrsctp lib into the appropriate
 * socket instance, so the user can send and receive data for a particular sid through the socket
 * instance.
 */
public class Sctp4j {
    private static boolean initialized = false;
    /**
     * We don't actually bind directly to a port (data is sent and received via a socket
     * elsewhere) but we do need to pass something reasonable into usrsctp or else it
     * isn't happy
     */
    private static int DUMMY_PORT = 5000;
    public static void init() {
        if (!initialized) {
            SctpJni.usrsctp_init(DUMMY_PORT);
            initialized = true;

            SctpJni.incomingSctpDataHandler = Sctp4j::onSctpIncomingData;
            SctpJni.outgoingSctpDataHandler = Sctp4j::onOutgoingSctpData;
        }
    }

    /**
     * SCTP notification
     */
    public static final int MSG_NOTIFICATION = 0x2000;

    /**
     * List of instantiated SctpSockets mapped by native pointer.
     */
    private static final Map<Long, SctpSocket2> sockets
            = new ConcurrentHashMap<>();

    //TODO: if we do end up sticking with this (where we index by ptr
    // when handling calls from the stack, but by socket when handling
    // calls from java) maybe we want to maintain a bi-map or something
    private static long getPtrFromSocket(SctpSocket2 socket) {
        for (Map.Entry<Long, SctpSocket2> entry : sockets.entrySet()) {
            if (entry.getValue() == socket) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private static void onSctpIncomingData(long socketAddr, byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags) {
        SctpSocket2 socket = sockets.get(socketAddr);
        if (socket != null) {
            socket.onSctpIn(data, sid, ssn, tsn, ppid, context, flags);
        }
    }

    /**
     *
     * @param socketAddr
     * @param data
     * @param tos
     * @param set_df
     * @return 0 if the packet was successfully sent, -1 otherwise
     */
    private static int onOutgoingSctpData(long socketAddr, byte[] data, int tos, int set_df) {
        SctpSocket2 socket = sockets.get(socketAddr);
        if (socket != null) {
            return socket.onSctpOut(data, tos, set_df);
        }
        System.out.println("Sctp4j couldn't find socket to send data, returning -1");
        return -1;
    }
    public static SctpSocket2 createSocket() {
        long ptr = SctpJni.usrsctp_socket(DUMMY_PORT);
        if (ptr == 0) {
            return null;
        }
        SctpSocket2 socket = new SctpSocket2(ptr);
        sockets.put(ptr, socket);

        return socket;
    }

    /**
     * Starts a connection using the given socket.
     * TODO: pretty sure this doesn't block until it fully connects
     * @param socket
     * @return
     */
    public static boolean connect(SctpSocket2 socket) {
        long ptr = getPtrFromSocket(socket);
        if (ptr != 0) {
            return SctpJni.usrsctp_connect(ptr, DUMMY_PORT);
        }
        return false;
    }

    public static int send(SctpSocket2 socket, ByteBuffer data, boolean ordered, int sid, int ppid) {
        long ptr = getPtrFromSocket(socket);
        if (ptr != 0) {
            SctpJni.usrsctp_send(ptr, data.array(), data.arrayOffset(), data.limit(), ordered, sid, ppid);
        }
        return -1;
    }
}
