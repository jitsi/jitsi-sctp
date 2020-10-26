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

import cz.adamh.utils.NativeUtils;

public class SctpJni {
    static {
        // Load the native library
        loadJniLibrary();
    }

    private static void loadJniLibrary() {
        try {
            try {
                // Prefer user-provided library over bundled into Jar
                System.loadLibrary("jnisctp");
                return;
            }
            catch (UnsatisfiedLinkError e) {
                System.out.println("SCTP JNI load: unable to load jnsctcp by System.loadLibrary(String libname) " +
                        "will extract library from JAR");
            }

            final String os = canonicalOsName();
            if (os == null) {
                throw new Exception("Unsupported OS: " + System.getProperty("os.name"));
            }
            final String arch = canonicalCpuArch();
            if (arch == null) {
                throw new Exception("Unsupported CPU architecture: " + System.getProperty("os.arch"));
            }

            if (os.contains("osx")) {
                System.out.println("SCTP JNI load: Mac OS detected");
                NativeUtils.loadLibraryFromJar(String.format("/lib/%s-%s/libjnisctp.jnilib", os, arch));
            } else if (os.contains("linux")) {
                System.out.println("SCTP JNI load: Linux OS detected");
                NativeUtils.loadLibraryFromJar(String.format("/lib/%s-%s/libjnisctp.so", os, arch));
            } else if (os.contains("windows")) {
                System.out.println("SCTP JNI load: Windows OS detected");
                NativeUtils.loadLibraryFromJar(String.format("/lib/%s-%s/jnisctp.dll", os, arch));
            } else {
                throw new Exception("Unsupported OS: " + os);
            }
            System.out.println("SCTP lib loaded");
        } catch (Exception e) {
            System.out.println("Error loading native library: " + e);
        }
    }

    private static String canonicalOsName() {

        final String os = System
            .getProperty("os.name")
            .toLowerCase()
            .trim()
            .replaceAll("[^0-9a-z]", "");

        if (os.contains("mac")) {
            return "osx";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        if (os.contains("freebsd")) {
            return "freebsd";
        }
        if (os.contains("windows")) {
            return "windows";
        }
        // not yet supported
        return null;
    }

    private static String canonicalCpuArch() {
        // Checks are borrowed from:
        // https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java#L178
        final String arch = System
            .getProperty("os.arch")
            .toLowerCase()
            .trim()
            .replaceAll("[^0-9a-z]", "");

        if (arch.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
            return "x86_64";
        }
        if (arch.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
            return "x86_32";
        }
        if (arch.matches("^(arm|arm32)$")) {
            return "arm_32";
        }
        if ("aarch64".equals(arch)) {
            return "aarch_64";
        }
        // not yet supported
        return null;
    }

    public static IncomingSctpDataHandler incomingSctpDataHandler = null;
    public static OutgoingSctpDataHandler outgoingSctpDataHandler = null;

    /**
     * Passes network packet to native SCTP stack counterpart.
     * @param ptr native socket pointer.
     * @param pkt buffer holding network packet data.
     * @param off the position in the buffer where packet data starts.
     * @param len packet data length.
     */
    public static native void on_network_in(
            long ptr,
            byte[] pkt, int off, int len);

    /**
     * Waits for incoming connection.
     * @param ptr native socket pointer.
     */
    public static native boolean usrsctp_accept(long ptr);

    /**
     * Closes SCTP socket.
     * @param ptr native socket pointer.
     */
    public static native void usrsctp_close(long ptr);

    /**
     * Connects SCTP socket to remote socket on given SCTP port.
     * @param ptr native socket pointer.
     * @param remotePort remote SCTP port.
     * @return <tt>true</tt> if the socket has been successfully connected.
     */
    public static native boolean usrsctp_connect(long ptr, int remotePort);

    /**
     * Disposes of the resources held by native counterpart.
     * @return <tt>true</tt> if stack successfully released resources.
     */
    public static native boolean usrsctp_finish();

    /**
     * Initializes native SCTP counterpart.
     * @param port UDP encapsulation port.
     * @return <tt>true</tt> on success.
     */
    public static native boolean usrsctp_init(int port);

    /**
     * Makes socket passive.
     * @param ptr native socket pointer.
     */
    public static native void usrsctp_listen(long ptr);

    /**
     * Sends given <tt>data</tt> on selected SCTP stream using given payload
     * protocol identifier.
     * FIXME add offset and length buffer parameters.
     * @param ptr native socket pointer.
     * @param data the data to send.
     * @param off the position of the data inside the buffer
     * @param len data length.
     * @param ordered should we care about message order ?
     * @param sid SCTP stream identifier
     * @param ppid payload protocol identifier
     * @return sent bytes count or <tt>-1</tt> in case of an error.
     */
    public static native int usrsctp_send(
            long ptr,
            byte[] data, int off, int len,
            boolean ordered,
            int sid,
            int ppid);

    /**
     * Creates native SCTP socket and returns pointer to it.
     * @param localPort local SCTP socket port.
     * @return native socket pointer or 0 if operation failed.
     */
    public static native long usrsctp_socket(int localPort, long index);

    /*
    FIXME to be added?
    int usrsctp_shutdown(struct socket *so, int how);
    */

    /**
     * Method fired by native counterpart to notify about incoming data.  We just invoke the incomingSctpDataHandler
     * (if it is not null) with the received data.
     *
     * @param socketAddr native socket pointer
     * @param data buffer holding received data
     * @param sid stream id
     * @param ssn
     * @param tsn
     * @param ppid payload protocol identifier
     * @param context
     * @param flags
     */
    public static void onSctpInboundPacket(
            long socketAddr, byte[] data, int sid, int ssn, int tsn, long ppid,
            int context, int flags)
    {
        if (incomingSctpDataHandler != null) {
            incomingSctpDataHandler.apply(socketAddr, data, sid, ssn, tsn, ppid, context, flags);
        }
    }

    /**
     * Method fired by native counterpart when SCTP stack wants to send
     * network packet.  Invokes the outgoingSctpDataHandler (if not null) with the data.
     * @param socketAddr native socket pointer
     * @param data buffer holding packet data
     * @param tos type of service???
     * @param set_df use IP don't fragment option
     * @return 0 if the packet has been successfully sent or -1 otherwise.
     */
    public static int onSctpOutboundPacket(
            long socketAddr, byte[] data, int tos, int set_df)
    {
        if (outgoingSctpDataHandler != null) {
            outgoingSctpDataHandler.apply(socketAddr, data, tos, set_df);
            return 0;
        }
        // FIXME handle tos and set_df
        return -1;
    }
}
