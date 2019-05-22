package info.nightscout.androidaps.plugins.pump.omnipod.comm.message.command;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import info.nightscout.androidaps.plugins.pump.omnipod.comm.message.MessageBlockType;
import info.nightscout.androidaps.plugins.pump.omnipod.defs.AlertType;

import static org.junit.Assert.assertArrayEquals;

public class AcknowledgeAlertsCommandUnitTests {

    @Test
    public void Constructor_BytesCorrect() throws Exception {

        List<AlertType> alertTypes = Arrays.asList(AlertType.AutoOff, AlertType.SuspendInProgress);
        AcknowledgeAlertsCommand acknowledgeAlertsCommand = new AcknowledgeAlertsCommand(0x10203040, alertTypes);
        byte[] rawData = acknowledgeAlertsCommand.getRawData();
        assertArrayEquals(new byte[] {
                MessageBlockType.AcknowledgeAlerts.getValue(),
                5, // length
                (byte)0x10, (byte)0x20, (byte)0x30, (byte)0x40, // nonce
                (byte)0x21 // alerts (bits 5 and 0)
        }, rawData);
    }

    @Test
    public void ConstructorSingleAlert_BytesCorrect() throws Exception {
        AcknowledgeAlertsCommand acknowledgeAlertsCommand = new AcknowledgeAlertsCommand(0x10203040, AlertType.SuspendInProgress);
        byte[] rawData = acknowledgeAlertsCommand.getRawData();
        assertArrayEquals(new byte[] {
                MessageBlockType.AcknowledgeAlerts.getValue(),
                5, // length
                (byte)0x10, (byte)0x20, (byte)0x30, (byte)0x40, // nonce
                (byte)0x20 // alerts (bit 5)
        }, rawData);
    }
}
