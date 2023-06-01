package ca.vdts.voiceselect.library.utilities;

import java.util.Comparator;

import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedInterface;

/**
 * Utility used to compare and order two names
 */
public class VDTSNameOrderUtil implements Comparator<VDTSIndexedNamedInterface> {
    private static VDTSNameOrderUtil instance;

    public static VDTSNameOrderUtil getInstance() {
        if (instance == null) {
            synchronized (VDTSNameOrderUtil.class) {
                instance = new VDTSNameOrderUtil();
            }
        }

        return instance;
    }

    @Override
    public int compare(VDTSIndexedNamedInterface name1, VDTSIndexedNamedInterface name2) {
        return name1.name().compareToIgnoreCase(name2.name());
    }
}
