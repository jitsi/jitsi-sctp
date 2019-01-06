package org.jitsi_modified.sctp4j;

public class SctpClientSocket extends SctpSocket
{
    public SctpClientSocket(long ptr) {
        super(ptr);
    }

    @Override
    protected boolean isReady()
    {
        return socketConnected();
    }

    /**
     * Starts a connection on this socket (if it's open).  See {@link Sctp4j#connect(SctpSocket, int)}
     * @return true if the connection has started, false otherwise
     */
    public synchronized boolean connect(int remoteSctpPort) {
        if (socketValid()) {
            return Sctp4j.connect(this, remoteSctpPort);
        }
        return false;
    }
}
