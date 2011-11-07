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
import java.io.FilenameFilter;

import com.matteofini.liturgiaore.LiturgiaOreAbstr.DIALOG_CODE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;

class FilterDirectory implements FilenameFilter {
	private String pattern;
	public FilterDirectory(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean accept(File dir, String filename) {
		if (dir.isDirectory() && filename.contains(pattern) && !filename.endsWith("zip"))
			return true;
		else return false;
	}
}

class UnzipThread extends Thread implements Runnable {
	private Handler h;
	public UnzipThread(Handler h, Runnable runnable) {
		super(runnable);
		this.h = h;
	}
}

class DownloadThread extends Thread implements Runnable {
	private Handler h;
	public DownloadThread(Handler h, Runnable runnable) {
		super(runnable);
		this.h = h;
	}
}

public class LiturgiaOre extends LiturgiaOreAbstr{
	private String filepath; 
	
	/*@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}*/	// TODO: se attivato impedisce cambiamento layout orizzontale
	
	@Override
	protected void onResume() {
		super.onResume();
		
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);		
		
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
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LiturgiaOre.this);
			filepath = prefs.getString("filepath", "/sdcard/download/");
			if(!filepath.startsWith("/sdcard"))
				showDialog(DIALOG_CODE.ERROR_BAD_PATH);
			
			View mainview = getWindow().getDecorView();
			LinearLayout container = (LinearLayout) mainview.findViewById(R.id.ListContainer);
			
			container.findViewById(R.id.button_liturgiaore).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("MessaLiturOre"))!=null)
						read(rootdir+"/00-ENTRA.htm");
				}
			});
				
			container.findViewById(R.id.button_rosario).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("Preghiere"))!=null)
						read(rootdir+"/PDArosarium/RVM.htm");
				}
			});
			
			container.findViewById(R.id.button_4settSalterio).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("Salterio4sett"))!=null)
						read(rootdir+"/00-ENTRA.htm");
				}
			});
			
			container.findViewById(R.id.button_breviariumRomanum).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("Breviarium"))!=null)
						read(rootdir+"/00-ENTRA.htm");
				}
			});
			
			container.findViewById(R.id.button_bibbiaCEI).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("BibbiaCEI"))!=null)
						read(rootdir+"/ENTRA.htm");
				}
			});
			
			container.findViewById(R.id.button_magisteroChiesa).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("MagistChiesa"))!=null)
						read(rootdir+"/mobile/m3.htm");
				}
			});
			
			container.findViewById(R.id.button_preghiere).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("Preghiere"))!=null)
						read(rootdir+"/mobile/m5.htm");
				}
			});
			
			container.findViewById(R.id.button_missaleRomanum).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("MissaleVO"))!=null)
						read(rootdir+"/index.htm");
				}
			});
			
			container.findViewById(R.id.button_ritodellamessa).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("Liturgia"))!=null)
						read(rootdir+"/testiPDA/ritomessa/ritomessa.htm");
				}
			});
			
			container.findViewById(R.id.button_conciliovaticanoII).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("MagistChiesa"))!=null)
						read(rootdir+"/testiPDA/cvii/indice.htm");
				}
			});
			
			container.findViewById(R.id.button_catechismo).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String rootdir;
					if((rootdir = open("MagistChiesa"))!=null)
						read(rootdir+"/PDAcompendio/index.htm");
				}
			});
		}
		else showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
	}
	
	protected File moduleExists(String mName) {
		File[] match = new File[0];
		File downloadDir = new File(getSharedPreferenceValue("filepath",LiturgiaOre.this));
		match = downloadDir.listFiles(new FilterDirectory(mName));
		if (match == null || match.length == 0)
			return null;
		else
			return match[0];
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(LiturgiaOre.this, Classic.class));
				finish();
				return true;
			}
		});
		menu.getItem(2).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showDialog(DIALOG_CODE.HELP_NEW);
				return true;
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
}