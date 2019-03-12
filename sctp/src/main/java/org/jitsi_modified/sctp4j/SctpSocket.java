/*
 * Copyright @ 2018 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jitsi_modified.sctp4j;

import java.io.*;
import java.nio.*;

/**
 * @author Pawel Domas
 * @author George Politis
 * @author Lyubomir Marinov
 * @author Brian Baldino
 * @author Boris Grozev
 */
public abstract class SctpSocket
{
    public interface SctpSocketEventHandler
    {
        /**
         * Called when this socket is ready for use
         */
        void onReady();
        void onDisconnected();
    }

    /**
     * Pointer to the native socket counterpart
     */
    protected long ptr;

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

    /**
     * The number of current readers of {@link #ptr} which are preventing the
     * writer (i.e. {@link #close()}) from invoking
     * {@link Sctp#closeSocket(long)}.
     */
    private int ptrLockCount = 0;

    /**
     * The indicator which determines whether {@link #close()} has been invoked
     * on this <tt>SctpSocket</tt>. It does NOT indicate whether
     * {@link Sctp#closeSocket(long)} has been invoked with {@link #ptr}.
     */
    private boolean closed = false;

    public SctpSocket(long ptr)
    {
        this.ptr = ptr;
    }

    /**
     * Locks {@link #ptr} for reading and returns its value if this
     * <tt>SctpSocket</tt> has not been closed (yet). Each <tt>lockPtr</tt>
     * method invocation must be balanced with a subsequent <tt>unlockPtr</tt>
     * method invocation.
     *
     * @return <tt>ptr</tt>
     * @throws IOException if this <tt>SctpSocket</tt> has (already) been closed
     */
    protected long lockPtr()
        throws IOException
    {
        long ptr;

        synchronized (this)
        {
            // It may seem that the synchronization with respect to the field
            // closed is inconsistent because there is no synchronization upon
            // writing its value. It is consistent though.
            if (closed)
            {
                throw new IOException("SctpSocket is closed!");
            }
            else
            {
                ptr = this.ptr;
                if (ptr == 0)
                    throw new IOException("SctpSocket is closed!");
                else
                    ++ptrLockCount;
            }
        }
        return ptr;
    }

    /**
     * Unlocks {@link #ptr} for reading. If this <tt>SctpSocket</tt> has been
     * closed while <tt>ptr</tt> was locked for reading and there are no other
     * readers at the time of the method invocation, closes <tt>ptr</tt>. Each
     * <tt>unlockPtr</tt> method invocation must be balanced with a previous
     * <tt>lockPtr</tt> method invocation.
     */
    protected void unlockPtr()
    {
        long ptr;

        synchronized (this)
        {
            int ptrLockCount = this.ptrLockCount - 1;

            if (ptrLockCount < 0)
            {
                throw new RuntimeException(
                        "Unbalanced SctpSocket#unlockPtr() method invocation!");
            }
            else
            {
                this.ptrLockCount = ptrLockCount;
                if (closed && (ptrLockCount == 0))
                {
                    // The actual closing of ptr was deferred until now.
                    ptr = this.ptr;
                    this.ptr = 0;
                }
                else
                {
                    // The actual closing of ptr may not have been requested or
                    // will be deferred.
                    ptr = 0;
                }
            }
        }
        if (ptr != 0)
        {
            Sctp4j.closeSocket(ptr);
        }
    }



    /**
     * Whether or not this connection is ready for use.  The logic to determine
     * this is different for client vs server sockets.
     * @return
     */
    protected abstract boolean isReady();

    protected boolean socketConnected()
    {
        return ptr != 0 && connected;
    }

