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
    private static final Map<Long, SctpSocket> sockets
            = new ConcurrentHashMap<>();

    //TODO: if we do end up sticking with this (where we index by ptr
    // when handling calls from the stack, but by socket when handling
    // calls from java) maybe we want to maintain a bi-map or something
    private static long getPtrFromSocket(SctpSocket socket) {
        for (Map.Entry<Long, SctpSocket> entry : sockets.entrySet()) {
            if (entry.getValue() == socket) {
                return entry.getKey();
            }
        }
        return 0;
    }

    /**
     * This callback is called by the SCTP stack when it has an incoming packet it has finished processing and wants
     * to pass on.  This is only called for SCTP 'app' packets (not control packets, which are handled entirely by
     * the stack itself)
     *
     * @param socketAddr
     * @param data
     * @param sid
     * @param ssn
     * @param tsn
     * @param ppid
     * @param context
     * @param flags
     */
    private static void onSctpIncomingData(long socketAddr, byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags) {
        SctpSocket socket = sockets.get(socketAddr);
        if (socket != null) {
            socket.onSctpIn(data, sid, ssn, tsn, ppid, context, flags);
        }
    }

    /**
     * This callback is called by the SCTP stack when it has a packet it wants to send out to the network
     * @param socketAddr
     * @param data
     * @param tos
     * @param set_df
     * @return 0 if the packet was successfully sent, -1 otherwise
     */
    private static int onOutgoingSctpData(long socketAddr, byte[] data, int tos, int set_df) {
        SctpSocket socket = sockets.get(socketAddr);
        if (socket != null) {
            return socket.onSctpOut(data, tos, set_df);
        }
        return -1;
    }

    /**
     * Create an SCTP socket
     * @return the created SCTP socket
     */
    public static SctpSocket createSocket() {
        long ptr = SctpJni.usrsctp_socket(DUMMY_PORT);
        if (ptr == 0) {
            return null;
        }
        SctpSocket socket = new SctpSocket(ptr);
        sockets.put(ptr, socket);

        return socket;
    }

    /**
     * Starts a connection using the given socket.
     * @param socket the socket to use for the connection
     * @return false if there was an error, true if we started the connection (NOTE: this does not
     * block until the socket is actually connected)
     */
    public static boolean connect(SctpSocket socket) {
        long ptr = getPtrFromSocket(socket);
        if (ptr != 0) {
            return SctpJni.usrsctp_connect(ptr, DUMMY_PORT);
        }
        return false;
    }

    /**
     *
     * @param socket
     * @param data
     * @param ordered
     * @param sid
     * @param ppid
     * @return the number of sent bytes or -1 on error
     */
    public static int send(SctpSocket socket, ByteBuffer data, boolean ordered, int sid, int ppid) {
        long ptr = getPtrFromSocket(socket);
        if (ptr != 0) {
            return SctpJni.usrsctp_send(ptr, data.array(), data.arrayOffset(), data.limit(), ordered, sid, ppid);
        }
        return -1;
    }
}
