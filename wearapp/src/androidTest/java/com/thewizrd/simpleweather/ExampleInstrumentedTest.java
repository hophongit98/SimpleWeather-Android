package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Before
    public void init() {
        // Context of the app under test.
        final Context appContext = ApplicationProvider.getApplicationContext();

        ApplicationLib app = new ApplicationLib() {
            @Override
            public Context getAppContext() {
                return appContext.getApplicationContext();
            }

            @Override
            public SharedPreferences getPreferences() {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext());
            }

            @Override
            public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
                return null;
            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }
        };

        SimpleLibrary.init(app);
        AndroidThreeTen.init(appContext);

        // Start logger
        Logger.init(appContext);
    }

    @Test
    public void getWeatherTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void updateLocationQueryTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);

        Settings.setAPI(WeatherAPI.YAHOO);
        wm.updateAPI();

        if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                || (weather == null && locationData != null && !locationData.getWeatherSource().equals(Settings.getAPI()))) {
            // Update location query and source for new API
            String oldKey = locationData.getQuery();

            if (weather != null)
                locationData.setQuery(wm.updateLocationQuery(weather));
            else
                locationData.setQuery(wm.updateLocationQuery(locationData));

            locationData.setWeatherSource(Settings.getAPI());
        }

        weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void minDate() {
        LocalDateTime updateTime = DateTimeUtils.getLocalDateTimeMIN();
        assertNotNull(LocalDateTime.parse("1/1/1900 12:00:00 AM",
                DateTimeFormatter.ofPattern("M/d/yyyy h:mm:ss a", Locale.JAPAN)));
    }
}
