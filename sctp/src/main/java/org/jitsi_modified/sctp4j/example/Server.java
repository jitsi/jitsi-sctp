package org.jitsi_modified.sctp4j.example;

import org.jitsi_modified.sctp4j.Sctp4j;
import org.jitsi_modified.sctp4j.SctpSocket;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Server {
    public static void main(String[] args) throws UnknownHostException, SocketException, ExecutionException, InterruptedException {
        Sctp4j.init();

        InetAddress remoteAddr = InetAddress.getByName("127.0.0.1");
        int remotePort = 48002;

        InetAddress localAddr = InetAddress.getByName("127.0.0.1");
        int localPort = 48001;

        DatagramSocket socket = new DatagramSocket(localPort, localAddr);

        final SctpSocket server = Sctp4j.createSocket();
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
        server.dataCallback = (data, sid, ssn, tsn, ppid, context, flags) -> {
            String message = new String(data);
            serverReceivedData.complete(message);
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
        String serverMessage = serverReceivedData.get();
        System.out.println("Server received message: '" + serverMessage + "'");

    }
}
