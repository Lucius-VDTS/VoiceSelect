package ca.vdts.voiceselect.library.utilities;

import android.content.Context;

import ca.vdts.voiceselect.R;

public class VDTSBNFUtil {
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

    public static String toPhonetic(String letter, Context context) {
        if (letter == null) {
            return "";
        } else if (letter.equalsIgnoreCase(context.getString(R.string.A))) {
            return context.getString(R.string.Alpha);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.B))) {
            return context.getString(R.string.Bravo);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.C))) {
            return context.getString(R.string.Charlie);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.D))) {
            return context.getString(R.string.Delta);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.E))) {
            return context.getString(R.string.Echo);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.F))) {
            return context.getString(R.string.Foxtrot);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.G))) {
            return context.getString(R.string.Golf);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.H))) {
            return context.getString(R.string.Hotel);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.I))) {
            return context.getString(R.string.India);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.J))) {
            return context.getString(R.string.Juliette);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.K))) {
            return context.getString(R.string.Kilo);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.L))) {
            return context.getString(R.string.Lima);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.M))) {
            return context.getString(R.string.Mike);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.N))) {
            return context.getString(R.string.November_Alpha);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.O))) {
            return context.getString(R.string.Oscar);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.P))) {
            return context.getString(R.string.Papa);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.Q))) {
            return context.getString(R.string.Quebec);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.R))) {
            return context.getString(R.string.Romeo);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.S))) {
            return context.getString(R.string.Sierra);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.T))) {
            return context.getString(R.string.Tango);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.U))) {
            return context.getString(R.string.Uniform);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.V))) {
            return context.getString(R.string.Victor);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.W))) {
            return context.getString(R.string.Whiskey);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.X))) {
            return context.getString(R.string.XRay);
        } else if (letter.equalsIgnoreCase(context.getString(R.string.Y))) {
            return context.getString(R.string.Yankee);
        } else {
            return letter.equalsIgnoreCase(context.getString(R.string.Z)) ? context.getString(R.string.Zulu) : letter;
        }
    }
}
