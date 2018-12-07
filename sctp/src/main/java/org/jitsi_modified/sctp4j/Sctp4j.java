package org.jitsi_modified.sctp4j;

public class Sctp4j {
    private static boolean initialized = false;
    public static synchronized void init() {
        if (!initialized) {
            SctpJni.usrsctp_init(0);
            initialized = true;
        }
    }
}
