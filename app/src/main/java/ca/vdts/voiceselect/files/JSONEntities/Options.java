package ca.vdts.voiceselect.files.JSONEntities;

import static ca.vdts.voiceselect.library.VDTSApplication.PREF_ENTRY_METHOD;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_GPS;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_NAME;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_TIME;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.library.VDTSApplication;

/**
 * JSON version of option settings
 */
public class Options {
    private static final Logger LOG = LoggerFactory.getLogger(Options.class);

    @Expose
    @SerializedName("Options")
    private List<Option> options;

    public Options(VDTSApplication application) {
        LOG.debug("Creating Options entity");
        options = new ArrayList<>();
        /*
        if (preferences.hasKey(PREF_COMPANY_NAME)) {
            options.add(new Option(PREF_COMPANY_NAME, preferences.getString(PREF_COMPANY_NAME)));
        }
        if (preferences.hasKey(PREF_REQUIRE_PHOTO)) {
            options.add(new Option(PREF_REQUIRE_PHOTO, preferences.getString(PREF_REQUIRE_PHOTO)));
        }*/
        if (application.getVDTSPrefKeyValue().hasKey(PREF_PHOTO_PRINT_NAME)) {
            options.add(
                    new Option(
                            PREF_PHOTO_PRINT_NAME,
                            application.getVDTSPrefKeyValue().getString(PREF_PHOTO_PRINT_NAME)
                    )
            );
        }
        if (application.getVDTSPrefKeyValue().hasKey(PREF_PHOTO_PRINT_GPS)) {
            options.add(
                    new Option(
                            PREF_PHOTO_PRINT_GPS,
                            application.getVDTSPrefKeyValue().getString(PREF_PHOTO_PRINT_GPS)
                    )
            );
        }
        if (application.getVDTSPrefKeyValue().hasKey(PREF_PHOTO_PRINT_TIME)) {
            options.add(
                    new Option(
                            PREF_PHOTO_PRINT_TIME,
                            application.getVDTSPrefKeyValue().getString(PREF_PHOTO_PRINT_TIME)
                    )
            );
        }
        /*if (preferences.hasKey(PREF_ONE_DRIVE)) {
            options.add(new Option(PREF_ONE_DRIVE, preferences.getString(PREF_ONE_DRIVE)));
        }*/
        if (application.getVDTSPrefKeyValue().hasKey(PREF_EXPORT_CSV)) {
            options.add(
                    new Option(
                            PREF_EXPORT_CSV,
                            application.getVDTSPrefKeyValue().getString(PREF_EXPORT_CSV)
                    )
            );
        }
        if (application.getVDTSPrefKeyValue().hasKey(PREF_EXPORT_JSON)) {
            options.add(
                    new Option(
                            PREF_EXPORT_JSON,
                            application.getVDTSPrefKeyValue().getString(PREF_EXPORT_JSON)
                    )
            );
        }
        if (application.getVDTSPrefKeyValue().hasKey(PREF_EXPORT_XLSX)) {
            options.add(
                    new Option(
                            PREF_EXPORT_XLSX,
                            application.getVDTSPrefKeyValue().getString(PREF_EXPORT_XLSX)
                    )
            );
        }
        if (application.getVDTSPrefKeyValue().hasKey(PREF_ENTRY_METHOD)) {
            options.add(
                    new Option(
                            PREF_ENTRY_METHOD,
                            application.getVDTSPrefKeyValue().getString(PREF_ENTRY_METHOD)
                    )
            );
        }
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }
}
