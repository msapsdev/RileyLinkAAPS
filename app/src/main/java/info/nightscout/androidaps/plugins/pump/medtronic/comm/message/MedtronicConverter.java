package info.nightscout.androidaps.plugins.pump.medtronic.comm.message;

import com.gxwtech.roundtrip2.util.StringUtil;

import org.joda.time.IllegalFieldValueException;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil;
import info.nightscout.androidaps.plugins.pump.common.utils.ByteUtil;
import info.nightscout.androidaps.plugins.pump.common.utils.HexDump;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.data.BasalProfile;
import info.nightscout.androidaps.plugins.pump.medtronic.comm.data.TempBasalPair;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.BatteryStatusDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.data.dto.PumpSettingDTO;
import info.nightscout.androidaps.plugins.pump.medtronic.defs.MedtronicCommandType;
import info.nightscout.androidaps.plugins.pump.medtronic.defs.MedtronicDeviceType;
import info.nightscout.androidaps.plugins.pump.medtronic.defs.PumpConfigurationGroup;
import info.nightscout.androidaps.plugins.pump.medtronic.util.MedtronicUtil;

/**
 * Created by andy on 5/9/18.
 */

public class MedtronicConverter {

    private static final Logger LOG = LoggerFactory.getLogger(MedtronicConverter.class);

    MedtronicDeviceType pumpModel;


    public Object convertResponse(MedtronicCommandType commandType, byte[] rawContent) {

        LOG.debug("Raw response before convert: " + HexDump.toHexStringDisplayable(rawContent));

        this.pumpModel = RileyLinkUtil.getMedtronicPumpModel();

        switch (commandType) {

            case PumpModel: {
                return MedtronicDeviceType.getByDescription(StringUtil.fromBytes(ByteUtil.substring(rawContent, 1, 3)));
            }

            case RealTimeClock: {
                return decodeTime(rawContent);
            }

            case GetRemainingInsulin: {
                return decodeRemainingInsulin(rawContent);
            }

            case GetBatteryStatus: {
                return decodeBatteryStatus(rawContent);
            }

            case GetBasalProfileSTD: {
                return new BasalProfile(rawContent);
            }

            case ReadTemporaryBasal: {
                return new TempBasalPair(rawContent);
            }

            case Settings_512: {
                return decodeSettings512(rawContent);
            }

            case Settings: {
                return decodeSettings(rawContent);
            }

            case SetBolus: {
                return rawContent;
            }


            default: {
                throw new RuntimeException("Unsupported command Type: " + commandType);
            }

        }


    }


    protected BasalProfile decodeProfile2(byte[] rep) {
        //byte rep[] = minimedReply.getRawData();

        // String profile = getProfileName(minimedReply);

        BasalProfile basalProfile = new BasalProfile();

        // 0x12 0x00 0x00 0x16 0x00 0x11 0x00

        if ((rep.length >= 3) && (rep[2] == 0x3F)) {
            //String i18value = i18nControl.getMessage("NOT_SET");
            //writeSetting(key, i18value, i18value, PumpConfigurationGroup.Basal);
            return null;
        }

        int time_x;
        double vald;

        for (int i = 0; i < rep.length; i += 3) {


            vald = MedtronicUtil.decodeBasalInsulin(rep[i + 1], rep[i]);

            time_x = rep[i + 2];

            LocalTime atd = MedtronicUtil.getTimeFrom30MinInterval(time_x);

            if ((i != 0) && (time_x == 0)) {
                break;
            }

            //String value = i18nControl.getMessage("CFG_BASE_FROM") + "=" + atd.getTimeString() + ", "
            //        + i18nControl.getMessage("CFG_BASE_AMOUNT") + "=" + vald;

            //writeSetting(key, value, value, PumpConfigurationGroup.Basal);


        }

        return basalProfile;
    }


    private BatteryStatusDTO decodeBatteryStatus(byte[] rawData) {
        // 00 7C 00 00

        BatteryStatusDTO batteryStatus = new BatteryStatusDTO();

        int status = rawData[0];

        if (status == 0) {
            batteryStatus.batteryStatusType = BatteryStatusDTO.BatteryStatusType.Normal;
        } else if (status == 1) {
            batteryStatus.batteryStatusType = BatteryStatusDTO.BatteryStatusType.Low;
        } else if (status == 2) {
            batteryStatus.batteryStatusType = BatteryStatusDTO.BatteryStatusType.Unknown;
        }

        if (rawData.length > 1) {

            // if response in 3 bytes then we add additional information
            //double d = MedtronicUtil.makeUnsignedShort(rawData[2], rawData[1]) / 100.0d;

            double d = ByteUtil.toInt(rawData[1], rawData[2]) / 100.0d;

            batteryStatus.voltage = d;

            //            double perc = (d - BatteryType.Alkaline.lowVoltage) / (BatteryType.Alkaline.highVoltage - BatteryType.Alkaline.lowVoltage);
            //
            //            LOG.warn("Percent status: " + perc);
            //            LOG.warn("Unknown status: " + rawData[0]);
            //            LOG.warn("Full result: " + d);
            //
            //            int percent = (int) (perc * 100.0d);

            //return percent;
        }

        return batteryStatus;
    }


