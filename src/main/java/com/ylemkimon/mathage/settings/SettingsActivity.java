package com.ylemkimon.mathage.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Process;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import com.freshdesk.mobihelp.Mobihelp;
import com.freshdesk.mobihelp.MobihelpConfig;
import com.ylemkimon.mathage.BuildConfig;
import com.ylemkimon.mathage.R;
import com.ylemkimon.mathage.TutorialActivity;

import java.util.Arrays;
import java.util.Locale;

public class SettingsActivity extends Activity {
    public static final String EXTRA_HELP = "HELP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        Fragment fragment;
        if (extras == null || extras.getInt(EXTRA_HELP, R.id.action_settings) == R.id.action_settings) {
            setTitle(R.string.action_settings);
            fragment = new SettingsFragment();
        } else {
            setTitle(R.string.action_help);
            fragment = new SettingsFragment();
        }

        fragment.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction()
                .replace(R.id.preference, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void restartService(Intent intent) {
        PendingIntent pi = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pi);
        Process.killProcess(Process.myPid());
    }

    private void restartService() {
        this.restartService(getIntent());
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null && getArguments().getInt(EXTRA_HELP) == R.id.action_help) {
                addPreferencesFromResource(R.xml.preference_help);
                findPreference("help_version").setSummary(BuildConfig.VERSION_NAME);
            } else {
                addPreferencesFromResource(R.xml.preference_settings);
                findPreference("pref_kb_theme").setSummary(getResources().getStringArray(R.array.pref_kb_theme_entries)[Arrays.asList((getResources().getStringArray(R.array.pref_kb_theme_values))).indexOf(getPreferenceManager().getSharedPreferences().getString("pref_kb_theme", getString(R.string.pref_kb_theme_default)))]);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            Context context = getActivity();
            if (preference.getKey().equals("help_support")) {
                MobihelpConfig config = Locale.getDefault().getLanguage().equals("ko")
                        ? new MobihelpConfig("https://ylemkimon.freshdesk.com", "mathage-1-54f3ac9542cc7114a6e3740ccee75db3", "a51dfa63a6bea61b4c2ff3b09a1ba37179d358cf")
                        : new MobihelpConfig("https://ylemkimon.freshdesk.com", "mathageenglish-1-e4007da3052087989bc393f973ee76cb", "7dd616ec6aa3a27b65dfa4d040caa49ac8a3c963");
                config.setAutoReplyEnabled(true);
                Mobihelp.init(context, config);
                Mobihelp.showSupport(context);
            } else if (preference.getKey().equals("help_tutorial"))
                startActivity(new Intent(context, TutorialActivity.class));
            else if (preference.getKey().equals("pref_appList"))
                startActivity(new Intent(context, SettingsAppListActivity.class));
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
            sharedPref.edit().commit();
            ((SettingsActivity) getActivity()).restartService();
        }
    }
}