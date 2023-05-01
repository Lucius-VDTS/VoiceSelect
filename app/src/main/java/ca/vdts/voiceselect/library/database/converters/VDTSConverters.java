package ca.vdts.voiceselect.library.database.converters;

import androidx.room.TypeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class VDTSConverters {
    @TypeConverter
    public static LocalDateTime toLocalDateTime(String dateString) {
        return dateString != null ? LocalDateTime.parse(dateString) : null;
    }

    @TypeConverter
    public static String toDateString(LocalDateTime date) {
        return date != null ? date.toString() : null;
    }

    @TypeConverter
    public static LocalDate toLocalDate(String dateString) {
        return dateString != null ? LocalDate.parse(dateString) : null;
    }

    @TypeConverter
    public static String toDateString(LocalDate date) {
        return date != null ? date.toString() : null;
    }

    @TypeConverter
    public static LocalTime toLocalTime(String timeString) {
        return timeString != null ? LocalTime.parse(timeString) : null;
    }

    @TypeConverter
    public static String toTimeString(LocalTime time) {
        return time != null ? time.toString() : null;
    }
}
