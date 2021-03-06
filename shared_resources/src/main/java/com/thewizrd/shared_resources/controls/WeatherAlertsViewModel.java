package com.thewizrd.shared_resources.controls;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class WeatherAlertsViewModel extends ObservableViewModel {
    private String locationKey;

    private MutableLiveData<List<WeatherAlertViewModel>> alerts;

    private LiveData<List<WeatherAlertViewModel>> currentAlertsData;

    public WeatherAlertsViewModel() {
        alerts = new MutableLiveData<>();
    }

    public LiveData<List<WeatherAlertViewModel>> getAlerts() {
        return alerts;
    }

    @MainThread
    public void updateAlerts(@NonNull LocationData location) {
        if (!ObjectsCompat.equals(this.locationKey, location.getQuery())) {
            this.locationKey = location.getQuery();

            if (currentAlertsData != null) {
                currentAlertsData.removeObserver(alertObserver);
            }

            LiveData<WeatherAlerts> weatherAlertsLiveData = AsyncTask.await(new Callable<LiveData<WeatherAlerts>>() {
                @Override
                public LiveData<WeatherAlerts> call() {
                    return Settings.getWeatherDAO().getLiveWeatherAlertData(locationKey);
                }
            });
            currentAlertsData = Transformations.map(weatherAlertsLiveData,
                    new Function<WeatherAlerts, List<WeatherAlertViewModel>>() {
                        @Override
                        public List<WeatherAlertViewModel> apply(WeatherAlerts weatherAlerts) {
                            List<WeatherAlertViewModel> alerts;

                            if (weatherAlerts != null && weatherAlerts.getAlerts() != null && !weatherAlerts.getAlerts().isEmpty()) {
                                alerts = new ArrayList<>(weatherAlerts.getAlerts().size());
                                final ZonedDateTime now = ZonedDateTime.now();

                                for (WeatherAlert alert : weatherAlerts.getAlerts()) {
                                    // Skip if alert has expired
                                    if (!alert.getExpiresDate().isAfter(now) || alert.getDate().isAfter(now))
                                        continue;

                                    WeatherAlertViewModel alertView = new WeatherAlertViewModel(alert);
                                    alerts.add(alertView);
                                }

                                return alerts;
                            }

                            return Collections.emptyList();
                        }
                    });

            currentAlertsData.observeForever(alertObserver);

            if (alerts != null) {
                alerts.postValue(currentAlertsData.getValue());
            }
        }
    }

    private Observer<List<WeatherAlertViewModel>> alertObserver = new Observer<List<WeatherAlertViewModel>>() {
        @Override
        public void onChanged(List<WeatherAlertViewModel> alertViewModels) {
            if (alerts != null) {
                alerts.postValue(alertViewModels);
            }
        }
    };

    @Override
    protected void onCleared() {
        super.onCleared();

        locationKey = null;

        if (currentAlertsData != null)
            currentAlertsData.removeObserver(alertObserver);

        currentAlertsData = null;

        alerts = null;
    }
}
