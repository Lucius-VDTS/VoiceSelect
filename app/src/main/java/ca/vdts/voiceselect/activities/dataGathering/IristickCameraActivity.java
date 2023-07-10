package ca.vdts.voiceselect.activities.dataGathering;

import android.os.Bundle;
import android.os.Environment;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;

import com.iristick.sdk.Experimental;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.camera.IRICamera;
import com.iristick.sdk.camera.IRICameraProfile;
import com.iristick.sdk.camera.IRICameraSession;
import com.iristick.sdk.camera.IRICameraType;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.library.utilities.VDTSMediaScannerUtil;

public class IristickCameraActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(IristickCameraActivity.class);

    //Views
    private TextureView cameraPreview;

    private IRICamera iriCamera;
    private IRICameraSession iriCameraSession;
    private boolean iriCameraCaptureInProgress = false;
    private IristickCameraActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_iristick_camera);

        IristickSDK.registerListener(this.getLifecycle(), this);

        cameraPreview = findViewById(R.id.cameraPreview);
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        initializeIristick(headset);
    }

    private void initializeIristick(IRIHeadset headset) {
        //todo - select camera based on voice command
        if (headset.findCamera(IRICameraType.WIDE_ANGLE) != null) {
            iriCamera = headset.findCamera(IRICameraType.WIDE_ANGLE);
        } else if (headset.findCamera(IRICameraType.ZOOM) != null) {
            iriCamera = headset.findCamera(IRICameraType.ZOOM);
        }

        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new IristickHUD();
            return iristickHUD;
        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                takePhoto -> takePhoto.add(("Take Picture"), this::takeIriPicture)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                takePhoto -> takePhoto.add(("Navigate Back"), this::finish)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                takePhoto -> takePhoto.add(("Close Camera"), this::finish)
        );
    }

    @OptIn(markerClass = Experimental.class)
    private void openIriCameraSession() {
        iriCameraSession = iriCamera.openSession(
                getLifecycle(), IRICameraProfile.STILL_CAPTURE, ic -> {
                    ic.addOutput(cameraPreview);
                    ic.addOutput(iristickHUD.cameraPreviewHUD);
                });
    }

    @OptIn(markerClass = Experimental.class)
    private void takeIriPicture() {
        if (iriCameraSession == null || iriCameraCaptureInProgress) {
            return;
        }

        final File photoDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS),
                "VoiceSelect/Pictures");

        if (!photoDir.exists()) {
            boolean mkdirResult = photoDir.mkdirs();
            if (!mkdirResult) {
                LOG.info("Failed to create image directory");
                return;
            }
        }

        iriCameraCaptureInProgress = true;
        iriCameraSession.captureStillImage((iriCameraSession, iriPhoto) -> {
            iriCameraCaptureInProgress = false;
            try {
                final File cameraOutput = new File(
                        photoDir,
                        "IMG_" + LocalDateTime.now().toString()
                                .replace(":", "_") + ".jpg");

                OutputStream outputStream = Files.newOutputStream(cameraOutput.toPath());

                if (iriPhoto != null) {
                    iriPhoto.writeJpeg(outputStream);
                }

                new VDTSMediaScannerUtil(this, cameraOutput);

                LOG.info("Capture succeeded");
                Toast.makeText(this, "Capture succeeded", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                LOG.error("Capture failed", e);
                Toast.makeText(this, "Capture failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public class IristickHUD extends IRIWindow {
        private TextureView cameraPreviewHUD;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_iristick_camera_hud);

            cameraPreviewHUD = findViewById(R.id.cameraPreviewHUD);

            openIriCameraSession();
        }
    }
}
