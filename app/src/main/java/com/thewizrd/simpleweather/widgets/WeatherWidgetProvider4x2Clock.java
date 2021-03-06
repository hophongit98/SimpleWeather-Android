package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import static com.thewizrd.simpleweather.widgets.WidgetType.Widget4x2Clock;

public class WeatherWidgetProvider4x2Clock extends WeatherWidgetProvider {
    private static WeatherWidgetProvider4x2Clock sInstance;

    public static synchronized WeatherWidgetProvider4x2Clock getInstance() {
        if (sInstance == null)
            sInstance = new WeatherWidgetProvider4x2Clock();

        return sInstance;
    }

    // Overrides
    @Override
    public WidgetType getWidgetType() {
        return Widget4x2Clock;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.app_widget_4x2_clock;
    }

    @Override
    protected String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public ComponentName getComponentName() {
        return new ComponentName(App.getInstance().getAppContext(), getClassName());
    }

    @Override
    public void onEnabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Register tick receiver
            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_STARTCLOCK));
        }

        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Unregister tick receiver
            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_CANCELCLOCK));
        }

        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // Update clock widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentname = new ComponentName(context.getPackageName(), getClassName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentname);

            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_UPDATECLOCK)
                    .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));

            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_UPDATEDATE)
                    .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));
        } else {
            super.onReceive(context, intent);
        }
    }
}
