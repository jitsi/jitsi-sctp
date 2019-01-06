package org.jitsi_modified.sctp4j;

/**
 * An SctpServerSocket can be used to listen for an incoming connection and then to send and receive
 * data to/from the other peer.
 */
public class SctpServerSocket extends SctpSocket
{
    private boolean accepted = false;

    public SctpServerSocket(long ptr) {
        super(ptr);
    }

    /**
     * Makes SCTP socket passive.
     * "Marks the socket as a passive socket, that is, as a socket that will be
     * used to accept incoming connection requests using accept"
     */
    public synchronized void listen()
    {
        if (socketValid()) {
            SctpJni.usrsctp_listen(ptr);
        } else {
            System.out.println("Server socket can't listen, socket isn't valid");
        }
    }

    @Override
    protected boolean isReady()
    {
        return socketConnected() && accepted;
    }

    /**
     * Accepts incoming SCTP connection.
     *
     * Usrsctp is currently configured to work in non blocking mode thus this
     * method should be polled in intervals.
     *
     * NOTE: Normally the socket used to accept would be re-used to accept multiple
     * incoming connections, and each successful accept would return a new socket
     * for the new connection.  Instead, the JNI C file, upon successfully accepting
     * a connection, will overwrite an underlying socket pointer it stores to now 'redirect'
     * this java {@link SctpSocket} instance to the newly accepted connection.  So after
     * a successful call to accept, this instance should be used for sending/receiving data
     * on that new connection.
     *
     * @return <tt>true</tt> if we have accepted incoming connection
     *         successfully.
     */
    public synchronized boolean accept()
    {
        if (socketValid()) {
            if (SctpJni.usrsctp_accept(ptr))
            {
                accepted = true;
                return true;
            }
        } else {
            System.out.println("Server can't accept, socket isn't valid");
        }
        return false;
    }
}
