import org.jitsi_modified.sctp4j.SctpJni;
import org.junit.Test;

public class SctpJniTest {

    @Test
    public void testSctpJni() {
        SctpJni.usrsctp_finish();
    }
}
