package com.matteofini.liturgiaore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

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
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

class FilterDirectory implements FilenameFilter {
	private String pattern;

	public FilterDirectory(String pattern) {
		this.pattern = pattern;
	}

	@Override
	public boolean accept(File dir, String filename) {
		if (dir.isDirectory() && filename.contains(pattern)
				&& !filename.endsWith("zip"))
			return true;
		else
			return false;
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

public class LiturgiaOre extends Activity {
	private static boolean mExternalStorageAvailable = false;
	private static boolean mExternalStorageWriteable = false;
	private BroadcastReceiver mExternalStorageReceiver;
	
	private static final String WEB_URL = "http://www.maranatha.it/Dwl-Edition/";
	private static final String DEBUG_URL = "http://birillo.dyndns.org/Maranatha/";
	private static final int START_DOWNLOAD_THREAD = 1000;
	private static final int START_UNZIP_THREAD = 2000;
	private static String whatimdownloading="";
	private static String whatimunzipping="";
	private DownloadThread _T_DOWNLOAD;
	private UnzipThread _T_UNZIP;
	
	protected static class DIALOG_CODE{
		protected static final int ERROR_SD_NOT_MOUNTED = 0;
		protected static final int ERROR_DIRECTORY_NOT_EXIST = 1;
		protected static final int ERROR_SD_ONLY_READ = 2;
		protected static final int ERROR_CONNECTION = 3;
		protected static final int CONNECTION_TIMEOUT = 302;
		protected static final int ERROR_BAD_PATH = 4;
		protected static final int DOWNLOAD = 101;
		protected static final int UNZIP = 102;
		protected static final int HELP = 103;
		protected static final int ERROR_FILE_NOT_FOUND = 401;
		protected static final int ERROR_FILE_SIZE = 501;	
	}

	private static String[] MODULES = new String[] { "02-Liturgia",
			"03-MessaLiturOre", "03-Salterio4sett", "03-Breviarium",
			"04-BibbiaCEI-74", "05-MagistChiesa", "06-Preghiere",
			"07-MissaleVO" };

	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
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
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
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
			String path = prefs.getString("filepath", "/sdcard/download/");
			
			if(!path.startsWith("/sdcard")){
				showDialog(DIALOG_CODE.ERROR_BAD_PATH);
			}

			View mainview = getLayoutInflater().inflate(R.layout.main, null);
			Drawable dr = getResources().getDrawable(R.drawable.sfondo);
			dr.setAlpha(200);
			mainview.setBackgroundDrawable(dr);
			
			File filedir = new File(path);
			ScrollView sv = (ScrollView) mainview.findViewById(R.id.ScrollView1);
			LinearLayout ll = (LinearLayout) sv.getChildAt(0);
			
			for (final String name : MODULES) {
				File[] match = new File[0];
				match = filedir.listFiles(new FilterDirectory(name));	// check if module "name" is present
				
				View row = getLayoutInflater().inflate(R.layout.row, null);
				ll.addView(row);
				TextView title = (TextView) row.findViewById(R.id.title);
				title.setText(getEntryName(name));	// TODO: refactor ??
				title.setContentDescription("pacchetto "+getEntryName(name));
				
				View b_down = row.findViewById(R.id.button_download);
				View b_read = row.findViewById(R.id.button_read);
				View b_upgr = row.findViewById(R.id.button_upgrade);
				b_down.setContentDescription("Scarica pacchetto "+getEntryName(name));
				b_read.setContentDescription("Leggi pacchetto"+getEntryName(name));
				b_upgr.setContentDescription("Aggiorna il pacchetto "+getEntryName(name));
								
				final String url = getEntryUrl(name);	// TODO
				if(match==null || match.length==0){	// module "name" not present || "/sdcard/dir" not exists --> download
					b_upgr.setEnabled(false); b_upgr.setClickable(false); b_upgr.setFocusable(false);
					b_read.setEnabled(false); b_read.setClickable(false); b_read.setFocusable(false);
					b_down.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
							ConnectivityManager CONNM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
							NetworkInfo netinfo = CONNM.getActiveNetworkInfo();
							if(netinfo!=null && netinfo.isConnected()){
								if(mExternalStorageAvailable && mExternalStorageWriteable){
									SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LiturgiaOre.this);
									String path = prefs.getString("filepath", "/sdcard/download/");
									download(name, url, path);
								}
								else{	
									showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
								}
							}
							else{
								showDialog(DIALOG_CODE.ERROR_CONNECTION);
							}
						}
					});
				}
				else {		// module "name" present --> read & upgrade (overwrite)
					b_down.setEnabled(false);
					b_down.setClickable(false); b_down.setFocusable(false);
					final File file = match[0];
					b_upgr.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
							ConnectivityManager CONNM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
							NetworkInfo netinfo = CONNM.getActiveNetworkInfo();
							if(netinfo!=null && netinfo.isConnected()){
								if(mExternalStorageAvailable && mExternalStorageWriteable){
									SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LiturgiaOre.this);
									String path = prefs.getString("filepath", "/sdcard/download/");
									download(name, url, path);
								}
								else{	
									showDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
								}
							}
							else{
								showDialog(DIALOG_CODE.ERROR_CONNECTION);
							}
						}
					});
					b_read.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							v.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
							if(mExternalStorageAvailable){
								String path = file.getAbsolutePath();
								Intent i = new Intent();
								i.setAction(Intent.ACTION_VIEW);
								i.addCategory(Intent.CATEGORY_BROWSABLE);
								if (!new File(file.getAbsoluteFile()+"/00-ENTRA.htm").exists()) {
									String[] split = file.getAbsolutePath().split("/");
									path = path+"/"+split[split.length - 1];
								}
								i.setType("text/html");
								i.setData(Uri.parse("file://" + path+ "/00-ENTRA.htm"));
								sendBroadcast(i);
							}
							else{
								showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
							}
						}
					});
				}
			}
			setContentView(mainview);	
			mainview.requestFocus();
		} else {
			showDialog(DIALOG_CODE.ERROR_SD_NOT_MOUNTED);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setIcon(getResources().getDrawable(R.drawable.alert));
		adb.setCancelable(true);	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LiturgiaOre.this);
		String path = prefs.getString("filepath", "/sdcard/download/");
		
		if(id==DIALOG_CODE.ERROR_SD_NOT_MOUNTED){
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
		}
		else if (id==DIALOG_CODE.ERROR_SD_ONLY_READ){
			AlertDialog d = adb.create();
			d.setTitle("Errore - Scheda SD di sola lettura");
			d.setMessage("Non è possibile scrivere sulla scheda di memoria, verifica che funzioni correttamente poi riavvia l'applicazione. Per ora potrai solo leggere" +
					"i file che hai già scaricato");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_SD_ONLY_READ);
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.ERROR_BAD_PATH){
			AlertDialog d = adb.create();
			d.setTitle("Avviso - Controllare la cartella di download");
			d.setMessage("La cartella specificata per il download è "+path+" . Il percorso della cartella dovrebbe inziare con '/sdcard/'");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua ugualmente", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
				}
			});
			d.setButton(DialogInterface.BUTTON_NEGATIVE, "Chiudi Applicazione", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
					finish();
				}
			});
			d.setButton(DialogInterface.BUTTON_NEUTRAL, "Impostazioni", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_BAD_PATH);
					startActivity(new Intent(LiturgiaOre.this, LiturgiaOrePreferences.class));
					finish();
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.ERROR_CONNECTION){
			AlertDialog d = adb.create();
			d.setTitle("Errore - Connessione dati non presente");
			d.setMessage("Nessuna connessione di rete disponibile, attiva la connessione WIFI o 3G se disponibili");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_CONNECTION);
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.DOWNLOAD){
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle("Download in corso");
			d.setMessage("di "+getEntryUrl(whatimdownloading)+" sarà salvato in "+path);
			d.setSecondaryProgress(0);
			d.setButton(DialogInterface.BUTTON_NEGATIVE, "Annulla", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.DOWNLOAD);
					_T_DOWNLOAD.stop();
					Toast.makeText(getApplicationContext(), "Downlaod interrotto. Premi SCARICA per effettuare nuovamente il download", Toast.LENGTH_LONG).show();
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.UNZIP){
			ProgressDialog d = new ProgressDialog(this);
			d.setCancelable(false);
			d.setTitle("Estrazione dei file in corso");
			d.setMessage(whatimunzipping+" sarà estratto in "+path);
			d.setSecondaryProgress(0);
			d.setButton(DialogInterface.BUTTON_NEGATIVE, "Annulla", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.UNZIP);
					_T_UNZIP.stop();
					Toast.makeText(getApplicationContext(), "Estrazione interrotta. Pacchetto "+whatimunzipping+" non utilizzabile a pieno. Premi AGGIORNA", Toast.LENGTH_LONG).show();
				}
			});
			return d;	
		}
		else if(id==DIALOG_CODE.HELP){
			AlertDialog d = adb.create();
			d.setTitle("Aiuto"); d.setIcon(getResources().getDrawable(R.drawable.help));
			d.setMessage("Premi SCARICA per scaricare il modulo scelto sulla memoria esterna del tuo smartphone.\n" +
					"\nPremi AGGIORNA per scaricare ed aggiornare i dati del modulo scelto (per esempio con la liturgia delle ore più recente).\n" +
					"\nPremi LEGGI per accedere alla homepage del modulo scelto.\n" +
					"\nPremi il tasto MENU del tuo smartphone per modificare la cartella di destinazione del salvataggio dei moduli.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Indietro", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.HELP);
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.ERROR_FILE_SIZE){
			AlertDialog d = adb.create();
			d.setTitle("Avviso");
			d.setMessage("La dimensione del file scaricato è minore di quella prevista. Non è un errore grave. " +
					"Riprovare il download premendo Aggiorna.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_FILE_SIZE);
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.ERROR_FILE_NOT_FOUND){
			AlertDialog d = adb.create();
			d.setTitle("Errore - File non trovato");
			d.setMessage("Il file che si vuole scaricare non è all'indirizzo "+ WEB_URL + ". " +
					"Riprova il download, verifica la tua connessione Internet oppure contatta lo sviluppatore");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.ERROR_FILE_NOT_FOUND);
				}
			});
			return d;
		}
		else if(id==DIALOG_CODE.CONNECTION_TIMEOUT){
			AlertDialog d = adb.create();
			d.setTitle("Timeout della connessione");
			d.setMessage("Il server a cui si chiede il file non ha risposto per troppo tempo. Potrebbe essere solo una congestione. Riprova più tardi e verifica la tua connessione Internet.");
			d.setButton(DialogInterface.BUTTON_POSITIVE, "Continua", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dismissDialog(DIALOG_CODE.CONNECTION_TIMEOUT);
				}
			});
			return d;
		}
		else{
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option, menu);
		menu.getItem(0).setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						startActivity(new Intent(LiturgiaOre.this,
								LiturgiaOrePreferences.class));
						finish();
						return true;
					}
				});
		menu.getItem(1).setOnMenuItemClickListener(
				new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						showDialog(DIALOG_CODE.HELP);
						return true;
					}
				});

		return super.onCreateOptionsMenu(menu);
	}

	Handler h = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.arg2 == 1) {
				showDialog(msg.arg1);
			}
			if(msg.arg2==START_UNZIP_THREAD){
				unzip((File)msg.obj, msg.getData().getString("path")); //unzip(newfile, path);
			}
		}
	};
	

	public void download(final String name, final String url, final String path) {
		whatimdownloading=name;
		showDialog(DIALOG_CODE.DOWNLOAD);
		DownloadThread t;
		_T_DOWNLOAD = t = new DownloadThread(h, new Runnable() {
			@Override
			public void run() {
				Log.println(Log.INFO, "LiturgiaOre",  "\t download "+url+" in "+path);
				try {					
					URL href_url = new URL(url);
					HttpURLConnection conn = (HttpURLConnection) href_url.openConnection();
					conn.setRequestMethod("GET");
					conn.setDoOutput(true);
					conn.setConnectTimeout(5000);
					conn.connect();

					BufferedInputStream in = new BufferedInputStream(conn.getInputStream(), 8192);
					File dir = new File(path);
					if (!dir.exists()){
						if(!dir.mkdirs())
							Log.println(Log.INFO, "LiturgiaOre", "\t directory "+dir.getName()+" NON creata");
						else
							Log.println(Log.INFO, "LiturgiaOre", "\t directory "+dir.getName()+" creata");
					}
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(path+"/"+name+".zip")), 8192);

					byte[] buffer = new byte[8192];
					int readed = 0;
					int downloaded = 0;
					while ((readed = in.read(buffer)) >= 0) {
						out.write(buffer, 0, readed);
						downloaded+=readed;
					}
					if(downloaded!=conn.getContentLength()){
						Log.println(Log.WARN, "LiturgiaOre", "\t downloaded "+downloaded+"B but expected "+conn.getContentLength()+"B in "+url);
						Message m = new Message();
						m.arg2=1; m.arg1=DIALOG_CODE.ERROR_FILE_SIZE;
						h.sendMessage(m);
					}
					else
						Log.println(Log.INFO, "LiturgiaOre", "\t downloaded "+downloaded+"B");
					out.flush();
					in.close();
					
					dismissDialog(DIALOG_CODE.DOWNLOAD);
					File newfile = new File(path+"/"+name+".zip");
					if(newfile.exists()){
						Message m = new Message();
						m.arg2=START_UNZIP_THREAD;
						m.obj = newfile;
						Bundle b = new Bundle();
						b.putString("path", path);
						m.setData(b);
						h.sendMessage(m);
						
						Vibrator VV = (Vibrator) getSystemService(VIBRATOR_SERVICE);
						VV.vibrate(500);
						//unzip(newfile, path);
					}
					else
						Log.println(Log.WARN, "LiturgiaOre", "\t An error has occurred during download. File "+name+" not created.");
				} catch (IOException e) {
					if (e.getClass() == UnknownHostException.class) {
						Message m = new Message();
						m.arg2=1; m.arg1=DIALOG_CODE.ERROR_CONNECTION;
						h.sendMessage(m);
						dismissDialog(DIALOG_CODE.DOWNLOAD);
					} 
					else if(e.getClass() == FileNotFoundException.class){
						Log.println(Log.WARN, "LiturgiaOre", "\t file all'indirizzo "+url+" non trovato");
						e.printStackTrace();
						Message m = new Message();
						m.arg2=1; m.arg1=DIALOG_CODE.ERROR_FILE_NOT_FOUND;
						h.sendMessage(m);
						dismissDialog(DIALOG_CODE.DOWNLOAD);
					} 
					else if(e.getClass()== SocketTimeoutException.class){
						Log.println(Log.WARN, "LiturgiaOre", "\t Timeout della connessione. Non è stato possibile contattare "+url);
						e.printStackTrace();
						Message m = new Message();
						m.arg2=1; m.arg1=DIALOG_CODE.CONNECTION_TIMEOUT;
						h.sendMessage(m);
						dismissDialog(DIALOG_CODE.DOWNLOAD);
					}
					else
						e.printStackTrace();
				}
			}
		});
		t.start();
	}

	public void unzip(final File newfile, final String path) {
		whatimunzipping=newfile.getName();
		showDialog(DIALOG_CODE.UNZIP);
		UnzipThread t;
		_T_UNZIP = t = new UnzipThread(h, new Runnable() {
			@Override
			public void run() {
				try {
					ZipFile zipfile = new ZipFile(newfile);
					Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipfile.entries();

					Log.println(Log.INFO, "LiturgiaOre", "\t Extracting "+newfile.getName()+" into "+path);
					while (entries.hasMoreElements()) {
						ZipEntry entry = (ZipEntry) entries.nextElement();
						BufferedInputStream in = new BufferedInputStream(zipfile.getInputStream(entry), 8192);
						File newfile = new File(path + "/" + entry.getName());
						if (entry.isDirectory()) {
							if (!newfile.mkdirs()) {
								if(!newfile.exists()){	// mkdirs() returns false if newfile already exists
									Log.println(Log.INFO, "\t LiturgiaOre.unzip()", "\t directory"+entry.getName()+" NON creata");
								}
							}
						} else {
							FileOutputStream fos = new FileOutputStream(newfile);	//if newfile exists it will be overwritten
							BufferedOutputStream out = new BufferedOutputStream(fos, 8192);
							byte[] buffer = new byte[8192];
							int n = 0;
							int tot = 0;
							while ((n = in.read(buffer)) >= 0) {
								out.write(buffer, 0, n);
								tot+=n;
							}
							out.flush();
							if(tot!=entry.getSize()){
								Log.println(Log.INFO, "\t LiturgiaOre", "\t Unzip "+tot+"B  but expected "+entry.getSize()+" in "+entry.getName());
							}
						}
						in.close();
					}
					Log.println(Log.INFO, "LiturgiaOre", "\t Estrazione di "+newfile.getName()+" completata.");
					newfile.delete();
					zipfile.close();
					dismissDialog(DIALOG_CODE.UNZIP);
					startActivity(new Intent(LiturgiaOre.this, LiturgiaOre.class));
					finish();
					Vibrator VV = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					VV.vibrate(500);
				} catch (FileNotFoundException e) {
					dismissDialog(DIALOG_CODE.UNZIP);
					Log.println(Log.ERROR, "LiturgiaOre.unzip", "Impossibile aprire il file da estrarre per la scrittura");
					e.printStackTrace();
				} catch (ZipException e) {
					dismissDialog(DIALOG_CODE.UNZIP);
					Log.println(Log.ERROR, "LiturgiaOre.unzip", "Errore durante la costruzione dello ZipFile");
					e.printStackTrace();
				} catch (IOException e) {
					dismissDialog(DIALOG_CODE.UNZIP);
					Log.println(Log.ERROR, "LiturgiaOre.unzip", "Errore durante l'apertura dell'InputStream sui dati dello ZipFile oppure Errore durante la scrittura write() sull'OutputStream");					
					e.printStackTrace();
				}
			}
		});
		t.start();
	}
	
	public String getEntryName(String name) {
		String str = "";
		if (name.contains("01-Home"))
			str = "Home";
		else if (name.contains("02-Liturgia"))
			str = "Liturgia";
		else if (name.contains("03-Messa"))
			str = "Liturgia delle ore";
		else if (name.contains("03-Salterio"))
			str = "Salterio";
		else if (name.contains("03-Breviarium"))
			str = "Breviario";
		else if (name.contains("04-Bibbia"))
			str = "Bibbia";
		else if (name.contains("05-Magist"))
			str = "Magistero Chiesa";
		else if (name.contains("06-Preghiere"))
			str = "Preghiere";
		else if (name.contains("07-Missale"))
			str = "Messale Romano";
		else
			str = "http://www.maranatha.it";
		return str;
	}

	public String getEntryUrl(String name) {
		String url = "";
		if (name.contains("01-Home"))
			url = DEBUG_URL+"01-HomePage.zip";
		else if (name.contains("02-Liturgia"))
			url = DEBUG_URL+"02-Liturgia.zip";
		else if (name.contains("03-Messa"))
			url = DEBUG_URL+"03-MessaLiturOre.zip";
		else if (name.contains("03-Salterio"))
			url = DEBUG_URL+"03-Salterio4sett.zip";
		else if (name.contains("03-Breviarium"))
			url = DEBUG_URL+"03-Breviarium.zip";
		else if (name.contains("04-Bibbia"))
			url = DEBUG_URL+"04-BibbiaCEI-74.zip";
		else if (name.contains("05-Magist"))
			url = DEBUG_URL+"05-MagistChiesa.zip";
		else if (name.contains("06-Preghiere"))
			url = DEBUG_URL+"06-Preghiere.zip";
		else if (name.contains("07-Missale"))
			url = DEBUG_URL+"07-MissaleVO.zip";
		else
			url = "http://www.maranatha.it";
		return url;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mExternalStorageReceiver);
	}
}