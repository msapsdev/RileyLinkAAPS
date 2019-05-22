package info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.record;

import info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.TimeStampedRecord;

/**
 * Created by geoff on 6/5/16.
 */
public class ChangeBolusScrollStepSizePumpEvent extends TimeStampedRecord {
    public ChangeBolusScrollStepSizePumpEvent() {
    }

    @Override
    public String getShortTypeName() {
        return "Ch Bolus Scroll SS";
    }

    @Override
    public boolean isAAPSRelevant() {
        return false;
    }
}
