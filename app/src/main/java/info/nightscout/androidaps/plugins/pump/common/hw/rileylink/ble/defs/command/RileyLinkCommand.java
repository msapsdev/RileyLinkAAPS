package info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.command;

import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkFirmwareVersion;

public abstract class RileyLinkCommand {

    protected RileyLinkFirmwareVersion version;

    public RileyLinkCommand(RileyLinkFirmwareVersion version) {
        this.version = version;
    }

    public abstract RileyLinkCommandType getCommandType();

    public abstract byte[] getRaw();

    protected byte[] getRawSimple() {
        return getByteArray(getCommandType().code);

    }

    protected byte[] getByteArray(byte... input) {
        return input;
    }


}
