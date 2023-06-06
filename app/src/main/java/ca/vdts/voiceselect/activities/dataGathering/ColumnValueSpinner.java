package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;

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
        int dimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                resources.getDisplayMetrics()
        );
        layoutParams.setMargins(dimen, 0, dimen, 0);

        columnValues = new ArrayList<>(columnValuesByColumn);

        columnValueAdapter = new VDTSNamedAdapter<>(
                context,
                R.layout.adapter_spinner_named,
                columnValues
        );

        columnValueAdapter.setToStringFunction((columnValue, integer) -> columnValue.getName());

        columnValueSpinner = new Spinner(context);
        columnValueSpinner.setAdapter(columnValueAdapter);
        columnValueSpinner.setGravity(Gravity.CENTER);
        columnValueSpinner.setLayoutParams(layoutParams);
        columnValueSpinner.setPadding(dimen, dimen, dimen, dimen);
        columnValueSpinner.setAdapter(columnValueAdapter);
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
