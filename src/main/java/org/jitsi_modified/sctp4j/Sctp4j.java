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

import org.jitsi.utils.logging2.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * This class represents the first layer of the actual API on top of the bare
 * SctpJni layer.  It manages the created sockets and muxes all data received
 * from the usrsctp lib into the appropriate socket instance, so the user can
 * send and receive data for a particular sid through the socket instance.
 *
 * @author Brian Baldino
 * @author Pawel Domas
 */
public class Sctp4j {
    private static boolean initialized = false;

    private static final Logger classLogger = new LoggerImpl(Sctp4j.class.toString());

    /**
     * https://github.com/sctplab/usrsctp/blob/master/Manual.md#usrsctp_init
     */
    public static void init(int port, int sctpDebugMask)
    {
        if (!initialized)
        {
            SctpJni.usrsctp_init(port, sctpDebugMask);
            initialized = true;

            SctpJni.incomingSctpDataHandler = Sctp4j::onSctpIncomingData;
            SctpJni.outgoingSctpDataHandler = Sctp4j::onOutgoingSctpData;
        }
    }

    public static void init(int port)
    {
        init(port, 0);
    }

    /**
     * Closes the SCTP socket addressed by the given native pointer.
     *
     * @param ptr the native socket pointer.
     */
    static void closeSocket(long ptr, long id)
    {
        SctpJni.usrsctp_close(ptr);
        sockets.remove(id);
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

    /**
     * This callback is called by the SCTP stack when it has an incoming packet
     * it has finished processing and wants to pass on.  This is only called for
     * SCTP 'app' packets (not control packets, which are handled entirely by
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
    private static void onSctpIncomingData(
            long socketAddr,
            byte[] data,
            int sid, int ssn, int tsn, long ppid, int context, int flags) {
        SctpSocket socket = sockets.get(socketAddr);
        if (socket != null)
        {
            socket.onSctpIn(data, sid, ssn, tsn, ppid, context, flags);
        }
        else
        {
            classLogger.error("No socket found in onSctpIncomingData");
        }
    }

    /**
     * This callback is called by the SCTP stack when it has a packet it wants
     * to send out to the network.
     * @param socketAddr
     * @param data
     * @param tos
     * @param set_df
     * @return 0 if the packet was successfully sent, -1 otherwise
     */
    private static int onOutgoingSctpData(
            long socketAddr, byte[] data, int tos, int set_df)
    {
        SctpSocket socket = sockets.get(socketAddr);
        if (socket != null)
        {
            return socket.onSctpOut(data, tos, set_df);
        }
        else
        {
            classLogger.error("No socket found in onOutgoingSctpData");
        }
        return -1;
    }

    /**
     * Create an {@link SctpServerSocket} which can be used to listen for an
     * incoming connection
     * @param localSctpPort
     * @return
     */
    public static SctpServerSocket createServerSocket(int localSctpPort, Logger parentLogger)
    {
        long id = nextId.getAndIncrement();
        long ptr = SctpJni.usrsctp_socket(localSctpPort, id);
        if (ptr == 0)
        {
            parentLogger.error("Failed to create server socket");
            return null;
        }
        SctpServerSocket socket = new SctpServerSocket(ptr, id, parentLogger);
        sockets.put(id, socket);

        return socket;
    }

    /**
     * Create an {@link SctpClientSocket} which can be used to connect to an
     * {@link SctpServerSocket}.
     *
     * @param localSctpPort
     * @return
     */
    public static SctpClientSocket createClientSocket(int localSctpPort, Logger parentLogger)
    {
        long id = nextId.getAndIncrement();
        long ptr = SctpJni.usrsctp_socket(localSctpPort, id);
        if (ptr == 0)
        {
            parentLogger.error("Failed to create client socket");
            return null;
        }
        SctpClientSocket socket = new SctpClientSocket(ptr, id, parentLogger);
        sockets.put(id, socket);

        return socket;
    }

    /**
     * Atomic counter to assign each SctpSocket a unique ID.
     */
    private static AtomicLong nextId = new AtomicLong(1);
}
