package com.thewizrd.shared_resources.weatherdata;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.CancellationToken;
import com.thewizrd.shared_resources.utils.WeatherException;

public final class WeatherRequest {
    private WeatherRequest() {
    }

    public interface WeatherErrorListener {
        void onWeatherError(WeatherException wEx);
    }

    private boolean forceRefresh;
    private boolean loadAlerts;
    private boolean loadForecasts;
    private boolean forceLoadSavedData;
    private boolean shouldSaveData = true;
    private WeatherErrorListener errorListener;
    private CancellationToken cancellationToken;

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public boolean isLoadAlerts() {
        return loadAlerts;
    }

    public boolean isLoadForecasts() {
        return loadForecasts;
    }

    public boolean isForceLoadSavedData() {
        return forceLoadSavedData;
    }

    public boolean isShouldSaveData() {
        return shouldSaveData;
    }

    public WeatherErrorListener getErrorListener() {
        return errorListener;
    }

    public CancellationToken getCancellationToken() {
        return cancellationToken;
    }

    public static final class Builder {
        private WeatherRequest request;

        public Builder() {
            request = new WeatherRequest();
        }

        public Builder forceRefresh(boolean value) {
            request.forceRefresh = value;
            return this;
        }

        public Builder loadAlerts() {
            request.loadAlerts = true;
            return this;
        }

        public Builder loadForecasts() {
            request.loadForecasts = true;
            return this;
        }

        public Builder forceLoadSavedData() {
            request.forceLoadSavedData = true;
            request.forceRefresh = false;
            return this;
        }

        public Builder forceRefreshWithoutSave() {
            request.forceRefresh = true;
            request.forceLoadSavedData = false;
            request.shouldSaveData = false;
            return this;
        }

        public Builder setErrorListener(WeatherErrorListener listener) {
            request.errorListener = listener;
            return this;
        }

        public Builder setCancellationToken(@Nullable CancellationToken token) {
            request.cancellationToken = token;
            return this;
        }

        public WeatherRequest build() {
            return request;
        }
    }
}
