package org.jitsi_modified.sctp4j;

public class SctpJni {

    static {
        System.loadLibrary("jnisctp");
    }
    /**
     * Passes network packet to native SCTP stack counterpart.
     * @param ptr native socket pointer.
     * @param pkt buffer holding network packet data.
     * @param off the position in the buffer where packet data starts.
     * @param len packet data length.
     */
    private static native void on_network_in(
            long ptr,
            byte[] pkt, int off, int len);

    /**
     * Waits for incoming connection.
     * @param ptr native socket pointer.
     */
    static native boolean usrsctp_accept(long ptr);

    /**
     * Closes SCTP socket.
     * @param ptr native socket pointer.
     */
    private static native void usrsctp_close(long ptr);

    /**
     * Connects SCTP socket to remote socket on given SCTP port.
     * @param ptr native socket pointer.
     * @param remotePort remote SCTP port.
     * @return <tt>true</tt> if the socket has been successfully connected.
     */
    static native boolean usrsctp_connect(long ptr, int remotePort);

    /**
     * Disposes of the resources held by native counterpart.
     * @return <tt>true</tt> if stack successfully released resources.
     */
    native private static boolean usrsctp_finish();

    /**
     * Initializes native SCTP counterpart.
     * @param port UDP encapsulation port.
     * @return <tt>true</tt> on success.
     */
    private static native boolean usrsctp_init(int port);

    /**
     * Makes socket passive.
     * @param ptr native socket pointer.
     */
    static native void usrsctp_listen(long ptr);

    /**
     * Sends given <tt>data</tt> on selected SCTP stream using given payload
     * protocol identifier.
     * FIXME add offset and length buffer parameters.
     * @param ptr native socket pointer.
     * @param data the data to send.
     * @param off the position of the data inside the buffer
     * @param len data length.
     * @param ordered should we care about message order ?
     * @param sid SCTP stream identifier
     * @param ppid payload protocol identifier
     * @return sent bytes count or <tt>-1</tt> in case of an error.
     */
    static native int usrsctp_send(
            long ptr,
            byte[] data, int off, int len,
            boolean ordered,
            int sid,
            int ppid);

    /**
     * Creates native SCTP socket and returns pointer to it.
     * @param localPort local SCTP socket port.
     * @return native socket pointer or 0 if operation failed.
     */
    private static native long usrsctp_socket(int localPort);

    /*
    FIXME to be added?
    int usrsctp_shutdown(struct socket *so, int how);
    */
}
