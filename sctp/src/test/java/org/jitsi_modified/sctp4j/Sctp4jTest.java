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

import org.jitsi_modified.sctp4j.util.SctpParser;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Sctp4jTest {
    @Test
    public void testInit() {
        // Very basic test to verify that everything has been linked up correctly
        Sctp4j.init(5000);
    }

    /**
     * Spin up a client and server and connect
     */
    @Test
    public void basicLoop() throws InterruptedException, TimeoutException, ExecutionException {
        Sctp4j.init(5000);

        final SctpServerSocket server = Sctp4j.createServerSocket(4242);
        final SctpClientSocket client = Sctp4j.createClientSocket(4243);

        server.outgoingDataSender = (data, offset, length) -> {
            new Thread(() -> {
                System.out.println("Server sending type " + (data[12] & 0xFF) + " to client-->");
                System.out.println(SctpParser.bytesToHex(data, offset, length));
                SctpParser.debugSctpPacket(data, "client outgoing");
                System.out.println();
                client.onConnIn(data, offset, length);
            }).start();
            return 0;
        };
        client.outgoingDataSender = (data, offset, length) -> {
            new Thread(() -> {
                System.out.println("\t\t\t\t<---Client sending type " + (data[12] & 0xFF) + " to server");
                System.out.println(SctpParser.bytesToHex(data, offset, length));
                SctpParser.debugSctpPacket(data, "client outgoing");
                System.out.println();
                server.onConnIn(data, offset, length);
            }).start();
            return 0;
        };

        CompletableFuture<String> serverReceivedData = new CompletableFuture<>();
        server.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            System.out.println("Server received app data for sid " + sid + "<---");
            System.out.println(SctpParser.bytesToHex(data, 0, data.length));
            SctpParser.debugSctpPacket(data, "server");
            System.out.println();
            String message = new String(data);
            serverReceivedData.complete(message);
        };
        client.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            System.out.println("\t\t\t\t--->Client received app data for sid " + sid);
            System.out.println(SctpParser.bytesToHex(data, 0, data.length));
            SctpParser.debugSctpPacket(data, "server");
            System.out.println();
        };

        client.eventHandler = new SctpSocket.SctpSocketEventHandler() {
            @Override
            public void onReady() {
                System.out.println("Client connected, sending data");
                String message = "Hello, world";
                client.send(ByteBuffer.wrap(message.getBytes()), true, 0, 1);
            }

            @Override
            public void onDisconnected() {
                System.out.println("Client disconnected");
            }
        };

        server.listen();

        new Thread(() -> {
            System.out.println("Client connecting");
            if (!client.connect(4242)) {
                System.out.println("Client failed to connect");
            }
        }, "Client thread").start();

        String serverMessage = serverReceivedData.get(5, TimeUnit.SECONDS);
        System.out.println(serverMessage + " should equal Hello, world");
    }
}