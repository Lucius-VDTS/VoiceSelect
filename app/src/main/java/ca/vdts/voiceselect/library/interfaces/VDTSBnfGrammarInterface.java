package ca.vdts.voiceselect.library.interfaces;

import android.content.Context;

public interface VDTSBnfGrammarInterface {
    /**
     * Create a representation of the entity in grammar form to be spoken by the user
     * @return - String to be used with BNF grammars.
     */
    String toGrammar(Context context);
}
