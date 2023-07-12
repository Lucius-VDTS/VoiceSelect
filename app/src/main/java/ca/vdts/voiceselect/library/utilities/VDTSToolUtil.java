package ca.vdts.voiceselect.library.utilities;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Utility that contains general tools
 */
public class VDTSToolUtil {
    public static Date getTimeStamp() {
        return Calendar.getInstance(TimeZone.getDefault()).getTime();
    }

    public static boolean isNumeric(String value) {
        Pattern PATTERN = Pattern.compile("^(-?0|-?[1-9]\\d*)(\\.\\d+)?(E\\d+)?$");
        return value != null && PATTERN.matcher(value).matches();
    }

    public static void showKeyboardForSomeReason(View view, Activity activity){
        if(view.requestFocus()){
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view,InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
