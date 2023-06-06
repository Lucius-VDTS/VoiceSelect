package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;

import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;

public class ColumnValueSpinnerAdapter {
    private final VDTSNamedAdapter<ColumnValue> columnValueAdapter;

    public ColumnValueSpinnerAdapter(Context context, List<ColumnValue> columnValuesByColumn) {
        columnValueAdapter = new VDTSNamedAdapter<>(
                context,
                R.layout.adapter_spinner_named,
                columnValuesByColumn
        );

        columnValueAdapter.setToStringFunction((columnValue, integer) -> columnValue.getName());
    }
}
