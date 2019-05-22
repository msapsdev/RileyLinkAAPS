package info.nightscout.androidaps.plugins.pump.omnipod.comm.message.command;

import org.junit.Test;
import info.nightscout.androidaps.plugins.pump.omnipod.comm.message.MessageBlockType;
import static org.junit.Assert.assertArrayEquals;

public class DeactivatePodCommandUnitTests {

    @Test
    public void Constructor_BytesCorrect() throws Exception {
        DeactivatePodCommand deactivatePodCommand = new DeactivatePodCommand(0x10203040);
        byte[] rawData = deactivatePodCommand.getRawData();
        assertArrayEquals(new byte[] {
                MessageBlockType.DeactivatePod.getValue(),
                4, // length
                (byte)0x10, (byte)0x20, (byte)0x30, (byte)0x40 // nonce
        }, rawData);
    }
}
