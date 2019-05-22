package info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.data;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.RFTools;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.RileyLinkFirmwareVersion;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.command.RileyLinkCommand;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.ble.defs.command.RileyLinkCommandType;
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil;
import info.nightscout.androidaps.plugins.pump.common.utils.CRC;

/**
 * Created by geoff on 5/30/16.
 */
public class RadioResponse {

    private static final Logger LOG = LoggerFactory.getLogger(RadioResponse.class);

    public boolean decodedOK = false;
    public int rssi;
    public int responseNumber;
    public byte[] decodedPayload = new byte[0];
    public byte receivedCRC;
    private RileyLinkCommand command;


    public RadioResponse() {
    }


    public RadioResponse(byte[] rxData) {
        init(rxData);
    }

    public RadioResponse(RileyLinkCommand command, byte[] raw) {
        this.command = command;
        init(raw);
    }


    public boolean isValid() {

        //We should check for all listening commands, but only one is actually used
        if (command != null && command.getCommandType() != RileyLinkCommandType.SendAndListen) {
            return true;
        }


        if (!decodedOK) {
            return false;
        }
        if (decodedPayload != null) {
            if (receivedCRC == CRC.crc8(decodedPayload)) {
                return true;
            }
        }
        return false;
    }


    public void init(byte[] rxData) {
        if (rxData == null) {
            return;
        }
        if (rxData.length < 3) {
            // This does not look like something valid heard from a RileyLink device
            return;
        }
        rssi = rxData[0];
        responseNumber = rxData[1];
        byte[] encodedPayload;
        if (RileyLinkFirmwareVersion.isSameVersion(RileyLinkUtil.getRileyLinkServiceData().versionCC110, RileyLinkFirmwareVersion.Version2)) {
            encodedPayload = ByteUtil.substring(rxData, 3, rxData.length - 3);
        } else {
            encodedPayload = ByteUtil.substring(rxData, 2, rxData.length - 2);

        }
        try {

            // for non-radio commands we just return the raw response
            // well, for non-radio commands we shouldn't even reach this point
            // but getVersion is kind of exception
            if (command != null && //
                    command.getCommandType() != RileyLinkCommandType.SendAndListen) {
                decodedOK = true;
                decodedPayload = encodedPayload;
                return;
            }
            //boolean isEncoded = command==null || command.isEncoded();

            switch (RileyLinkUtil.getEncoding()) {
                case Manchester:
                    //decodedPayload = ByteUtil.substring(encodedPayload, 0, encodedPayload.length - 1);
                    decodedOK = true;
                    decodedPayload = encodedPayload;
                    //receivedCRC = encodedPayload[encodedPayload.length - 1];
                    break;
                case FourByteSixByte:
                    byte[] decodeThis = RFTools.decode4b6b(encodedPayload);
                    decodedOK = true;
                    decodedPayload = ByteUtil.substring(decodeThis, 0, decodeThis.length - 1);
                    receivedCRC = decodeThis[decodeThis.length - 1];
                    byte calculatedCRC = CRC.crc8(decodedPayload);
                    if (receivedCRC != calculatedCRC) {
                        LOG.error(String.format("RadioResponse: CRC mismatch, calculated 0x%02x, received 0x%02x", calculatedCRC, receivedCRC));
                    }
                    break;
                default:
                    throw new NotImplementedException("this {" + RileyLinkUtil.getEncoding().toString() + "} encoding is not supported");
            }
        } catch (NumberFormatException e) {
            decodedOK = false;
            LOG.error("Failed to decode radio data: " + ByteUtil.shortHexString(encodedPayload));
        }
    }


//    public void init(byte[] rxData, boolean isEncoded) {
//        if (rxData == null) {
//            return;
//        }
//        if (rxData.length < 3) {
//            // This does not look like something valid heard from a RileyLink device
//            return;
//        }
//        rssi = rxData[0];
//        responseNumber = rxData[1];
//        byte[] encodedPayload = ByteUtil.substring(rxData, 2, rxData.length - 2);
//        try {
//            byte[] decodeThis = RFTools.decode4b6b(encodedPayload);
//            decodedOK = true;
//            decodedPayload = ByteUtil.substring(decodeThis, 0, decodeThis.length - 1);
//            byte calculatedCRC = CRC.crc8(decodedPayload);
//            receivedCRC = decodeThis[decodeThis.length - 1];
//            if (receivedCRC != calculatedCRC) {
//                LOG.error("RadioResponse: CRC mismatch, calculated 0x%02x, received 0x%02x", calculatedCRC, receivedCRC);
//            }
//        } catch (NumberFormatException e) {
//            decodedOK = false;
//            LOG.error("Failed to decode radio data: " + ByteUtil.shortHexString(encodedPayload));
//        }
//    }


    public byte[] getPayload() {
        return decodedPayload;
    }
}
