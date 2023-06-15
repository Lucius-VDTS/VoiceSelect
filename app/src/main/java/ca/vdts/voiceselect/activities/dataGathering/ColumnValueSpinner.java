package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;

/**
 * Class contains programmatically generated column value spinners, which includes the dataset,
 * adapter, and spinner.
 */
public class ColumnValueSpinner {
    private final List<ColumnValue> columnValues;
    private final VDTSNamedAdapter<ColumnValue> columnValueAdapter;
    private final Spinner columnValueSpinner;

    public ColumnValueSpinner(Context context,
                              List<ColumnValue> columnValuesByColumn,
                              AdapterView.OnItemSelectedListener columnValueSpinnerListener) {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        Resources resources = context.getResources();

        int minWidthDimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                96,
                resources.getDisplayMetrics()
        );

        int marginPaddingDimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2,
                resources.getDisplayMetrics()
        );
        layoutParams.setMargins(marginPaddingDimen, 0, marginPaddingDimen, 0);

        columnValues = new ArrayList<>(columnValuesByColumn);
        columnValues.add(0, null);

        columnValueAdapter = new VDTSNamedAdapter<>(
                context,
                R.layout.adapter_spinner_named,
                columnValues
        );
        columnValueAdapter.setToStringFunction((columnValue, integer) -> columnValue.getName());

        columnValueSpinner = new Spinner(context);
        columnValueSpinner.setAdapter(columnValueAdapter);
        columnValueSpinner.setMinimumWidth(minWidthDimen);
        columnValueSpinner.setLayoutParams(layoutParams);
        columnValueSpinner.setPadding(
                marginPaddingDimen, marginPaddingDimen, marginPaddingDimen, marginPaddingDimen);
        columnValueSpinner.setGravity(Gravity.CENTER);
        columnValueSpinner.setBackground(
                ContextCompat.getDrawable(context, R.drawable.spinner_background));
        columnValueSpinner.setOnItemSelectedListener(columnValueSpinnerListener);
    }

    public List<ColumnValue> getColumnValues() {
        return columnValues;
    }

    public VDTSNamedAdapter<ColumnValue> getColumnValueAdapter() {
        return columnValueAdapter;
    }

    public Spinner getColumnValueSpinner() {
        return columnValueSpinner;
    }
}
