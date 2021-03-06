package com.thewizrd.simpleweather.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.main.MainActivity;

public class WeatherNotificationBuilder {
    private static final String TAG = "WeatherNotificationBuilder";

    static Notification updateNotification(String notificationID, final WeatherNowViewModel viewModel) {
        Context context = App.getInstance().getAppContext();

        // Build update
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.weather_notification_layout);

        String condition = viewModel.getCurCondition();
        String hiTemp = StringUtils.removeNonDigitChars(viewModel.getHiTemp());
        String loTemp = StringUtils.removeNonDigitChars(viewModel.getLoTemp());
        String temp = viewModel.getCurTemp() != null ?
                StringUtils.removeNonDigitChars(viewModel.getCurTemp().toString()) : "--";

        // Weather icon
        updateViews.setImageViewResource(R.id.weather_icon,
                WeatherUtils.getWeatherIconResource(viewModel.getWeatherIcon()));

        // Location Name
        updateViews.setTextViewText(R.id.location_name, viewModel.getLocation());

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather,
                String.format("%s°%s - %s", !StringUtils.isNullOrWhitespace(temp) ? temp : "--", viewModel.getTempUnit(), condition));

        // Details
        updateViews.setTextViewText(R.id.condition_hi, !StringUtils.isNullOrWhitespace(hiTemp) ? hiTemp + "°" : "--");
        updateViews.setTextViewText(R.id.condition_lo, !StringUtils.isNullOrWhitespace(loTemp) ? loTemp + "°" : "--");
        updateViews.setViewVisibility(R.id.condition_hilo_layout, viewModel.isShowHiLo() ? View.VISIBLE : View.GONE);

        // Get extras
        DetailItemViewModel chanceModel = null, windModel = null, feelsLikeModel = null, humidityModel = null, popRainModel = null, popSnowModel = null;
        for (DetailItemViewModel input : viewModel.getWeatherDetails()) {
            if (input.getDetailsType() == WeatherDetailsType.POPCHANCE || input.getDetailsType() == WeatherDetailsType.POPCLOUDINESS) {
                chanceModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                windModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.FEELSLIKE) {
                feelsLikeModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.HUMIDITY) {
                humidityModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.POPRAIN) {
                popRainModel = input;
            } else if (input.getDetailsType() == WeatherDetailsType.POPSNOW) {
                popSnowModel = input;
            }

            if (chanceModel != null && windModel != null && feelsLikeModel != null && humidityModel != null && popRainModel != null && popSnowModel != null) {
                break;
            }
        }

        // Extras
        int textSize = (int) ActivityUtils.dpToPx(context, 24f);
        if (chanceModel != null) {
            updateViews.setImageViewBitmap(R.id.weather_popicon,
                    ImageUtils.weatherIconToBitmap(context, chanceModel.getIcon(), textSize, false)
            );
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.getValue());
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE);
        }
        if (windModel != null) {
            if (windModel.getIconRotation() != 0) {
                updateViews.setImageViewBitmap(R.id.weather_windicon,
                        ImageUtils.rotateBitmap(ImageUtils.bitmapFromDrawable(context, R.drawable.direction_up), windModel.getIconRotation())
                );
            } else {
                updateViews.setImageViewResource(R.id.weather_windicon, R.drawable.direction_up);
            }
            String speed = TextUtils.isEmpty(windModel.getValue()) ? "" : windModel.getValue().toString();
            speed = speed.split(",")[0];
            updateViews.setTextViewText(R.id.weather_windspeed, speed);
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE);
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE);
        }
        updateViews.setViewVisibility(R.id.extra_layout, chanceModel != null || windModel != null ? View.VISIBLE : View.GONE);

        // Extras 2
        RemoteViews bigUpdateViews;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            bigUpdateViews = new RemoteViews(updateViews);
        } else {
            bigUpdateViews = updateViews.clone();
        }
        if (feelsLikeModel != null) {
            bigUpdateViews.setTextViewText(R.id.feelslike_label, context.getString(R.string.label_feelslike));
            bigUpdateViews.setTextViewText(R.id.feelslike_temp, feelsLikeModel.getValue() + viewModel.getTempUnit());
            bigUpdateViews.setViewVisibility(R.id.feelslike_layout, View.VISIBLE);
        }
        if (humidityModel != null) {
            bigUpdateViews.setImageViewBitmap(R.id.humidity_icon,
                    ImageUtils.weatherIconToBitmap(context, humidityModel.getIcon(), textSize, false)
            );
            bigUpdateViews.setTextViewText(R.id.humidity, humidityModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.humidity_layout, View.VISIBLE);
        }
        if (popRainModel != null) {
            bigUpdateViews.setImageViewBitmap(R.id.precip_rain_icon,
                    ImageUtils.weatherIconToBitmap(context, popRainModel.getIcon(), textSize, false)
            );
            bigUpdateViews.setTextViewText(R.id.precip_rain, popRainModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.precip_rain_layout, View.VISIBLE);
        }
        if (popSnowModel != null) {
            bigUpdateViews.setTextViewText(R.id.precip_snow, popSnowModel.getValue());
            bigUpdateViews.setViewVisibility(R.id.precip_snow_layout, View.VISIBLE);
        }
        bigUpdateViews.setViewVisibility(R.id.extra2_layout, feelsLikeModel != null || humidityModel != null || popRainModel != null || popSnowModel != null ? View.VISIBLE : View.GONE);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, notificationID)
                        .setCustomContentView(updateViews)
                        .setCustomBigContentView(bigUpdateViews)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true);

        if (Settings.getNotificationIcon().equals(Settings.TEMPERATURE_ICON)) {
            Integer tempLevel = NumberUtils.tryParseInt(temp.replace("°", ""));

            if (tempLevel == null) {
                mBuilder.setSmallIcon(R.drawable.notification_temp_unknown);
            } else {
                mBuilder.setSmallIcon(WeatherNotificationTemp.getTempDrawable(tempLevel));
            }
        } else if (Settings.getNotificationIcon().equals(Settings.CONDITION_ICON)) {
            mBuilder.setSmallIcon(WeatherUtils.getWeatherIconResource(viewModel.getWeatherIcon()));
        }

        Intent onClickIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);
        mBuilder.setContentIntent(clickPendingIntent);

        // Builds the notification and issues it.
        Float temp_float = NumberUtils.tryParseFloat(temp);
        if (temp_float != null) {
            float temp_f = viewModel.getCurTemp().toString().endsWith(WeatherIcons.FAHRENHEIT) ?
                    temp_float : ConversionMethods.CtoF(temp_float);
            mBuilder.setColor(WeatherUtils.getColorFromTempF(temp_f));
        } else {
            mBuilder.setColor(Colors.SIMPLEBLUE);
        }
        return mBuilder.build();
    }
}