    protected Float decodeRemainingInsulin(byte[] rawData) {
        //float value = MedtronicUtil.makeUnsignedShort(rawData[0], rawData[1]) / 10.0f;

        float value = ByteUtil.toInt(rawData[0], rawData[1]) / 10.0f;

        System.out.println("Remaining insulin: " + value);
        return value;
    }


    private LocalDateTime decodeTime(byte[] rawContent) {

        int hours = ByteUtil.asUINT8(rawContent[0]);
        int minutes = ByteUtil.asUINT8(rawContent[1]);
        int seconds = ByteUtil.asUINT8(rawContent[2]);
        int year = (ByteUtil.asUINT8(rawContent[4]) & 0x3f) + 1984;
        int month = ByteUtil.asUINT8(rawContent[5]);
        int day = ByteUtil.asUINT8(rawContent[6]);
        try {
            LocalDateTime pumpTime = new LocalDateTime(year, month, day, hours, minutes, seconds);
            return pumpTime;
        } catch (IllegalFieldValueException e) {
            LOG.error("decodeTime: Failed to parse pump time value: year=%d, month=%d, hours=%d, minutes=%d, seconds=%d", year, month, day, hours, minutes, seconds);
            return null;
        }

    }


    public List<PumpSettingDTO> decodeSettings512(byte[] rd) {

        List<PumpSettingDTO> outList = new ArrayList<>();

        outList.add(new PumpSettingDTO("PCFG_AUTOOFF_TIMEOUT", "" + rd[0], PumpConfigurationGroup.General));


        if (rd[1] == 4) {
            outList.add(new PumpSettingDTO("PCFG_ALARM_MODE", "Silent", PumpConfigurationGroup.Sound));
        } else {
            outList.add(new PumpSettingDTO("PCFG_ALARM_MODE", "Normal", PumpConfigurationGroup.Sound));
            outList.add(new PumpSettingDTO("PCFG_ALARM_BEEP_VOLUME", "" + rd[1], PumpConfigurationGroup.Sound));
        }

        outList.add(new PumpSettingDTO("PCFG_AUDIO_BOLUS_ENABLED", parseResultEnable(rd[2]), PumpConfigurationGroup.Bolus));

        if (rd[2] == 1) {
            outList.add(new PumpSettingDTO("PCFG_AUDIO_BOLUS_STEP_SIZE", "" + decodeBolusInsulin(ByteUtil.asUINT8(rd[3])), PumpConfigurationGroup.Bolus));
        }

        outList.add(new PumpSettingDTO("PCFG_VARIABLE_BOLUS_ENABLED", parseResultEnable(rd[4]), PumpConfigurationGroup.Bolus));
        outList.add(new PumpSettingDTO("PCFG_MAX_BOLUS", "" + decodeMaxBolus(rd), PumpConfigurationGroup.Bolus));
        outList.add(new PumpSettingDTO("PCFG_MAX_BASAL", "" + decodeBasalInsulin(ByteUtil.makeUnsignedShort(rd[getSettingIndexMaxBasal()], rd[getSettingIndexMaxBasal() + 1])), PumpConfigurationGroup.Basal));
        outList.add(new PumpSettingDTO("CFG_BASE_CLOCK_MODE", rd[getSettingIndexTimeDisplayFormat()] == 0 ? "12h" : "24h", PumpConfigurationGroup.General));
        outList.add(new PumpSettingDTO("PCFG_INSULIN_CONCENTRATION", "" + (rd[9] != 0 ? 50 : 100), PumpConfigurationGroup.Insulin));
        outList.add(new PumpSettingDTO("PCFG_BASAL_PROFILES_ENABLED", parseResultEnable(rd[10]), PumpConfigurationGroup.Basal));

        if (rd[10] == 1) {
            String patt;
            switch (rd[11]) {
                case 0:
                    patt = "STD";
                    break;

                case 1:
                    patt = "A";
                    break;

                case 2:
                    patt = "B";
                    break;

                default:
                    patt = "???";
                    break;
            }

            outList.add(new PumpSettingDTO("PCFG_ACTIVE_BASAL_PROFILE", patt, PumpConfigurationGroup.Basal));

        }

        outList.add(new PumpSettingDTO("CFG_MM_RF_ENABLED", parseResultEnable(rd[12]), PumpConfigurationGroup.General));
        outList.add(new PumpSettingDTO("CFG_MM_BLOCK_ENABLED", parseResultEnable(rd[13]), PumpConfigurationGroup.General));

        outList.add(new PumpSettingDTO("PCFG_TEMP_BASAL_TYPE", rd[14] != 0 ? "Percent" : "Units", PumpConfigurationGroup.Basal));

        if (rd[14] == 1) {
            outList.add(new PumpSettingDTO("PCFG_TEMP_BASAL_PERCENT", "" + rd[15], PumpConfigurationGroup.Basal));
        }

        outList.add(new PumpSettingDTO("CFG_PARADIGM_LINK_ENABLE", parseResultEnable(rd[16]), PumpConfigurationGroup.General));

        decodeInsulinActionSetting(rd, outList);

        return outList;
    }


