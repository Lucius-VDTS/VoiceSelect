package ca.vdts.voiceselect.library.utilities;

import java.util.Comparator;

import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedEntityInterface;

/**
 * Utility used to compare and order two names
 */
public class VDTSNameOrderUtil implements Comparator<VDTSIndexedNamedEntityInterface> {
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
    public int compare(VDTSIndexedNamedEntityInterface name1, VDTSIndexedNamedEntityInterface name2) {
        return name1.name().compareToIgnoreCase(name2.name());
    }
}
