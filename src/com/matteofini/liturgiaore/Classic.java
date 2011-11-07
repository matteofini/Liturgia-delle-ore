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
import java.util.HashMap;

import com.matteofini.liturgiaore.LiturgiaOreAbstr.DIALOG_CODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;

public class Classic extends LiturgiaOreAbstr{
	private String filepath; 
	
	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}*/ // TODO: se attivato impedisce cambiamento layout orizzontale
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.classic);	
		String SDstate = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(SDstate)) {
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(SDstate)) {
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		if (mExternalStorageAvailable) {
			if (!mExternalStorageWriteable)
				showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Classic.this);
			filepath = prefs.getString("filepath", "/sdcard/download/");
			if(!filepath.startsWith("/sdcard")){
				showDialog(DIALOG_CODE.ERROR_BAD_PATH);
			}
			
			
			View mainview = getWindow().getDecorView();
			LinearLayout container = (LinearLayout) mainview.findViewById(R.id.ListContainer);
			
			/*
			"02-Liturgia", "03-MessaLiturOre", "03-Salterio4sett", "03-Breviarium", "04-BibbiaCEI-74", "05-MagistChiesa", "06-Preghiere","07-MissaleVO"
			*/
			HashMap<String, LinearLayout> hm_moduli = new HashMap<String, LinearLayout>(0); 
			LinearLayout ll_Liturgia = (LinearLayout) container.findViewById(R.id.Liturgia); hm_moduli.put("Liturgia", ll_Liturgia);
			LinearLayout ll_MessaLiturOre = (LinearLayout) container.findViewById(R.id.MessaLiturOre); hm_moduli.put("MessaLiturOre", ll_MessaLiturOre);
			LinearLayout ll_Salterio4sett = (LinearLayout) container.findViewById(R.id.Salterio4sett); hm_moduli.put("Salterio4sett", ll_Salterio4sett);
			LinearLayout ll_Breviarium = (LinearLayout) container.findViewById(R.id.Breviarium); hm_moduli.put("Breviarium", ll_Breviarium);
			LinearLayout ll_BibbiaCEI = (LinearLayout) container.findViewById(R.id.BibbiaCEI); hm_moduli.put("BibbiaCEI", ll_BibbiaCEI);
			LinearLayout ll_MagistChiesa = (LinearLayout) container.findViewById(R.id.MagistChiesa); hm_moduli.put("MagistChiesa", ll_MagistChiesa);
			LinearLayout ll_Preghiere = (LinearLayout) container.findViewById(R.id.Preghiere); hm_moduli.put("Preghiere", ll_Preghiere);
			LinearLayout ll_MissaleVO = (LinearLayout) container.findViewById(R.id.MissaleVO); hm_moduli.put("MissaleVO", ll_MissaleVO);

			File downloadDir = new File(filepath);
			for(final String mName : MODULES){
				LinearLayout ll_modulo = hm_moduli.get(mName);
				final String url = getEntryUrl(mName);	// TODO
				final File mRrootdir;
				if ((mRrootdir = moduleExists(mName)) == null) {
					Button b_down = (Button) ll_modulo.findViewById(R.id.cl_button_scarica);
					b_down.setEnabled(true);
					b_down.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(checkConnection()){
								if(checkWritePermission()){
									String path = getSharedPreferenceValue("filepath", Classic.this);
									download(mName, url, path);
								}
								else showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
							}
							else showDialog(DIALOG_CODE.ERROR_CONNECTION);
						}
					});
				}
				else{		// module "name" present => read || upgrade (overwrite)
					Button b_upgr = (Button) ll_modulo.findViewById(R.id.cl_button_aggiorna); 
					Button b_read = (Button) ll_modulo.findViewById(R.id.cl_button_leggi); 
					b_upgr.setEnabled(true);
					b_read.setEnabled(true);
					b_upgr.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(checkConnection()){
								if(checkWritePermission()){
									String path = getSharedPreferenceValue("filepath", Classic.this);
									download(mName, url, path);
								}
								else showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
							}
							else showDialog(DIALOG_CODE.ERROR_CONNECTION);
						}
					});
					b_read.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if(checkReadPermission()){
								if (!new File(mRrootdir.getAbsoluteFile()+"/00-ENTRA.htm").exists()){
									if (new File(mRrootdir.getAbsoluteFile()+"/index.htm").exists())
										read(mRrootdir.getAbsolutePath()+"/index.htm");
								}
								else
									read(mRrootdir.getAbsolutePath()+"/00-ENTRA.htm");
								
							}
							else showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
						}
					});
				}
			}
		}
		else showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mExternalStorageReceiver);
	}

	protected File moduleExists(String mName) {
		File[] match = new File[0];
		File downloadDir = new File(getSharedPreferenceValue("filepath",Classic.this));
		match = downloadDir.listFiles(new FilterDirectory(mName));
		if (match == null || match.length == 0)
			return null;
		else
			return match[0];
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.getItem(0).setTitle("vista rapida");
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(Classic.this, LiturgiaOre.class));
				finish();
				return true;
			}
		});
		menu.getItem(2).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showDialog(DIALOG_CODE.HELP_CL);
				return true;
			}
		});
	}
}
