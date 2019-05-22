package info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.record;

import info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.TimeStampedRecord;

public class ClearAlarmPumpEvent extends TimeStampedRecord {
    public ClearAlarmPumpEvent() {
    }

    @Override
    public String getShortTypeName() {
        return "Clear Alarm";
    }

    @Override
    public boolean isAAPSRelevant() {
        return false;
    }
}