    public List<PumpSettingDTO> decodeSettings(byte[] rd) {
        List<PumpSettingDTO> outList = decodeSettings512(rd);

        outList.add(new PumpSettingDTO("PCFG_MM_RESERVOIR_WARNING_TYPE_TIME", rd[18] != 0 ? "PCFG_MM_RESERVOIR_WARNING_TYPE_TIME" : "PCFG_MM_RESERVOIR_WARNING_TYPE_UNITS", PumpConfigurationGroup.Other));

        outList.add(new PumpSettingDTO("PCFG_MM_SRESERVOIR_WARNING_POINT", "" + ByteUtil.asUINT8(rd[19]), PumpConfigurationGroup.Other));

        outList.add(new PumpSettingDTO("CFG_MM_KEYPAD_LOCKED", parseResultEnable(rd[20]), PumpConfigurationGroup.Other));


        if (MedtronicDeviceType.isSameDevice(pumpModel, MedtronicDeviceType.Medtronic_523andHigher)) {

            outList.add(new PumpSettingDTO("PCFG_BOLUS_SCROLL_STEP_SIZE", "" + rd[21], PumpConfigurationGroup.Bolus));
            outList.add(new PumpSettingDTO("PCFG_CAPTURE_EVENT_ENABLE", parseResultEnable(rd[22]), PumpConfigurationGroup.Other));
            outList.add(new PumpSettingDTO("PCFG_OTHER_DEVICE_ENABLE", parseResultEnable(rd[23]), PumpConfigurationGroup.Other));
            outList.add(new PumpSettingDTO("PCFG_OTHER_DEVICE_PAIRED_STATE", parseResultEnable(rd[24]), PumpConfigurationGroup.Other));
        }

        return outList;
    }


    protected String parseResultEnable(int i) {
        switch (i) {
            case 0:
                return "No";
            case 1:
                return "Yes";
            default:
                return "???";
        }
    }


    public float getStrokesPerUnit(boolean isBasal) {
        return isBasal ? 40.0f : 10.0f;
    }


    // 512
    public void decodeInsulinActionSetting(byte ai[], List<PumpSettingDTO> outList) {
        if (MedtronicDeviceType.isSameDevice(pumpModel, MedtronicDeviceType.Medtronic_512_712)) {
            outList.add(new PumpSettingDTO("PCFG_INSULIN_ACTION_TYPE", (ai[17] != 0 ? "Regular" : "Fast"), PumpConfigurationGroup.Insulin));
        } else {
            int i = ai[17];
            String s = "";

            if ((i == 0) || (i == 1)) {
                s = ai[17] != 0 ? "Regular" : "Fast";
            } else {
                if (i == 15)
                    s = "Unset";
                else
                    s = "Curve: " + i;
            }

            outList.add(new PumpSettingDTO("PCFG_INSULIN_ACTION_TYPE", s, PumpConfigurationGroup.Insulin));
        }
    }


    public double decodeBasalInsulin(int i) {
        return (double) i / (double) getStrokesPerUnit(true);
    }


    public double decodeBolusInsulin(int i) {

        return (double) i / (double) getStrokesPerUnit(false);
    }


    private int getSettingIndexMaxBasal() {
        return is523orHigher() ? 7 : 6;
    }


    private int getSettingIndexTimeDisplayFormat() {
        return is523orHigher() ? 9 : 8;
    }


    public double decodeMaxBolus(byte ai[]) {
        return is523orHigher() ? decodeBolusInsulin(ByteUtil.toInt(ai[5], ai[6])) : decodeBolusInsulin(ByteUtil.asUINT8(ai[5]));
    }


    private boolean is523orHigher() {
        return (MedtronicDeviceType.isSameDevice(pumpModel, MedtronicDeviceType.Medtronic_523andHigher));
    }


}
