package ca.vdts.voiceselect.library.services;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;

/**
 * Service that scans files and adds them to media apps, file explorers, etc.
 */
public class VDTSMediaScannerService implements MediaScannerConnection.MediaScannerConnectionClient {
    private final MediaScannerConnection mediaScanner;
    private final File file;

    public VDTSMediaScannerService(Context context, File file) {
        this.file = file;
        mediaScanner = new MediaScannerConnection(context, this);
        mediaScanner.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mediaScanner.scanFile(file.getAbsolutePath(), null);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        mediaScanner.disconnect();
    }
}
