package info.nightscout.androidaps.plugins.pump.medtronic.data.dto;

import org.joda.time.LocalDateTime;

import info.nightscout.androidaps.plugins.pump.common.utils.StringUtil;

/**
 * Created by andy on 6/2/18.
 */

public class PumpTimeStampedRecord {

    protected LocalDateTime localDateTime;
    protected int decimalPrecission = 2;

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime ATechDate) {
        this.localDateTime = ATechDate;
    }

    public String getFormattedDecimal(double value) {
        return StringUtil.getFormatedValueUS(value, this.decimalPrecission);
    }

}
