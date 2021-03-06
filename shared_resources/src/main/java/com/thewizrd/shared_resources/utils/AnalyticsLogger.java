package com.thewizrd.shared_resources.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.thewizrd.shared_resources.BuildConfig;
import com.thewizrd.shared_resources.SimpleLibrary;

@SuppressLint("MissingPermission")
public class AnalyticsLogger {
    private static final FirebaseAnalytics analytics;

    static {
        analytics = FirebaseAnalytics.getInstance(SimpleLibrary.getInstance().getAppContext());
    }

    public static void logEvent(@NonNull @Size(min = 1L, max = 40L) String eventName) {
        logEvent(eventName, null);
    }

    public static void logEvent(@NonNull @Size(min = 1L, max = 40L) String eventName, @Nullable Bundle properties) {
        if (BuildConfig.DEBUG) {
            String append = properties == null ? "" : StringUtils.lineSeparator() + properties.toString();
            Logger.writeLine(Log.INFO, "EVENT | " + eventName + append);
        } else {
            // NOTE: Firebase Analytics only supports eventName with alphanumeric characters and underscores
            analytics.logEvent(eventName.replaceAll("[^a-zA-Z0-9]", "_"), properties);
        }
    }
}
