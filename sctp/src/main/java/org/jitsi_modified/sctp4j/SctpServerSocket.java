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

/**
 * An SctpServerSocket can be used to listen for an incoming connection and then
 * to send and receive data to/from the other peer.
 *
 * @author Brian Baldino
 */
public class SctpServerSocket extends SctpSocket
{
    private boolean accepted = false;

    public SctpServerSocket(long ptr)
    {
        super(ptr);
    }

    /**
     * Makes SCTP socket passive.
     * "Marks the socket as a passive socket, that is, as a socket that will be
     * used to accept incoming connection requests using accept"
     */
    public void listen()
    {
        try
        {
            lockPtr();
        }
        catch (IOException ioe)
        {
            System.out.println("Server socket can't listen: " + ioe.getMessage());
            return;
        }

        try
        {
            SctpJni.usrsctp_listen(ptr);
        }
        finally
        {
            unlockPtr();
        }
    }

    /**
     * {@inheritDoc}
     */
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
     * NOTE: Normally the socket used to accept would be re-used to accept
     * multiple incoming connections, and each successful accept would return a
     * new socket for the new connection.  Instead, the JNI C file, upon
     * successfully accepting a connection, will overwrite an underlying socket
     * pointer it stores to now 'redirect' this java {@link SctpSocket} instance
     * to the newly accepted connection.  So after a successful call to accept,
     * this instance should be used for sending/receiving data on that new
     * connection.
     *
     * @return <tt>true</tt> if we have accepted incoming connection
     *         successfully.
     */
    public boolean accept()
    {
        boolean ret = false;
        try
        {
            lockPtr();
        }
        catch (IOException ioe)
        {
            System.out.println("Server can't accept: " + ioe.getMessage());
            return ret;
        }

        try
        {
            if (SctpJni.usrsctp_accept(ptr))
            {
                accepted = true;
                // It's possible we can get the SCTP notification SCTP_COMM_UP
                // before we've accepted, since accept is called repeatedly at
                // some interval, so we need to check if we're ready here
                //TODO: doesn't feel great to invoke this handler in the context
                // of the accept call, should we post it elsewhere?
                if (isReady())
                {
                    eventHandler.onReady();
                }
                ret = true;
            }
        }
        finally
        {
            unlockPtr();
        }

        return ret;
    }
}
