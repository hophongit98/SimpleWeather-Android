package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.simpleweather.R;

public class DetailCard extends ConstraintLayout {
    private TextView detailLabel;
    private TextView detailIcon;
    private TextView detailValue;

    public DetailCard(Context context) {
        super(context);
        initialize(context);
    }

    public DetailCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public DetailCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.card_weather_detail, this);

        detailLabel = view.findViewById(R.id.detail_label);
        detailIcon = view.findViewById(R.id.detail_icon);
        detailValue = view.findViewById(R.id.detail_value);
    }

    public void setDetails(DetailItemViewModel viewModel) {
        detailLabel.setText(viewModel.getLabel());
        detailIcon.setText(viewModel.getIcon());
        detailValue.setText(viewModel.getValue());
        detailIcon.setRotation(viewModel.getIconRotation());
    }
}
