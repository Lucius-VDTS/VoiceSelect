package ca.vdts.voiceselect.library.services;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Set;

import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.library.VDTSApplication;

public class VDTSFeedbackService {
    private static final Logger LOG = LoggerFactory.getLogger(VSApplication.class);

    private final VDTSApplication vdtsApplication;

    private TextToSpeech ttsEngine;
    private final String packageName = "com.google.android.tts";

    public VDTSFeedbackService(VDTSApplication vdtsApplication) {
        this.vdtsApplication = vdtsApplication;
        initializeTTSEngine();
    }

    /**
     * Check if Google's TTS engine is installed. If installed set the TTS engine's language, voice,
     * locale, and features. Otherwise, install Google's TTS engine
     */
    private void initializeTTSEngine() {
        if ((isPackageInstalled(packageName, vdtsApplication.getPackageManager()))) {
            LOG.info(packageName + " is installed");
            ttsEngine = new TextToSpeech(vdtsApplication, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = ttsEngine.setLanguage(Locale.getDefault());

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        LOG.error("Language not supported");
                        ttsEngine.setLanguage(Locale.US);
                    } else {
                        String name = ttsEngine.getVoice().getName();
                        Locale locale = ttsEngine.getVoice().getLocale();
                        Set<String> features = ttsEngine.getVoice().getFeatures();

                        Voice voice = new Voice(name, locale, Voice.QUALITY_VERY_HIGH,
                                Voice.LATENCY_VERY_LOW, false, features);
                        ttsEngine.setVoice(voice);
                    }
                } else {
                    LOG.error("Failed to initialize TTS engine");
                }
            }, packageName);
        } else {
            LOG.info(packageName + "engine is not installed");
            installTTSEngine();
        }
    }

    /**
     * Check if a particular TTS engine is installed. Default/preferred engine is
     * Google's TTS engine
     * @param packageName - The TTS engine to check
     * @param packageManager - The device package manager
     * @return - True if package is installed
     */
    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                packageManager.getPackageInfo(packageName,
                        PackageManager.PackageInfoFlags.of(0));
            } else {
                packageManager.getPackageInfo(packageName, 0);
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if network is available to download package
     * @param application - Application's context
     * @return - True if network is available
     */
    private Boolean isNetworkAvailable(Application application) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
    }

    /**
     * Install Google's TTS engine from play store
     */
    private void installTTSEngine() {
        if (isNetworkAvailable(vdtsApplication)) {
            Intent installIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(
                            "https://play.google.com/store/apps/details?id=" + packageName));
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                    | Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    | Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                vdtsApplication.startActivity(installIntent);
            } catch (ActivityNotFoundException e) {
                LOG.error("Failed to install " + packageName);
            }
        } else {
            LOG.info("No network connection, can't download " + packageName);
        }
    }

    public TextToSpeech getTTSEngine() {
        return ttsEngine;
    }
}
