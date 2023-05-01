package ca.vdts.voiceselect.library.services;

import java.util.Comparator;

import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedEntityInterface;

/**
 * Service used to compare two names
 */
public class VDTSNameOrderService implements Comparator<VDTSIndexedNamedEntityInterface> {
    private static VDTSNameOrderService instance;

    public static VDTSNameOrderService getInstance() {
        if (instance == null) {
            synchronized (VDTSNameOrderService.class) {
                instance = new VDTSNameOrderService();
            }
        }

        return instance;
    }

    @Override
    public int compare(VDTSIndexedNamedEntityInterface name1, VDTSIndexedNamedEntityInterface name2) {
        return name1.name().compareToIgnoreCase(name2.name());
    }
}
