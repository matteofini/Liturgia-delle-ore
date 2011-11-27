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
import com.matteofini.liturgiaore.R;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ViewFlipper;

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
	
	OnLongClickListener buttonLongClickListener(final String modulo, final String home){
		return new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				AlertDialog.Builder adb = new AlertDialog.Builder(LiturgiaOre.this);
				final AlertDialog d = adb.create();
				d.setIcon(R.drawable.ic_menu_info);
				d.setTitle("Aggiorna");
				d.setMessage("Verrà effettuato nuovamente il download dell'archivio ed i file precedenti verranno sovrascritti.\n\n"
						+"ATTENZIONE: l'aggiornamento non garantisce che il contenuto sarà più recente: sarà tale solo quando è disponibile una nuova versione dei file, altrimenti i file saranno sovrascritti con la medesima versione.");
				d.setButton(DialogInterface.BUTTON1, "Continua", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						d.dismiss();
						open(modulo, true);
					}
				});
				d.setButton(DialogInterface.BUTTON2, "Cancella", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						d.dismiss();
					}
				});
				d.show();
				return true;
			}
		};
	}
	
	OnClickListener buttonClickListener(final String modulo, final String home){
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				Animation anim = AnimationUtils.loadAnimation(LiturgiaOre.this, R.anim.anim_button);
				anim.reset();
				
				Animation.AnimationListener anim_listener = new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}
					@Override
					public void onAnimationRepeat(Animation animation) {}
					@Override
					public void onAnimationEnd(Animation animation) {
						String rootdir;
						if((rootdir = open(modulo, false))!=null)
							read(rootdir+home);
					}
				};
				anim.setAnimationListener(anim_listener);
				View wp = (View) v.getParent();
				wp.startAnimation(anim);
			}
		};
	}
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.main);		
		View mainview = getLayoutInflater().inflate(R.layout.main, null);
		
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
			
			LinearLayout container = (LinearLayout) mainview.findViewById(R.id.ListContainer);
			
			container.findViewById(R.id.button_liturgiaore).setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					registerForContextMenu(v);
					return true;
				}
			});
			
			container.findViewById(R.id.button_liturgiaore).setOnLongClickListener(buttonLongClickListener("MessaLiturOre", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_rosario).setOnLongClickListener(buttonLongClickListener("Preghiere", "/PDArosarium/RVM.htm"));
			container.findViewById(R.id.button_4settSalterio).setOnLongClickListener(buttonLongClickListener("Salterio4sett", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_breviariumRomanum).setOnLongClickListener(buttonLongClickListener("Breviarium", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_bibbiaCEI).setOnLongClickListener(buttonLongClickListener("BibbiaCEI","/index.htm"));
			container.findViewById(R.id.button_magisteroChiesa).setOnLongClickListener(buttonLongClickListener("MagistChiesa", "/mobile/m3.htm"));
			container.findViewById(R.id.button_preghiere).setOnLongClickListener(buttonLongClickListener("Preghiere", "/mobile/m5.htm"));
			container.findViewById(R.id.button_missaleRomanum).setOnLongClickListener(buttonLongClickListener("MissaleFE", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_ritodellamessa).setOnLongClickListener(buttonLongClickListener("Liturgia", "/testiPDA/ritomessa/ritomessa.htm"));
			container.findViewById(R.id.button_conciliovaticanoII).setOnLongClickListener(buttonLongClickListener("MagistChiesa", "/testiPDA/cvii/indice.htm"));
			container.findViewById(R.id.button_catechismo).setOnLongClickListener(buttonLongClickListener("MagistChiesa", "/PDAcompendio/index.htm"));
			
			container.findViewById(R.id.button_liturgiaore).setOnClickListener(buttonClickListener("MessaLiturOre", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_rosario).setOnClickListener(buttonClickListener("Preghiere", "/PDArosarium/RVM.htm"));
			container.findViewById(R.id.button_4settSalterio).setOnClickListener(buttonClickListener("Salterio4sett", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_breviariumRomanum).setOnClickListener(buttonClickListener("Breviarium", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_bibbiaCEI).setOnClickListener(buttonClickListener("BibbiaCEI","/index.htm"));
			container.findViewById(R.id.button_magisteroChiesa).setOnClickListener(buttonClickListener("MagistChiesa", "/mobile/m3.htm"));
			container.findViewById(R.id.button_preghiere).setOnClickListener(buttonClickListener("Preghiere", "/mobile/m5.htm"));
			container.findViewById(R.id.button_missaleRomanum).setOnClickListener(buttonClickListener("MissaleFE", "/00-ENTRA.htm"));
			container.findViewById(R.id.button_ritodellamessa).setOnClickListener(buttonClickListener("Liturgia", "/testiPDA/ritomessa/ritomessa.htm"));
			container.findViewById(R.id.button_conciliovaticanoII).setOnClickListener(buttonClickListener("MagistChiesa", "/testiPDA/cvii/indice.htm"));
			container.findViewById(R.id.button_catechismo).setOnClickListener(buttonClickListener("MagistChiesa", "/PDAcompendio/index.htm"));
		}
		else showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
		ViewFlipper flipper = (ViewFlipper) mainview.findViewById(R.id.ViewFlipper1);
		flipper.startFlipping();
		setContentView(mainview);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
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
		menu.getItem(3).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showDialog(DIALOG_CODE.DISCLAIMER);
				return true;
			}
		});
		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}
}