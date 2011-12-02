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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;

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

public abstract class LiturgiaOreAbstr extends Activity {
	private static final String WEB_URL = "http://www.maranatha.it/Dwl-Edition/";
	//protected static final String WEB_URL = "http://birillo.dyndns.org/Maranatha/";

	protected static String[] MODULES = new String[] { "Liturgia",
			"MessaLiturOre", "Salterio4sett", "Breviarium",
			"BibbiaCEI", "MagistChiesa", "Preghiere",
			"MissaleFE" };
	protected static final int START_DOWNLOAD_THREAD = 1000;
	protected static final int START_UNZIP_THREAD = 2000;

	protected String whatimdownloading = "";
	protected String whatimunzipping = "";
	
	protected static boolean mExternalStorageAvailable = false;
	protected static boolean mExternalStorageWriteable = false;
	protected BroadcastReceiver mExternalStorageReceiver;
	protected BroadcastReceiver mReceiver;
	
	protected static final String ACTION_SHOWDIALOG = "com.matteofini.liturgiaore.ACTION_SHOWDIALOG";

	protected static class DIALOG_CODE {
		protected static final int ERROR_SD_NOT_MOUNTED = 0;
		protected static final int ERROR_DIRECTORY_NOT_EXIST = 1;
		protected static final int ERROR_SD_ONLY_READ = 2;
		protected static final int ERROR_CONNECTION = 3;
		protected static final int CONNECTION_TIMEOUT = 302;
		protected static final int ERROR_BAD_PATH = 4;
		protected static final int DOWNLOAD = 101;
		protected static final int UNZIP = 102;
		protected static final int HELP_CL = 103;
		protected static final int DISCLAIMER = 104;
		protected static final int HELP_NEW = 105;
		protected static final int ERROR_FILE_NOT_FOUND = 401;
		protected static final int ERROR_FILE_SIZE = 501;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {	
				if(intent.getAction().equals(ACTION_SHOWDIALOG)){
					if(intent.getExtras().getInt("dialog")==DIALOG_CODE.UNZIP){
						dismissDialog(DIALOG_CODE.UNZIP);	//TODO: errore
						startActivity(new Intent(getApplicationContext(), LiturgiaOre.class));
						finish();
					}
					else if(intent.getExtras().getInt("dialog")==DIALOG_CODE.DOWNLOAD){
						dismissDialog(DIALOG_CODE.DOWNLOAD);
						showDialog(DIALOG_CODE.UNZIP);
					}
					else{
						dismissDialog(DIALOG_CODE.DOWNLOAD);
						showDialog(intent.getExtras().getInt("dialog"));
					}
				}
				
			}
		};
		IntentFilter iff = new IntentFilter();
		iff.addAction(ACTION_SHOWDIALOG);
		registerReceiver(mReceiver, iff);
		
		mExternalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction()==Intent.ACTION_MEDIA_MOUNTED){
					mExternalStorageAvailable = true;
					mExternalStorageWriteable = !intent.getExtras().getBoolean("read-only");
				}
				else{
					mExternalStorageAvailable = false;
					mExternalStorageWriteable = false;
				}
			}
		};
		IntentFilter iif = new IntentFilter();
		iif.addAction(Intent.ACTION_MEDIA_MOUNTED);
		iif.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		iif.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
		iif.addAction(Intent.ACTION_MEDIA_REMOVED);
		registerReceiver(mExternalStorageReceiver, iif);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mExternalStorageReceiver);
		unregisterReceiver(mReceiver);
	}

	protected String getEntryUrl(String name) {
		String url = "";
		if (name.contains("Home"))
			url = WEB_URL + "01-HomePage.zip";
		else if (name.contains("Liturgia"))
			url = WEB_URL + "02-Liturgia.zip";
		else if (name.contains("MessaLiturOre"))
			url = WEB_URL + "03-MessaLiturOre.zip";
		else if (name.contains("Salterio4sett"))
			url = WEB_URL + "03-Salterio4sett.zip";
		else if (name.contains("Breviarium"))
			url = WEB_URL + "03-Breviarium.zip";
		else if (name.contains("BibbiaCEI"))
			url = WEB_URL + "04-BibbiaCEI-74.zip";
		else if (name.contains("MagistChiesa"))
			url = WEB_URL + "05-MagistChiesa.zip";
		else if (name.contains("Preghiere"))
			url = WEB_URL + "06-Preghiere.zip";
		else if (name.contains("MissaleFE"))
			url = WEB_URL + "07-MissaleFE.zip";
		else
			url = "http://www.maranatha.it";
		return url;
	}

	

	protected String open(String name, boolean forcedownload) {
		String mName = name;
		String url = getEntryUrl(mName);
		File mRrootdir;
		if ((mRrootdir = moduleExists(mName)) != null && !forcedownload) {
			return mRrootdir.getAbsolutePath();
		} else {
			if (checkConnection()) {
				if (checkWritePermission()) {
					String path = getSharedPreferenceValue("filepath",this);
					download(mName, url, path);
				} else
					showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
			} else
				showDialog(DIALOG_CODE.ERROR_CONNECTION);
		}
		return null;
	}
	

	protected void download(String mName, String url, String path) {
		whatimdownloading = mName;
		whatimunzipping = mName;
		Intent intent = new Intent(this, DownloadService.class);
		intent.putExtra("name", mName);
		intent.putExtra("url", url);
		intent.putExtra("path", path);
		showDialog(DIALOG_CODE.DOWNLOAD);
		startService(intent);
		//download_nothread(mName, url, path);
	}

	protected abstract File moduleExists(String mName);
	
	protected void read(String path) {
		Intent i = new Intent();
		i.setAction(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_BROWSABLE);
		i.setType("text/html");
		i.setData(Uri.parse("file://" + path));
		sendBroadcast(i);
	}

	protected Boolean checkDate(String modulo, File mRrootdir) {
		long offline = mRrootdir.lastModified();
		long online = Long.MAX_VALUE;
		System.out.println("\t\t data file "+ DateFormat.format("dd/MM/yy h:mmaa", mRrootdir.lastModified()));

		String url = getEntryUrl(modulo);
		URL href_url;
		try {
			href_url = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) href_url.openConnection();
			conn.setRequestMethod("HEAD");
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			online = conn.getLastModified();
			System.out.println("\t\t last mod"+ DateFormat.format("dd/MM/yy h:mmaa", conn.getLastModified()));
			// conn.connect();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (online > offline)
			return false;
		else
			return true;
	}

	
	protected boolean checkConnection() {
		ConnectivityManager CONNM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = CONNM.getActiveNetworkInfo();
		return (netinfo != null && netinfo.isConnected());
	}

	protected boolean checkWritePermission() {
		return (mExternalStorageAvailable && mExternalStorageWriteable);
	}

	protected boolean checkReadPermission() {
		return (mExternalStorageAvailable);
	}

	protected String getSharedPreferenceValue(String key, Context ctx) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		return prefs.getString(key, "/sdcard/download/");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option, menu);
		menu.getItem(1).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(LiturgiaOreAbstr.this, LiturgiaOrePreferences.class));
				finish();
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
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setIcon(getResources().getDrawable(R.drawable.ic_menu_info));
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		String path = prefs.getString("filepath", "/sdcard/download/");

		if (id == DIALOG_CODE.ERROR_SD_NOT_MOUNTED) {
			AlertDialog d = adb.create();
			d.setTitle("Errore - Scheda SD non montata");
			d.setMessage("La scheda SD non risulta montata. Il telefono è collegato al PC? Disattiva la funzione 'archivio USB', oppure rimuovi la scheda da Esplora Risorse di Windows");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Chiudi", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			return d;
		} else if (id == DIALOG_CODE.ERROR_SD_ONLY_READ) {
			AlertDialog d = adb.create();
			d.setTitle("Errore - Scheda SD di sola lettura");
			d.setMessage("Non è possibile scrivere sulla scheda di memoria, verifica che funzioni correttamente poi riavvia l'applicazione. Per ora potrai solo leggere i file che hai già scaricato");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.ERROR_BAD_PATH) {
			AlertDialog d = adb.create();
			d.setTitle("Avviso - Controllare la cartella di download");
			d.setMessage("La cartella specificata per il download è "+path+". Il percorso della cartella dovrebbe inziare con '/sdcard/'");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua ugualmente",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
				}
			});
			d.setButton(DialogInterface.BUTTON_NEGATIVE, "Chiudi Applicazione",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
					finish();
				}
			});
			d.setButton(DialogInterface.BUTTON_NEUTRAL, "Impostazioni",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
					startActivity(new Intent(getApplicationContext(),LiturgiaOrePreferences.class));
					finish();
				}
			});
			return d;
		} else if (id == DIALOG_CODE.ERROR_CONNECTION) {
			AlertDialog d = adb.create();
			d.setTitle("Errore - Connessione dati non presente");
			d.setMessage("Nessuna connessione di rete disponibile, attiva la connessione WIFI o 3G se disponibili");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_CONNECTION);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.DOWNLOAD) {
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle("Download in corso");
			d.setMessage("il file sarà salvato in " + path);
			d.setSecondaryProgress(0);
			return d;
		} else if (id == DIALOG_CODE.UNZIP) {
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle("Estrazione dei file in corso");
			d.setMessage("il file sarà estratto in " + path+ "\nPossono occorrere un paio di minuti");
			d.setSecondaryProgress(0);
			return d;
		} else if (id == DIALOG_CODE.HELP_CL) {
			AlertDialog d = adb.create();
			d.setTitle("Aiuto");
			d.setIcon(getResources().getDrawable(R.drawable.ic_menu_info));
			d.setMessage("Premi SCARICA per scaricare l'archivio scelto sulla memoria esterna del tuo smartphone.\n"
							+ "\nPremi AGGIORNA per scaricare ed aggiornare i dati dell'archivio scelto (per esempio con la liturgia delle ore più recente).\n"
							+ "\nPremi LEGGI per aprire e leggere l'archivio scelto.\n"
							+ "\nATTENZIONE: l'aggiornamento non garantisce che il contenuto sarà più recente: sarà tale solo quando è disponibile una nuova versione dei file, altrimenti i file saranno sovrascritti con la medesima versione.(vedi disclaimer).\n"
							+ "\nPremi il tasto MENU del tuo smartphone per modificare la cartella di destinazione del salvataggio degli archivi.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Indietro",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.HELP_CL);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.HELP_NEW) {
			AlertDialog d = adb.create();
			d.setTitle("Aiuto");
			d.setIcon(getResources().getDrawable(R.drawable.ic_menu_info));
			d.setMessage("Per LEGGERE un archivio (liturgia, messale, etc) CLICCA su uno dei pulsanti colorati; se il file che contiene quell'archivio non è presente sulla memoria, verrà automaticamente scaricato e scompattato e sarà aperto per la lettura.\n\n" +
					"Per AGGIORNARE i file dei vari archivi TIENI PREMUTO A LUNGO su uno dei pulsanti colorati; l'aggiornamento scaricherà nuovamente l'archivio e sovrascriverà tutti i file precedenti.\n\n" +
					"ATTENZIONE: l'aggiornamento non garantisce che il contenuto sarà più recente: sarà tale solo quando è disponibile una nuova versione dei file, altrimenti i file saranno sovrascritti con la medesima versione.(vedi disclaimer).\n\n"+ 
					"Premi il tasto MENU del tuo smartphone per modificare la cartella di destinazione del salvataggio degli archivi.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Indietro",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.HELP_NEW);
				}
			});
			return d;
		}  else if (id == DIALOG_CODE.ERROR_FILE_SIZE) {
			AlertDialog d = adb.create();
			d.setTitle("Avviso");
			d.setMessage("La dimensione del file scaricato è minore di quella prevista. Non è un errore grave. "+ "Riprovare il download premendo Aggiorna.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_FILE_SIZE);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.ERROR_FILE_NOT_FOUND) {
			AlertDialog d = adb.create();
			d.setTitle("Errore - File non trovato");
			d.setMessage("Il file che si vuole scaricare non è all'indirizzo "+ WEB_URL+ ". "+ "Riprova il download, verifica la tua connessione Internet oppure contatta lo sviluppatore");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_FILE_NOT_FOUND);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.CONNECTION_TIMEOUT) {
			AlertDialog d = adb.create();
			d.setTitle("Timeout della connessione");
			d.setMessage("Il server a cui si chiede il file non ha risposto per troppo tempo. Potrebbe essere solo una congestione. Riprova più tardi e verifica la tua connessione Internet.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.CONNECTION_TIMEOUT);
				}
			});
			return d;
		} else if (id == DIALOG_CODE.DISCLAIMER) {
			AlertDialog d = adb.create();
			d.setTitle("Disclaimer");
			d.setIcon(getResources().getDrawable(R.drawable.ic_menu_info));
			d.setView(getLayoutInflater().inflate(R.layout.disclaimer, null));
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Indietro",new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.DISCLAIMER);
				}
			});
			return d;
		} else {
			return null;
		}
	}
	
	 
}
