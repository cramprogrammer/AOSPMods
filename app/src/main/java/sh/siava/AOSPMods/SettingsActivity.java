package sh.siava.AOSPMods;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.nfx.android.rangebarpreference.RangeBarHelper;
import com.topjohnwu.superuser.Shell;


public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    Context DPContext;
    private static final String TITLE_TAG = "settingsActivityTitle";

    public void RestartSysUI(View view) {
        Shell.su("killall com.android.systemui").submit();
    }

    public void backButtonEnabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void backButtonDisabled(){
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DPContext = this.createDeviceProtectedStorageContext();
        DPContext.moveSharedPreferencesFrom(this, BuildConfig.APPLICATION_ID + "_preferences");
        super.onCreate(savedInstanceState);

        backButtonDisabled();
        
        //update settings from previous config file
        try {
            if(PreferenceManager.getDefaultSharedPreferences(DPContext).contains("Show4GIcon"))
            {
                boolean fourGEnabled = PreferenceManager.getDefaultSharedPreferences(DPContext).getBoolean("Show4GIcon", false);
                if(fourGEnabled)
                {
                    PreferenceManager.getDefaultSharedPreferences(DPContext).edit().putInt("LTE4GIconMod", 2).apply();
                }
                PreferenceManager.getDefaultSharedPreferences(DPContext).edit().remove("Show4GIcon").commit();
            }
        }
        catch(Exception e){}


        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }

        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                            backButtonDisabled();
                        }
                    }
                });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (getTitle() == getString(R.string.title_activity_settings)){
        backButtonDisabled();
        } else {
            backButtonEnabled();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        backButtonEnabled();
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class NavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.nav_prefs, rootKey);
        }

    }

    public static class ThemingFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.theming_prefs, rootKey);
        }

    }

    public static class LockScreenFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.lock_screen_prefs, rootKey);
        }
    }
    
    public static class SBBBFragment extends PreferenceFragmentCompat {
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_batterybar_prefs, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    updateVisibility(prefs);
                }
            });
            updateVisibility(prefs);
        }
    
        private void updateVisibility(SharedPreferences prefs) {
            String json = prefs.getString("batteryWarningRange", "");
            boolean critZero = RangeBarHelper.getLowValueFromJsonString(json) == 0;
            boolean warnZero = RangeBarHelper.getHighValueFromJsonString(json) == 0;
            boolean bBarEnabled = prefs.getBoolean("BBarEnabled", false);
            boolean isColorful = prefs.getBoolean("BBarColorful", false);
            boolean transitColors = prefs.getBoolean("BBarTransitColors", false);
    
            findPreference("batteryFastChargingColor").setVisible(prefs.getBoolean("indicateFastCharging", false) && bBarEnabled);
            findPreference("batteryChargingColor").setVisible(prefs.getBoolean("indicateCharging", false) && bBarEnabled);
            findPreference("batteryWarningColor").setVisible(!warnZero && bBarEnabled);
            findPreference("batteryCriticalColor").setVisible((!critZero || transitColors) && bBarEnabled && findPreference("batteryWarningColor").isVisible());
    
            findPreference("BBarTransitColors").setVisible(bBarEnabled && !isColorful);
            findPreference("BBOnlyWhileCharging").setVisible(bBarEnabled);
            findPreference("BBOnBottom").setVisible(bBarEnabled);
            findPreference("BBarColorful").setVisible(bBarEnabled);
            findPreference("BBOpacity").setVisible(bBarEnabled);
            findPreference("BBarHeight").setVisible(bBarEnabled);
            findPreference("BBSetCentered").setVisible(bBarEnabled);
            findPreference("indicateCharging").setVisible(bBarEnabled);
            findPreference("indicateFastCharging").setVisible(bBarEnabled);
            findPreference("batteryWarningRange").setVisible(bBarEnabled);
        }
    
    }
    
    public static class MiscFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.misc_prefs, rootKey);

        }
    }

    public static class SBCFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_clock_prefs, rootKey);
        }
    }

    /* Let's disable this for the time being...
    public static class ScreenOffFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.screen_off_prefs, rootKey);
        }
    }
    */

    public static class ThreeButtonNavFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.three_button_prefs, rootKey);
        }
    }

    public static class StatusbarFragment extends PreferenceFragmentCompat {

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("BatteryStyle")) {
                    updateBatteryMod();
                } else if (key.equals("BatteryIconScaleFactor")) {
                    updateBatteryIconScaleFactor();
                }
            }
        };

        private void updateBatteryIconScaleFactor() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());

                findPreference("BatteryIconScaleFactor").setSummary(prefs.getInt("BatteryIconScaleFactor", 50)*2 + getString(R.string.battery_size_summary));
            }
            catch(Exception e){
                return;
            }

        }

        private void updateBatteryMod() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());

                boolean enabled = !(prefs.getString("BatteryStyle", "0").equals(("0")));
                findPreference("BatteryShowPercent").setEnabled(enabled);
            }catch(Exception e){}
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.statusbar_settings, rootKey);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener(listener);
            updateBatteryMod();
            updateBatteryIconScaleFactor();
        }
    }

    public static class QuicksettingsFragment extends PreferenceFragmentCompat {

        Preference QSPulldownPercent;

        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("QSPulldownPercent")) {
                    updateQSPulldownPercent();
                } else if (key.equals("QSPullodwnEnabled")) {
                    updateQSPulldownEnabld();
                } else if (key.equals("QSFooterMod")) {
                    updateQSFooterMod();
                } else if (key.equals("QSBrightnessDisabled")){
                    updateQQSBrightness();
                }
            }
        };
    
        private void updateQQSBrightness() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
                boolean disabled = prefs.getBoolean("QSBrightnessDisabled", false);
        
                findPreference("QQSBrightnessEnabled").setEnabled(!disabled);
            } catch(Exception e){}
        }
    
        private void updateQSFooterMod() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
                boolean enabled = prefs.getBoolean("QSFooterMod", false);

                findPreference("QSFooterText").setEnabled(enabled);
            } catch(Exception e){}
        }

        private void updateQSPulldownEnabld() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
                boolean enabled = prefs.getBoolean("QSPullodwnEnabled", false);

                findPreference("QSPulldownPercent").setEnabled(enabled);
                findPreference("QSPulldownSide").setEnabled(enabled);
            }catch(Exception e){}
        }

        private void updateQSPulldownPercent() {
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());

                int value = prefs.getInt("QSPulldownPercent", 25);
                QSPulldownPercent.setSummary(value + "%");
            }catch(Exception e){}
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.quicksettings_prefs, rootKey);
            QSPulldownPercent = findPreference("QSPulldownPercent");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
            prefs.registerOnSharedPreferenceChangeListener(listener);
            updateQSPulldownPercent();
            updateQSPulldownEnabld();
            updateQSFooterMod();
            updateQQSBrightness();
        }
    }

    public static class GestureNavFragment extends PreferenceFragmentCompat {

        Context context;
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateBackGesture();
                updateNavPill();
            }
        };

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            this.context= context.createDeviceProtectedStorageContext();
        }

        private void updateNavPill() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
                findPreference("GesPillWidthModPos").setEnabled(prefs.getBoolean("GesPillWidthMod", true));
                findPreference("GesPillWidthModPos").setSummary(prefs.getInt("GesPillWidthModPos", 50)*2 + getString(R.string.pill_width_summary));
            } catch(Exception e){}
        }

        private void updateBackGesture() {
            try {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext());
                findPreference("BackLeftHeight").setEnabled(prefs.getBoolean("BackFromLeft", true));
                findPreference("BackRightHeight").setEnabled(prefs.getBoolean("BackFromRight", true));

                findPreference("BackLeftHeight").setSummary(prefs.getInt("BackLeftHeight", 100) + "%");
                findPreference("BackRightHeight").setSummary(prefs.getInt("BackRightHeight", 100) + "%");
            } catch(Exception e){}
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            getPreferenceManager().setStorageDeviceProtected();
            setPreferencesFromResource(R.xml.gesture_nav_perfs, rootKey);

            PreferenceManager.getDefaultSharedPreferences(getContext().createDeviceProtectedStorageContext()).registerOnSharedPreferenceChangeListener(listener);

            updateBackGesture();
            updateNavPill();
        }

    }
}