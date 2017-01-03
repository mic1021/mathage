package com.ylemkimon.mathage.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.ylemkimon.mathage.R;

import java.util.Collections;
import java.util.Set;

public class SettingsManager {
    private final SharedPreferences sharedPref;

    private static final String PREF_EULA = "eula";
    private static final String PREF_HOTKEY = "pref_hotkey";
    private static final String PREF_NO_TUTORIAL = "no_tutorial";
    private static final String PREF_APP_LIST = "SettingsAppList";
    private static final String PREF_KB_THEME = "pref_kb_theme";
    private static final String PREF_PALM_LEFT = "pref_palmRejectionLeft";

    public static final String THEME_LXX = "lxx";
    public static final String THEME_LXX_DARK = "lxx_dark";
    public static final String THEME_ICS = "ics";
    public static final String THEME_KLP = "klp";

    public SettingsManager(Context context) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getHotkey() {
        return sharedPref.getString(PREF_HOTKEY, "=");
    }

    public Set<String> getAppList() {
        return sharedPref.getStringSet(PREF_APP_LIST, Collections.<String>emptySet());
    }

    public void setAppList(Set<String> apps) {
        sharedPref.edit()
                .putStringSet(PREF_APP_LIST, apps)
                .apply();
    }

    public boolean getEula() {
        return sharedPref.getBoolean(PREF_EULA, false);
    }

    public void setEula() {
        sharedPref.edit()
                .putBoolean(PREF_EULA, true)
                .apply();
    }

    public boolean getTutorial() {
        return sharedPref.getBoolean(PREF_NO_TUTORIAL, false);
    }

    public void setTutorial() {
        sharedPref.edit()
                .putBoolean(PREF_NO_TUTORIAL, true)
                .apply();
    }

    public boolean getPalm() {
        return sharedPref.getBoolean(PREF_PALM_LEFT, false);
    }

    public int getKeyboardTheme() {
        String theme = sharedPref.getString(PREF_KB_THEME, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? THEME_LXX : THEME_KLP);
        switch (theme) {
            case THEME_LXX:
                return R.style.lxx;
            case THEME_LXX_DARK:
                return R.style.lxx_dark;
            case THEME_ICS:
                return R.style.ics;
            default:
                return R.style.klp;
        }
    }
}
