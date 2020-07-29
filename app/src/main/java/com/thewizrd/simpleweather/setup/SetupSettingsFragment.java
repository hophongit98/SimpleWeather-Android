package com.thewizrd.simpleweather.setup;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.transition.MaterialSharedAxis;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSetupSettingsBinding;

public class SetupSettingsFragment extends PreferenceFragmentCompat {

    private FragmentSetupSettingsBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupSettingsBinding.inflate(inflater, container, false);
        ViewGroup root = (ViewGroup) binding.getRoot();
        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        binding.fragmentContainer.addView(inflatedView);

        setDivider(new ColorDrawable(ActivityUtils.getColor(root.getContext(), R.attr.colorPrimary)));
        setDividerHeight((int) ActivityUtils.dpToPx(root.getContext(), 1f));

        return root;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_setup, null);

        final ListPreference notIconPref = findPreference("key_notificationicon");
        final SwitchPreferenceCompat onGoingPref = findPreference("key_ongoingnotification");

        onGoingPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean value = (boolean) newValue;
                notIconPref.setVisible(value);
                return true;
            }
        });

        notIconPref.setVisible(onGoingPref.isChecked());
    }
}
