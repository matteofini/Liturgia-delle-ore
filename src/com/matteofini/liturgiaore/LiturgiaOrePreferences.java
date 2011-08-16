package com.matteofini.liturgiaore;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class LiturgiaOrePreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		//Toast.makeText(getApplicationContext(), prefs.getString("filepath", "/sdcard/download/"), Toast.LENGTH_LONG).show();
		final Preference pref = (Preference) findPreference("filepath");
		pref.setSummary(prefs.getString("filepath", "/sdcard/download/"));
		
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String val = String.valueOf(newValue);
				File f = new File(val);
				if(!f.exists()){
					Log.println(Log.INFO, "LiturgiaOrePreferences", "creata cartella "+val);
					f.mkdir();
					pref.setSummary(prefs.getString("filepath", "/sdcard/download/"));
				}
				return true;
			}
		});
	}
	
	@Override
	public void onBackPressed() {
		startActivity(new Intent(getBaseContext(), LiturgiaOre.class));
		finish();
	}	
}
