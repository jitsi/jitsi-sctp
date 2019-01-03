package org.jitsi_modified.sctp4j;

import java.nio.ByteBuffer;

public class SctpSocket {
    public interface SctpSocketEventHandler {
        void onConnected();
        void onDisconnected();
    }
    /**
     * Pointer to the native socket counterpart
     */
    private long ptr;

    /**
     * Used to send network packets.
     */
    public SctpDataSender outgoingDataSender;

    /**
     * Handler to be notified of socket events (connected, disconnected)
     */
    public SctpSocketEventHandler eventHandler;

    private boolean connected = false;

    /**
     * Callback used to notify about received data that has been processed by the
     * SCTP stack
     */
    public SctpDataCallback dataCallback;

    public SctpSocket(long ptr) {
        this.ptr = ptr;
    }

    private boolean socketOpen() {
        return this.ptr != 0;
    }

    private boolean socketConnected() {
        return socketOpen() && connected;
    }

    /**
     * Fired when usrsctp stack sends notification.
     *
     * @param notification the <tt>SctpNotification</tt> triggered.
     */
    private synchronized void onNotification(SctpNotification notification) {
        System.out.println("Received SCTP notification " + notification);
        if (notification instanceof SctpNotification.AssociationChange) {
            SctpNotification.AssociationChange associationChange = (SctpNotification.AssociationChange)notification;
            switch (associationChange.state) {
                case SctpNotification.AssociationChange.SCTP_COMM_UP: {
                    boolean wasConnected = socketConnected();
                    connected = true;
                    if (socketConnected() && !wasConnected) {
                        if (eventHandler != null) {
                            eventHandler.onConnected();
                        }
                    }
                    break;
                }
                case SctpNotification.AssociationChange.SCTP_COMM_LOST:
                case SctpNotification.AssociationChange.SCTP_SHUTDOWN_COMP:
                case SctpNotification.AssociationChange.SCTP_CANT_STR_ASSOC: {
                    connected = false;
                    if (eventHandler != null) {
                        eventHandler.onDisconnected();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Makes SCTP socket passive.
     * "Marks the socket as a passive socket, that is, as a socket that will be
     * used to accept incoming connection requests using accept"
     */
    public synchronized void listen()
    {
        if (socketOpen()) {
            SctpJni.usrsctp_listen(ptr);
        }
    }

    /**
     * Accepts incoming SCTP connection.
     *
     * Usrsctp is currently configured to work in non blocking mode thus this
     * method should be polled in intervals.
     *
     * @return <tt>true</tt> if we have accepted incoming connection
     *         successfully.
     */
    public synchronized boolean accept() {
        if (socketOpen()) {
            return SctpJni.usrsctp_accept(ptr);
        }
        return false;
    }

    /**
     * Close this socket
     */
    public synchronized void close() {
        SctpJni.usrsctp_close(ptr);
        ptr = 0;
        connected = false;
    }

    /**
     * Call this method to pass network packets received on the link to the SCTP stack
     *
     * @param packet network packet received.
     * @param offset the position in the packet buffer where actual data starts
     * @param len length of packet data in the buffer.
     */
    public synchronized void onConnIn(byte[] packet, int offset, int len)
    {
        if (offset < 0 || len <= 0 || offset + len > packet.length)
        {
            throw new IllegalArgumentException(
                    "o: " + offset + " l: " + len + " packet l: " + packet.length);
        }
        if (socketOpen()) {
            SctpJni.on_network_in(ptr, packet, offset, len);
        }
    }

    /**
     * Method fired by SCTP stack to notify about incoming data.
     *
     * @param data buffer holding received data
     * @param sid stream id
     * @param ssn
     * @param tsn
     * @param ppid payload protocol identifier
     * @param context
     * @param flags
     */
    synchronized void onSctpIn(
            byte[] data, int sid, int ssn, int tsn, long ppid, int context,
            int flags)
    {
        if (socketOpen()) {
            if ((flags & Sctp4j.MSG_NOTIFICATION) != 0) {
                onNotification(SctpNotification.parse(data));
            } else {
                if (dataCallback != null) {
                    dataCallback.onSctpPacket(data, sid, ssn, tsn, ppid, context, flags);
                }
            }
        }
    }

    /**
     * Callback triggered by SCTP stack whenever it wants to send some network
     * packet.
     *
     * @param packet network packet buffer.
     * @param tos type of service???
     * @param set_df use IP don't fragment option
     * @return 0 if the packet was successfully sent or -1 otherwise.
     */
    public synchronized int onSctpOut(byte[] packet, int tos, int set_df)
    {
        if (socketOpen()) {
            if (outgoingDataSender != null) {
                return outgoingDataSender.send(packet, 0, packet.length);
            }
        }
        return -1;
    }

    /**
     * Starts a connection on this socket (if it's open).  See {@link Sctp4j#connect(SctpSocket)}
     * @return true if the connection has started, false otherwise
     */
    public synchronized boolean connect() {
        if (socketOpen()) {
            return Sctp4j.connect(this);
        }
        return false;
    }

    /**
     * Send SCTP app data through the stack and out
     * @return the number of bytes sent or -1 on error
     */
    public synchronized int send(ByteBuffer data, boolean ordered, int sid, int ppid) {
        if (socketOpen()) {
            return Sctp4j.send(this, data, ordered, sid, ppid);
        }
        return -1;
    }
}
