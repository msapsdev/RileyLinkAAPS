package info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.command;

import org.apache.commons.lang3.NotImplementedException;

import java.nio.ByteBuffer;

import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkFirmwareVersion;

public class SetPreamble extends RileyLinkCommand {

    private int preamble;

    public SetPreamble(RileyLinkFirmwareVersion version, int preamble) throws Exception {
        super(version);
        if (!this.version.isSameVersion(RileyLinkFirmwareVersion.Version2AndHigher)) { //this command was not supported before 2.0
            throw new NotImplementedException("Old firmware does not support SetPreamble command");
        }

        if (preamble < 0 || preamble > 0xFFFF) {
            throw new Exception("preamble value is out of range");
        }
        this.preamble = preamble;
    }

    @Override
    public RileyLinkCommandType getCommandType() {
        return RileyLinkCommandType.SetPreamble;
    }

    @Override
    public byte[] getRaw() {
        byte[] bytes = ByteBuffer.allocate(4).putInt(preamble).array();
        return getByteArray(this.getCommandType().code, bytes[2], bytes[3]);
    }
}
