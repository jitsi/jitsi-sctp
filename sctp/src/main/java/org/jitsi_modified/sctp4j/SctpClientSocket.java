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
 * @author Brian Baldino
 */
public class SctpClientSocket extends SctpSocket
{
    public SctpClientSocket(long ptr)
    {
        super(ptr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isReady()
    {
        return socketConnected();
    }

    /**
     * Starts a connection on this socket (if it's open).
     *
     * @return true if the connection has started, false otherwise
     */
    public boolean connect(int remoteSctpPort)
    {
        boolean ret = false;
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
            ret = SctpJni.usrsctp_connect(ptr, remoteSctpPort);
        }
        finally
        {
            unlockPtr();
        }
        return ret;
    }
}
