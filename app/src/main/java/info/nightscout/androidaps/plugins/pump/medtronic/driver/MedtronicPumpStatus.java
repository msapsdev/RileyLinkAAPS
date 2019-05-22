package info.nightscout.androidaps.plugins.pump.medtronic.driver;


import com.gxwtech.roundtrip2.MainApp;
import com.gxwtech.roundtrip2.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import info.nightscout.androidaps.interfaces.PumpDescription;
import info.nightscout.androidaps.plugins.pump.common.data.PumpStatus;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkConst;
import info.nightscout.androidaps.plugins.pump.common.hw.rileylink.RileyLinkUtil;
import info.nightscout.androidaps.plugins.pump.medtronic.util.MedtronicConst;
import info.nightscout.utils.SP;

/**
 * Created by andy on 4/28/18.
 */

public class MedtronicPumpStatus extends PumpStatus {


    //private static MedtronicPumpStatus medtronicPumpStatus = new MedtronicPumpStatus();
    private static Logger LOG = LoggerFactory.getLogger(MedtronicPumpStatus.class);

    public String errorDescription = null;
    public String serialNumber;
    public PumpType pumpType = null;
    public String pumpFrequency = null;
    public String rileyLinkAddress = null;
    public Integer maxBolus;
    public Integer maxBasal;
    public long timeIndex;
    public Date time;
    public double remainUnits = 0;
    public int remainBattery = 0;
    public double currentBasal = 0;

    // fixme
    public int tempBasalInProgress = 0;
    public int tempBasalRatio = 0;
    public int tempBasalRemainMin = 0;
    public Date tempBasalStart;
    public Date last_bolus_time;
    public double last_bolus_amount = 0;
    String regexMac = "([\\da-fA-F]{1,2}(?:\\:|$)){6}";
    String regexSN = "[0-9]{6}";
    private String[] frequencies;
    private boolean isFrequencyUS = false;
    private Map<String, PumpType> medtronicPumpMap = null;

    public MedtronicPumpStatus(PumpDescription pumpDescription) {
        super(pumpDescription);
    }

    public long getTimeIndex() {
        return (long) Math.ceil(time.getTime() / 60000d);
    }


    // fixme

    public void setTimeIndex(long timeIndex) {
        this.timeIndex = timeIndex;
    }

    @Override
    public void initSettings() {

        this.activeProfileName = "STD";
        this.reservoirRemainingUnits = 75d;
        this.batteryRemaining = 75;

        if (this.medtronicPumpMap == null) createMedtronicPumpMap();
    }


    private void createMedtronicPumpMap() {

        medtronicPumpMap = new HashMap<>();
        medtronicPumpMap.put("512", PumpType.Minimed_512_712);
        medtronicPumpMap.put("712", PumpType.Minimed_512_712);
        medtronicPumpMap.put("515", PumpType.Minimed_515_715);
        medtronicPumpMap.put("715", PumpType.Minimed_515_715);

        medtronicPumpMap.put("522", PumpType.Minimed_522_722);
        medtronicPumpMap.put("722", PumpType.Minimed_522_722);
        medtronicPumpMap.put("523", PumpType.Minimed_523_723);
        medtronicPumpMap.put("723", PumpType.Minimed_523_723);
        medtronicPumpMap.put("554", PumpType.Minimed_554_754_Veo);
        medtronicPumpMap.put("754", PumpType.Minimed_554_754_Veo);

        frequencies = new String[2];
        frequencies[0] = MainApp.gs(R.string.medtronic_pump_frequency_us);
        frequencies[1] = MainApp.gs(R.string.medtronic_pump_frequency_worldwide);
    }


    public void verifyConfiguration() {
        try {

            // FIXME don't reload information several times
            if (this.medtronicPumpMap == null) createMedtronicPumpMap();


            this.errorDescription = null;
            this.serialNumber = null;
            this.pumpType = null;
            this.pumpFrequency = null;
            this.rileyLinkAddress = null;
            this.maxBolus = null;
            this.maxBasal = null;


            String serialNr = SP.getString(MedtronicConst.Prefs.PumpSerial, null);

            if (serialNr == null) {
                this.errorDescription = MainApp.gs(R.string.medtronic_error_serial_not_set);
                return;
            } else {
                if (!serialNr.matches(regexSN)) {
                    this.errorDescription = MainApp.gs(R.string.medtronic_error_serial_invalid);
                    return;
                } else {
                    this.serialNumber = serialNr;
                }
            }


            String pumpType = SP.getString(MedtronicConst.Prefs.PumpType, null);

            if (pumpType == null) {
                this.errorDescription = MainApp.gs(R.string.medtronic_error_pump_type_not_set);
                return;
            } else {
                String pumpTypePart = pumpType.substring(0, 3);

                if (!pumpTypePart.matches("[0-9]{3}")) {
                    this.errorDescription = MainApp.gs(R.string.medtronic_error_pump_type_invalid);
                    return;
                } else {
                    this.pumpType = medtronicPumpMap.get(pumpTypePart);

                    RileyLinkUtil.setPumpStatus(this);

                    if (pumpTypePart.startsWith("7")) this.reservoirFullUnits = "300";
                    else this.reservoirFullUnits = "180";
                }
            }


            String pumpFrequency = SP.getString(MedtronicConst.Prefs.PumpFrequency, null);

            if (pumpFrequency == null) {
                this.errorDescription = MainApp.gs(R.string.medtronic_error_pump_frequency_not_set);
                return;
            } else {
                if (!pumpFrequency.equals(frequencies[0]) && !pumpFrequency.equals(frequencies[1])) {
                    this.errorDescription = MainApp.gs(R.string.medtronic_error_pump_frequency_invalid);
                    return;
                } else {
                    this.pumpFrequency = pumpFrequency;
                    this.isFrequencyUS = pumpFrequency.equals(frequencies[0]);
                }
            }


            String rileyLinkAddress = SP.getString(RileyLinkConst.Prefs.RileyLinkAddress, null);

            if (rileyLinkAddress == null) {
                this.errorDescription = MainApp.gs(R.string.medtronic_error_rileylink_address_invalid);
                return;
            } else {
                if (!rileyLinkAddress.matches(regexMac)) {
                    this.errorDescription = MainApp.gs(R.string.medtronic_error_rileylink_address_invalid);
                } else {
                    this.rileyLinkAddress = rileyLinkAddress;
                }
            }


            String value = SP.getString(MedtronicConst.Prefs.MaxBolus, "25");

            maxBolus = Integer.parseInt(value);

            if (maxBolus > 25) {
                SP.putString(MedtronicConst.Prefs.MaxBolus, "25");
            }

            value = SP.getString(MedtronicConst.Prefs.MaxBasal, "35");

            maxBasal = Integer.parseInt(value);

            if (maxBasal > 35) {
                SP.putString(MedtronicConst.Prefs.MaxBasal, "35");
            }

        } catch (Exception ex) {
            this.errorDescription = ex.getMessage();
            LOG.error("Error on Verification: " + ex.getMessage(), ex);
        }
    }


    public String getErrorInfo() {
        verifyConfiguration();

        return (this.errorDescription == null) ? "-" : this.errorDescription;
    }


    @Override
    public void refreshConfiguration() {
        verifyConfiguration();
    }


}
