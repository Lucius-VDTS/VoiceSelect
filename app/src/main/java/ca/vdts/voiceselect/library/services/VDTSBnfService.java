package ca.vdts.voiceselect.library.services;

import android.content.Context;

import ca.vdts.voiceselect.R;

public class VDTSBnfService {
    /**
     * Sanitizes grammar so that it will be usable for voice commands.
     * Several of the characters removed are replaced by spaces, except for " and #
     * which are replaced with "inches" and "NUMBER" respectively.
     *
     * @param grammar - Grammar to sanitize
     * @return - Sanitized grammar
     */
    public static String sanitizeGrammar(final String grammar, Context context) {
        return grammar.replace('%', ' ')
                .replace(',', ' ')
                .replace('/', ' ')
                .replace('\\',' ')
                .replace(';', ' ')
                .replace('.', ' ')
                .replace('$', ' ')
                .replace('`', ' ')
                .replace(':', ' ')
                .replace('<', ' ')
                .replace('>', ' ')
                .replaceAll("\\(", " ")
                .replaceAll("\\)", " ")
                .replaceAll("\\[", " ")
                .replaceAll("\\]", " ")
                .replaceAll("\\{", " ")
                .replaceAll("\\}", " ")
                .replaceAll("\"", context.getResources().getString(R.string.inches))
                .replaceAll("#", context.getResources().getString(R.string.number))
                .replaceAll(" {2,}", " ")
                .trim();
    }
}
