/**
 *  Program: Liturgia delle Ore
 *  Author: Matteo Fini <mf.calimero@gmail.com>
 *  Year: 2011
 *  
 *	This file is part of "Liturgia delle Ore".
 *	"Liturgia delle Ore" is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  "Liturgia delle Ore" is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>. 
 */

package com.matteofini.liturgiaore;

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class LiturgiaOrePreferences extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.liturgiaore_preference);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		//Toast.makeText(getApplicationContext(), prefs.getString("filepath", "/sdcard/download/"), Toast.LENGTH_LONG).show();
		final Preference pref = (Preference) findPreference("filepath");
		pref.setSummary(prefs.getString("filepath", "/sdcard/download/"));
		
		pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String val = String.valueOf(newValue);
				File f = new File(val);
				if(val.startsWith("/sdcard/")){
					if(!f.exists()){
						if(!f.mkdirs()){
							Log.println(Log.INFO, "LiturgiaOrePreferences", "Impossibile creare la cartella "+val);
							showDialog(1);
							return false;
						}
						else{
							pref.setSummary(String.valueOf(newValue));
							Log.println(Log.INFO, "LiturgiaOrePreferences", "Creata cartella "+val);
							Toast.makeText(LiturgiaOrePreferences.this, "Cartella "+val+" creata.", Toast.LENGTH_LONG).show();
							return true;
						}
					}
					else{
						pref.setSummary(String.valueOf(newValue));
						Toast.makeText(LiturgiaOrePreferences.this, "Cartella "+val+" già esistente.", Toast.LENGTH_LONG).show();
						return true;
					}
				}
				else{
					showDialog(0);
					return false;
				}
			}
		});
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		if(id==0){
			AlertDialog d = adb.create();
			d.setTitle("Avviso");
			d.setMessage("Il percorso della cartella deve iniziare con '/sdcard/'");
			d.setButton(Dialog.BUTTON_NEUTRAL, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(0);
				}
			});
			return d;
		}
		else if(id==1){
			AlertDialog d = adb.create();
			d.setTitle("Avviso");
			d.setMessage("Impossibile creare la cartella. Non è permesso creare una cartella nel percorso scelto. Il percorso" +
					"della cartella deve iniziare con '/sdcard/'");
			d.setButton(Dialog.BUTTON_NEUTRAL, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(1);
				}
			});
			return d;
		}
		else
			return null;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
			startActivity(new Intent(getBaseContext(), LiturgiaOre.class));
			finish();
			return true;
		}
		else return super.onKeyDown(keyCode, event);
	}
	

	/*
	@Override
	public void onBackPressed() {
		startActivity(new Intent(getBaseContext(), LiturgiaOre.class));
		finish();
	}
	*/
	
}
