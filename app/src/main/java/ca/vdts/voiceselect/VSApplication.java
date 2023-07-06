package ca.vdts.voiceselect;

import com.iristick.sdk.IristickSDK;

import ca.vdts.voiceselect.library.VDTSApplication;

/**
 * Custom Voice Select application class.
 */
public class VSApplication extends VDTSApplication {

    @Override
    public void onCreate() {
        super.onCreate();

            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLInputFactory",
                    "com.fasterxml.aalto.stax.InputFactoryImpl"
            );
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLOutputFactory",
                    "com.fasterxml.aalto.stax.OutputFactoryImpl"
            );
            System.setProperty(
                    "org.apache.poi.javax.xml.stream.XMLEventFactory",
                    "com.fasterxml.aalto.stax.EventFactoryImpl"
            );

        IristickSDK.setup(this);
    }
}