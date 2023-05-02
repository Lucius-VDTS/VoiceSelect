package ca.vdts.voiceselect.library.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ca.vdts.voiceselect.library.database.repositories.VDTSPrefRepository;

/**
 * Key/Value getters and setters for VDTSPref entity.
 */
public class VDTSPrefKeyValue {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSPrefKeyValue.class);

    private final VDTSPrefRepository prefRepo;

    public VDTSPrefKeyValue(VDTSPrefRepository prefRepo) {
        this.prefRepo = prefRepo;
    }

    public boolean hasKey(String key) {
        return prefRepo.find(key) != null;
    }

    public String getString(String key) {
        ca.vdts.voiceselect.library.database.entities.VDTSPref pref = prefRepo.find(key);
        if (pref!= null) {
            return pref.getValue();
        } else {
            return null;
        }
    }

    public String getString(String key, String defaultValue) {
        ca.vdts.voiceselect.library.database.entities.VDTSPref pref = prefRepo.find(key);
        if (pref!= null) {
            return pref.getValue();
        } else {
            prefRepo.insert(new ca.vdts.voiceselect.library.database.entities.VDTSPref(key,defaultValue));
            return defaultValue;
        }
    }

    public Long getLong(String key) {
        try {
            final String value = getString(key);
            if (value != null) {
                return Long.parseLong(value);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return a Long: ", key, e);
            return null;
        }
    }

    public Long getLong(String key, long defaultValue) {
        try {
            return Long.parseLong(getString(key, Long.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return a Long: ", key, e);
            return null;
        }
    }

    public Double getDouble(String key) {
        try {
            final String value = getString(key);
            if (value != null) {
                return Double.parseDouble(value);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return a Double: ", key, e);
            return null;
        }
    }

    public Double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(getString(key, Double.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return a Double: ", key, e);
            return null;
        }
    }

    public Integer getInt(String key) {
        try {
            final String value = getString(key);
            if (value != null) {
                return Integer.parseInt(value);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an Integer: ", key, e);
            return null;
        }
    }

    public Integer getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(getString(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an Integer: ", key, e);
            return null;
        }
    }

    public Boolean getBoolean(String key) {
        try {
            final String value = getString(key);
            if (value != null) {
                return Boolean.parseBoolean(value);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an Boolean: ", key, e);
            return null;
        }
    }

    public Boolean getBoolean(String key, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(getString(key,defaultValue? "True":"False"));
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an Boolean: ", key, e);
            return null;
        }
    }

    public Set<String> getStringSet(String key) {
        try {
            final String value = getString(key);
            if (value != null) {
                return stringToSet(value);
            } else {
                return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an String Set: ", key, e);
            return null;
        }
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue) {
        try {
            String value = defaultValue != null? defaultValue.toString() : null;
            return stringToSet(getString(key,value));
        } catch (NumberFormatException e) {
            LOG.error("VDTSPrefKeyValue {} did not return an String Set: ", key, e);
            return null;
        }
    }

    public void setPref(String key, String value) {
        ca.vdts.voiceselect.library.database.entities.VDTSPref pref = prefRepo.find(key);
        if (pref!= null) {
            pref.setValue(value);
            prefRepo.update(pref);
        } else {
            prefRepo.insert(new ca.vdts.voiceselect.library.database.entities.VDTSPref(key,value));
        }
    }

    public void setLong(String key, long value) {
        setPref(key, Long.toString(value));
    }

    public void setDouble(String key, double value) {
        setPref(key, Double.toString(value));
    }

    public void setInt(String key, int value) {
        setPref(key, Integer.toString(value));
    }

    public void setBoolean(String key, boolean value) {
        setPref(key, Boolean.toString(value));
    }

    public void setStringSet(String key, Set<String> value) {
        if (value != null) {
            setPref(key, value.toString());
        } else {
            setPref(key, null);
        }
    }

    private Set<String> stringToSet(String string) {
        if (string != null){
            string = string.replace("[", "").replace("]", "").replace(" ", "");
            String[] tokens = string.split(",");

            Set<String> set = new HashSet<>();
            Collections.addAll(set, tokens);
            return set;
        } else {
            return null;
        }
    }
}
