package info.nightscout.androidaps.plugins.pump.omnipod.comm.message.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AssignAddressCommandUnitTests {

    @Test
    public void StatusResponse_EnumsCorrect() throws Exception {

        AssignAddressCommand assignAddressCommand = new AssignAddressCommand(0x11223344);
        byte[] rawData = assignAddressCommand.getRawData();
        assertEquals(0x11, rawData[2]);
        assertEquals(0x22, rawData[3]);
        assertEquals(0x33, rawData[4]);
        assertEquals(0x44, rawData[5]);
    }

}
