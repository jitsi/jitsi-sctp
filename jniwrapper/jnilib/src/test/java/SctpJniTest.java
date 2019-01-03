import org.jitsi_modified.sctp4j.SctpJni;
import org.junit.Test;

public class SctpJniTest {
    @Test
    public void testSctpJni() {
        // Basic test to make sure the lib loads correctly
        SctpJni.usrsctp_finish();
    }
}
