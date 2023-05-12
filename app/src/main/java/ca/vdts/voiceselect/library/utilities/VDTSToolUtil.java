package ca.vdts.voiceselect.library.utilities;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility that contains general tools
 */
public class VDTSToolUtil {
    public static Date getTimeStamp() {
        return Calendar.getInstance(TimeZone.getDefault()).getTime();
    }
}