    /**
     * Fired when usrsctp stack sends notification.
     *
     * @param notification the <tt>SctpNotification</tt> triggered.
     */
    private void onNotification(SctpNotification notification)
    {
        if (notification instanceof SctpNotification.AssociationChange)
        {
            SctpNotification.AssociationChange associationChange
                    = (SctpNotification.AssociationChange)notification;
            System.out.println(
                "Got sctp association state update: " + associationChange.state);
            switch (associationChange.state)
            {
                case SctpNotification.AssociationChange.SCTP_COMM_UP:
                {
                    boolean wasReady = isReady();
                    System.out.println("sctp is now up.  was ready? " + wasReady);
                    connected = true;
                    if (isReady() && !wasReady)
                    {
                        if (eventHandler != null)
                        {
                            System.out.println("sctp invoking onready");
                            eventHandler.onReady();
                        }
                    }
                    break;
                }
                case SctpNotification.AssociationChange.SCTP_COMM_LOST:
                case SctpNotification.AssociationChange.SCTP_SHUTDOWN_COMP:
                case SctpNotification.AssociationChange.SCTP_CANT_STR_ASSOC:
                {
                    connected = false;
                    if (eventHandler != null)
                    {
                        eventHandler.onDisconnected();
                    }
                    break;
                }
            }
        }
    }

    /**
     * Closes this socket. After call to this method this instance MUST NOT be
     * used.
     */
    public void close()
    {
        // The value of the field closed only ever changes from false to true.
        // Additionally, its reading is always synchronized and combined with
        // access to the field ptrLockCount governed by logic which binds the
        // meanings of the two values together. Consequently, the
        // synchronization with respect to closed is considered consistent.
        // Allowing the writing outside the synchronized block expedites the
        // actual closing of ptr.
        closed = true;
        connected = false;

        long ptr;

        synchronized (this)
        {
            if (ptrLockCount == 0)
            {
                // The actual closing of ptr will not be deferred.
                ptr = this.ptr;
                this.ptr = 0;
            }
            else
            {
                // The actual closing of ptr will be deferred.
                ptr = 0;
            }
        }
        if (ptr != 0)
        {
            Sctp4j.closeSocket(ptr);
        }
    }


    /**
     * Call this method to pass network packets received on the link to the
     * SCTP stack.
     *
     * @param packet network packet received.
     * @param offset the position in the packet buffer where actual data starts
     * @param len length of packet data in the buffer.
     */
    public void onConnIn(byte[] packet, int offset, int len)
    {
        if (offset < 0 || len <= 0 || offset + len > packet.length)
        {
            throw new IllegalArgumentException(
                    "o: " + offset + " l: " + len + " packet l: " + packet.length);
        }

        try
        {
            lockPtr();
        }
        catch (IOException ioe)
        {
            System.out.println("Socket isn't open, ignoring incoming data");
            return;
        }

        try
        {
            SctpJni.on_network_in(ptr, packet, offset, len);
        }
        finally
        {
            unlockPtr();
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
    void onSctpIn(
            byte[] data, int sid, int ssn, int tsn, long ppid, int context,
            int flags)
    {
        if ((flags & Sctp4j.MSG_NOTIFICATION) != 0)
        {
            onNotification(SctpNotification.parse(data));
        }
        else
        {
            if (dataCallback != null)
            {
                dataCallback.onSctpPacket(
                        data, sid, ssn, tsn, ppid, context, flags);
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
    public int onSctpOut(byte[] packet, int tos, int set_df)
    {
        int ret = -1;
        try
        {
            lockPtr();
        }
        catch (IOException ioe)
        {
            return ret;
        }

        try
        {
            if (outgoingDataSender != null)
            {
                ret = outgoingDataSender.send(packet, 0, packet.length);
            }
        }
        finally
        {
            unlockPtr();
        }

        return ret;
    }

    /**
     * Send SCTP app data through the stack and out
     * @return the number of bytes sent or -1 on error
     */
    public int send(
            ByteBuffer data, boolean ordered, int sid, int ppid)
    {
        int ret = -1;

        try
        {
            lockPtr();
        }
        catch (IOException ioe)
        {
            return ret;
        }

        try
        {
            if (socketConnected())
            {
                ret = SctpJni.usrsctp_send(
                        ptr,
                        data.array(), data.arrayOffset(), data.limit(),
                        ordered, sid, ppid);
            }
        }
        finally
        {
            unlockPtr();
        }
        return ret;
    }
}
