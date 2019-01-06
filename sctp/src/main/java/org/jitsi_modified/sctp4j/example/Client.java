package org.jitsi_modified.sctp4j.example;

import org.jitsi_modified.sctp4j.*;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Client {
    public static void main(String[] args) throws UnknownHostException, SocketException, ExecutionException, InterruptedException {
        Sctp4j.init(5000);

        InetAddress localAddr = InetAddress.getByName("127.0.0.1");
        int localPort = 48002;
        int localSctpPort = 5002;

        InetAddress remoteAddr = InetAddress.getByName("127.0.0.1");
        int remotePort = 48001;
        int remoteSctpPort = 5001;

        DatagramSocket socket = new DatagramSocket(localPort, localAddr);

        final SctpClientSocket client = Sctp4j.createClientSocket(localSctpPort);
        CompletableFuture<String> dataReceived = new CompletableFuture<>();
        client.outgoingDataSender = (data, offset, length) -> {
            DatagramPacket packet = new DatagramPacket(data, offset, length, remoteAddr, remotePort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e.toString());
            }
            return 0;
        };
        client.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            String message = new String(data);
            dataReceived.complete(message);
        };
        new Thread(() -> {
            byte[] buf = new byte[1600];
            DatagramPacket p = new DatagramPacket(buf, 1600);
            while (true) {
                try {
                    socket.receive(p);
                    client.onConnIn(p.getData(), p.getOffset(), p.getLength());
                    if (dataReceived.isDone()) {
                        // Quit once we sent data to the server
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving from udp socket: " + e.toString());
                    break;
                }
            }
        }).start();

        client.connect(remoteSctpPort);

        client.eventHandler = new SctpSocket.SctpSocketEventHandler() {
            @Override
            public void onReady() {
                System.out.println("Client connected, sending message");
                String message = "Marco";
                client.send(ByteBuffer.wrap(message.getBytes()), true, 0, 1);
            }

            @Override
            public void onDisconnected() {
                System.out.println("Client disconnected");
            }
        };

        String serverMessage = dataReceived.get();
        client.close();
        System.out.println("Client received message: '" + serverMessage + "'");
    }
}
