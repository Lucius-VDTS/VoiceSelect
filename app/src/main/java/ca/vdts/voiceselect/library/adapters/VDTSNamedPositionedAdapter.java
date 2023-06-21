package ca.vdts.voiceselect.library.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.function.BiFunction;

import ca.vdts.voiceselect.R;

/**
 * Generic array adapter for named entities (ex - spinners) that are positioned in an ordered list,
 * such as a scrollview.
 * @param <Entity>
 */
public class VDTSNamedPositionedAdapter<Entity> extends ArrayAdapter<Entity> {
    private final int resource;
    private int position;
    private final int spinnerPosition;

    private BiFunction<Entity, Integer, String> toStringFunction;

    public VDTSNamedPositionedAdapter(Context context, int resource,
                                      List<Entity> objects, int spinnerPosition) {
        super(context, resource, objects);
        this.resource = resource;
        this.spinnerPosition = spinnerPosition;
    }

    public VDTSNamedPositionedAdapter(Context context,  List<Entity> objects, int spinnerPosition) {
        this(context, R.layout.adapter_spinner_named, objects, spinnerPosition);
    }

    @NonNull
    @Override
    @SuppressLint("ViewHolder")
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View v = LayoutInflater
                .from(getContext())
                .inflate(resource, parent, false);
        final TextView tv = (TextView)v;

        final Entity item = getItem(position);
        this.position = position;
        final String text;
        if (item != null) {
            if (toStringFunction != null) {
                text = toStringFunction.apply(item, position);
            } else {
                text = item.toString();
            }
        } else {
            text = "";
        }

        tv.setText(text);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public int getSelectedEntityIndex() {
        return position;
    }

    public int getSelectedSpinnerPosition() { return spinnerPosition; }

    /**
     * Receives an entity and an integer and produces a string
     * @param toStringFunction
     */
    public void setToStringFunction(BiFunction<Entity, Integer, String> toStringFunction) {
        this.toStringFunction = toStringFunction;
    }
}
