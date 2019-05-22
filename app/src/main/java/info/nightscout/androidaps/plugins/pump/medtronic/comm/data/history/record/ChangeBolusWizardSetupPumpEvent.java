package info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.record;


import info.nightscout.androidaps.plugins.pump.medtronic.comm.data.history.TimeStampedRecord;

public class ChangeBolusWizardSetupPumpEvent extends TimeStampedRecord {

    public ChangeBolusWizardSetupPumpEvent() {
    }

    @Override
    public int getLength() {
        return 144;
    }

    @Override
    public String getShortTypeName() {
        return "Ch Bolus Wizard Setup";
    }

    @Override
    public boolean isAAPSRelevant() {
        return false;
    }
}
