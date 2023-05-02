package ca.vdts.voiceselect.library.database.converters;

import androidx.room.TypeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * Tool for converting database values.
 */
public class VDTSConverter {
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

    @TypeConverter
    public static LocalDateTime toLocalDateTime(String dateString) {
        return dateString != null ? LocalDateTime.parse(dateString) : null;
    }

    @TypeConverter
    public static String toLocalDateTimeString(LocalDateTime date) {
        return date != null ? date.toString() : null;
    }

    @TypeConverter
    public static LocalTime toLocalTime(String timeString) {
        return timeString != null ? LocalTime.parse(timeString) : null;
    }

    @TypeConverter
    public static String toLocalTimeString(LocalTime time) {
        return time != null ? time.toString() : null;
    }

    @TypeConverter
    public static LocalDate toLocalDate(String dateString) {
        return dateString != null ? LocalDate.parse(dateString) : null;
    }

    @TypeConverter
    public static String toLocalDateString(LocalDate date) {
        return date != null ? date.toString() : null;
    }
}
