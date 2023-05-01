package ca.vdts.voiceselect.library.database.converters;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Tool for converting data field
 */
public class VDTSDateConverter {
    @TypeConverter
    public static Date toDate(Long value) {
        if (value != null) {
            return new Date(value);
        } else {
            return null;
        }
    }

    @TypeConverter
    public static Long fromDate(Date value) {
        if (value != null) {
            return value.getTime();
        } else {
            return null;
        }
    }
}
