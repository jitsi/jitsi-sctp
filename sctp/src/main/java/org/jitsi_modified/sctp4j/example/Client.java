package org.jitsi_modified.sctp4j.example;

import org.jitsi_modified.sctp4j.Sctp4j;
import org.jitsi_modified.sctp4j.SctpSocket;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Client {
    public static void main(String[] args) throws UnknownHostException, SocketException, ExecutionException, InterruptedException {
        Sctp4j.init();

        InetAddress localAddr = InetAddress.getByName("127.0.0.1");
        int localPort = 48002;

        InetAddress remoteAddr = InetAddress.getByName("127.0.0.1");
        int remotePort = 48001;

        DatagramSocket socket = new DatagramSocket(localPort, localAddr);

        final SctpSocket client = Sctp4j.createSocket();
        CompletableFuture<Boolean> dataSent = new CompletableFuture<>();
        client.outgoingDataSender = (data, offset, length) -> {
            DatagramPacket packet = new DatagramPacket(data, offset, length, remoteAddr, remotePort);
            try {
                socket.send(packet);
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e.toString());
            }
            return 0;
        };
        new Thread(() -> {
            byte[] buf = new byte[1600];
            DatagramPacket p = new DatagramPacket(buf, 1600);
            while (true) {
                try {
                    socket.receive(p);
                    client.onConnIn(p.getData(), p.getOffset(), p.getLength());
                    if (dataSent.isDone()) {
                        // Quit once we sent data to the server
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving from udp socket: " + e.toString());
                    break;
                }
            }
        }).start();

        client.connect();


        client.eventHandler = new SctpSocket.SctpSocketEventHandler() {
            @Override
            public void onConnected() {
                System.out.println("Client connected");
                String message = "Hello, world";
                client.send(ByteBuffer.wrap(message.getBytes()), true, 0, 1);
                client.close();
                dataSent.complete(true);
            }

            @Override
            public void onDisconnected() {
                System.out.println("Client disconnected");
            }
        };

        dataSent.get();
        System.out.println("Client done");
    }
}
