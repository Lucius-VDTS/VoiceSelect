package ca.vdts.voiceselect.library.utilities;

import androidx.room.TypeConverter;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.Arrays;

import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedInterface;

/**
 * ENUM used for time period dropdowns
 */
public enum VDTSTimePeriodUtil implements VDTSIndexedNamedInterface {
    @SerializedName("0")
    SEC(0, "Seconds"),
    @SerializedName("1")
    MIN(1, "Minutes"),
    @SerializedName("2")
    HRS(2, "Hours"),
    @SerializedName("3")
    DAY(3, "Days"),
    @SerializedName("4")
    WEEK(4, "Weeks"),
    @SerializedName("5")
    MTH(5, "Months"),
    @SerializedName("6")
    YRS(6, "Years");

    private final long id;
    private final String name;

    VDTSTimePeriodUtil(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getID() {
        return id;
    }

    public String getName() {
        return name;
    }


    @Override
    public long id() {
        return id;
    }

    public static VDTSTimePeriodUtil getByID(long id) {
        return Arrays.stream(VDTSTimePeriodUtil.values())
                .filter(VDTSTimePeriodUtil -> VDTSTimePeriodUtil.id() == id)
                .findFirst()
                .orElse(MTH);
    }

    public static LocalDateTime getTime(boolean add, long length, VDTSTimePeriodUtil unit) {
        switch (unit) {
            case SEC:
                if (add) {
                    return LocalDateTime.now().plusSeconds(length);
                } else {
                    return LocalDateTime.now().minusSeconds(length);
                }
            case MIN:
                if (add) {
                    return LocalDateTime.now().plusMinutes(length);
                } else {
                    return LocalDateTime.now().minusMinutes(length);
                }
            case HRS:
                if (add) {
                    return LocalDateTime.now().plusHours(length);
                } else {
                    return LocalDateTime.now().minusHours(length);
                }
            case DAY:
                if (add) {
                    return LocalDateTime.now().plusDays(length);
                } else {
                    return LocalDateTime.now().minusDays(length);
                }
            case WEEK:
                if (add) {
                    return LocalDateTime.now().plusWeeks(length);
                } else {
                    return LocalDateTime.now().minusWeeks(length);
                }
            case MTH:
            default:
                if (add) {
                    return LocalDateTime.now().plusMonths(length);
                } else {
                    return LocalDateTime.now().minusMonths(length);
                }
            case YRS:
                if (add) {
                    return LocalDateTime.now().plusYears(length);
                } else {
                    return LocalDateTime.now().minusYears(length);
                }
        }
    }

    public static LocalDateTime getTime(boolean add, long length, long unitID) {
        return getTime(add, length, getByID(unitID));
    }

    @TypeConverter
    public static VDTSTimePeriodUtil toPeriod(long id) {
        return VDTSTimePeriodUtil.getByID(id);
    }

    @TypeConverter
    public static long toLong(VDTSTimePeriodUtil period) {
        return period != null ? period.id() : 5;
    }
}
