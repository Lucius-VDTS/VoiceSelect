package ca.vdts.voiceselect.library.services;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Service that contains general utilities and tools
 */
public class VDTSUtilService {
    public static Date getTimeStamp() {
        return Calendar.getInstance(TimeZone.getDefault()).getTime();
    }
}
