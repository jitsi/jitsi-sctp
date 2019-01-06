package org.jitsi_modified.sctp4j.example;

import org.jitsi_modified.sctp4j.*;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Server {
    public static void main(String[] args) throws UnknownHostException, SocketException, ExecutionException, InterruptedException {
        Sctp4j.init(5000);

        InetAddress remoteAddr = InetAddress.getByName("127.0.0.1");
        int remotePort = 48002;

        InetAddress localAddr = InetAddress.getByName("127.0.0.1");
        int localPort = 48001;
        int localSctpPort = 5001;

        DatagramSocket socket = new DatagramSocket(localPort, localAddr);

        final SctpServerSocket server = Sctp4j.createServerSocket(localSctpPort);
        server.outgoingDataSender = (data, offset, length) -> {
            DatagramPacket packet = new DatagramPacket(data, offset, length, remoteAddr, remotePort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e.toString());
            }
            return 0;
        };
        CompletableFuture<String> serverReceivedData = new CompletableFuture<>();
        CompletableFuture<Boolean> connectionReady = new CompletableFuture<>();

        server.eventHandler = new SctpSocket.SctpSocketEventHandler()
        {
            @Override
            public void onReady()
            {
                System.out.println("Server socket is ready for use");
                connectionReady.complete(true);
            }

            @Override
            public void onDisconnected()
            {
                System.out.println("Server socket disconnected");
            }
        };

        server.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            String message = new String(data);
            System.out.println("Server received data: " + message);
            serverReceivedData.complete(message);
            connectionReady.thenRun(() -> {
                System.out.println("Server accepted connection and received data, sending response");
                String response = "Polo";
                server.send(ByteBuffer.wrap(response.getBytes()), true, sid, (int)ppid);
            });
        };
        new Thread(() -> {
            byte[] buf = new byte[1600];
            DatagramPacket p = new DatagramPacket(buf, 1600);
            while (true) {
                try {
                    socket.receive(p);
                    server.onConnIn(p.getData(), p.getOffset(), p.getLength());
                    if (serverReceivedData.isDone()) {
                        // Quit once we've received data from the client
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving from udp socket: " + e.toString());
                    break;
                }
            }
        }).start();

        server.listen();
        while (!server.accept()) {
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("Server accepted connection");
        connectionReady.complete(true);

        String clientMessage = serverReceivedData.get();

        server.close();
        System.out.println("Server received message: '" + clientMessage + "'");
    }
}
