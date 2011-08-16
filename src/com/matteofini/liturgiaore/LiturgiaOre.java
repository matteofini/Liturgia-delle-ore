package com.matteofini.liturgiaore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.Semaphore;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

class FilterDirectory implements FilenameFilter {
	private String pattern;
	
	public FilterDirectory(String pattern) {
		this.pattern = pattern;
	}
	@Override
	public boolean accept(File dir, String filename) {
		if (dir.isDirectory() && filename.contains(pattern) && !filename.endsWith("zip"))
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

	private Thread t;
//	public final static String ACTION_HTML = "com.android.maranatha.HTML";
	private String path;
	private int _LAYOUT=0;
	private String[] modules = new String[]{"01-HomePage", "02-Liturgia", "03-MessaLiturOre", "03-Salterio4sett", "03-Breviarium", "04-BibbiaCEI-74", "05-MagistChiesa", "06-Preghiere", "07-MissaleVO"};	
	private Semaphore _Sdownload = new Semaphore(1);
	private Semaphore _Sunzip = new Semaphore(0);
	
	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        // check if SD is mounted
        String SDstr = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(SDstr)){
        	File rootdir = Environment.getExternalStorageDirectory();
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        	path = prefs.getString("filepath", "/sdcard/download/");

        	File filedir = new File(path);
        	View rl = getLayoutInflater().inflate(R.layout.main, null);
        	ScrollView sv = (ScrollView) rl.findViewById(R.id.ScrollView1);
        	TableLayout ll = (TableLayout) sv.getChildAt(0);
        	
        	if(!filedir.exists()){
    			AlertDialog.Builder d1 = new AlertDialog.Builder(LiturgiaOre.this);
				d1.setMessage("Cartella "+path+" inesistente. Impostare la cartella dove si trovano i file oppure " +
    					"fare il download dei file desiderati. Essi verranno salvati nella nuova cartella "+path);
				d1.setCancelable(true);
				d1.setPositiveButton("Continua", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
		               dialog.cancel();
		           }
				});
				d1.setNegativeButton("Impostazioni", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						startActivity(new Intent(LiturgiaOre.this, LiturgiaOrePreferences.class));
						finish();
					}

				});
				AlertDialog dial = d1.create();
				dial.show();
    		}
        	
        	/* for each module get the list of file/dir with its name
        	 * 	if the list is not empty then the module exist
        	 * 		read or unzip button are shown
        	 * 	otherwise download button is shown
        	 */
        	for(final String name : modules){
        		File[] match = new File[0];

        		if(filedir.exists()) 
        			match = filedir.listFiles(new FilterDirectory(name));
        		
        		TableRow row = new TableRow(LiturgiaOre.this);
        		row.setPadding(0, 5, 0, 5);
        		ll.addView(row);
        		
        		TextView v = (TextView) getLayoutInflater().inflate(R.layout.label, null);
        		v.setText(getEntryName(name));
				row.addView(v);
				
				String label = getEntryName(name);
				final String url = getEntryUrl(name);
				v.setText(label);
        		
				
				ImageButton b_down = (ImageButton) getLayoutInflater().inflate(R.layout.download_button, null);
				row.addView(b_down);
				b_down.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						// download						
						download(name, url);	// start new thread
						File filezip = new File(path+"/"+name+".zip");
						unzip(filezip.getAbsolutePath(), path);	// start new thread	
					}
				});
					
        		if(match.length==0){
        			b_down.setImageResource(R.drawable.download);
        		}
        		else{
        			b_down.setImageResource(R.drawable.refresh);				
					final File file = match[0];
					View b = getLayoutInflater().inflate(R.layout.read_button, null);
					row.addView(b);
					b.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							String path = file.getAbsolutePath();
							Intent i = new Intent();
					        i.setAction(Intent.ACTION_VIEW);
					        i.addCategory(Intent.CATEGORY_BROWSABLE);
					        if(!new File(file.getAbsoluteFile()+"/00-ENTRA.htm").exists()){
					        	String[] split = file.getAbsolutePath().split("/");
					        	path = path+"/"+split[split.length-1];
					        }
					        i.setType("text/html");
					        i.setData(Uri.parse("file://"+path+"/00-ENTRA.htm"));
					        //i.setComponent(new ComponentName("com.android.htmlviewer", "com.android.htmlviewer.HTMLViewerActivity"));
					        //startActivity(i);
					        sendBroadcast(i);
						}
					});	
        		}
        			
        	}
        	setContentView(rl);
        }
        else{
        	Dialog dialog = new Dialog(this); 	
        	dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();	
				}
			});
        	TextView dialog_view = new TextView(this);
        	dialog_view.setPadding(10, 0, 10, 10);
        	dialog_view.setText("Scheda sd non montata. Hai il telefono collegato al pc? \nDisattiva la funzione \"Archivio USB\" dal telefono o \"espelli\" la scheda di memoria montata da Esplora Risorse.\nTOCCA PER USCIRE.");
        	dialog.setTitle("Notifica di errore");
        	
        	dialog.setCanceledOnTouchOutside(true);
        	dialog.setCancelable(true);
        	dialog.setContentView(dialog_view);
        	dialog.show();
        }
    }

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.option, menu);
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				startActivity(new Intent(LiturgiaOre.this,
						LiturgiaOrePreferences.class));
				finish();
				return true;
			}
		});
		menu.getItem(2).setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
					showDialog(1);
				return true;
			}
		});
		menu.getItem(1).setOnMenuItemClickListener(new OnMenuItemClickListener() {			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent i = new Intent();
				i.setAction(Intent.ACTION_MAIN);
				i.addCategory(Intent.CATEGORY_LAUNCHER);
				i.setComponent(new ComponentName("com.lindaandny.lindamanager", "com.lindaandny.lindamanager.LindaManager"));
				startActivity(i);
				return true;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	
	protected Dialog onCreateDialog(int id, Bundle args) {
		if(id==0){
			ProgressDialog pd = new ProgressDialog(LiturgiaOre.this);
			pd.setTitle("Unzip "+args.getString("name"));
			pd.setMessage("estrazione dei file in corso\nin "+args.getString("path"));
			pd.setSecondaryProgress(0);
			return pd;
		}
		else if(id==1 || id==2){
			AlertDialog.Builder d = new AlertDialog.Builder(LiturgiaOre.this);
			d.setTitle("Aiuto");
			d.setMessage(R.string.help1);
			d.setCancelable(true);
			d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
	               dialog.cancel();
	           }
			});
			AlertDialog dial = d.create();
			return dial;
		}
		else if(id==3){
			ProgressDialog pd = new ProgressDialog(LiturgiaOre.this);
			pd.setTitle("Download "+args.getString("name"));
			pd.setMessage("salvataggio in "+args.getString("path"));
			pd.setSecondaryProgress(0);
			return pd;
		}
		else if(id==4){
			AlertDialog.Builder d = new AlertDialog.Builder(LiturgiaOre.this);
			d.setMessage("Connessione non disponibile. Attivare la Connessione WIFI o 3G per scaricare il file.");
			d.setCancelable(true);
			d.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
	               dialog.cancel();
	               dismissDialog(3);	// dismiss download dialog 
	           }
			});
			AlertDialog dial = d.create();
			return dial;
		}
		else
			return super.onCreateDialog(id);
	}

	
	public String getEntryName(String name){
		String str = "";
		if(name.contains("01-Home"))
			str = "Home";
		else if(name.contains("02-Liturgia"))
			str = "Liturgia";
		else if(name.contains("03-Messa"))
			str = "Liturgia delle ore";
		else if(name.contains("03-Salterio"))
			str = "Salterio";
		else if(name.contains("03-Breviarium"))
				str = "Breviario";
		else if(name.contains("04-Bibbia"))
			str = "Bibbia";
		else if(name.contains("05-Magist"))
			str = "Magistero Chiesa";
		else if(name.contains("06-Preghiere"))
			str = "Preghiere";
		else if(name.contains("07-Missale"))
			str = "Messale Romano";
		else
			str = "http://www.maranatha.it";
		return str;
	}
	
	public String getEntryUrl(String name){
		String url = "";
		if(name.contains("01-Home"))
			url = "http://www.maranatha.it/Dwl-Edition/01-HomePage.zip";
		else if(name.contains("02-Liturgia"))
			url = "http://www.maranatha.it/Dwl-Edition/02-Liturgia.zip";
		else if(name.contains("03-Messa"))
			url = "http://www.maranatha.it/Dwl-Edition/03-MessaLiturOre.zip";
		else if(name.contains("03-Salterio"))
			url = "http://www.maranatha.it/Dwl-Edition/03-Salterio4sett.zip";
		else if(name.contains("03-Breviarium"))
			url = "http://www.maranatha.it/Dwl-Edition/03-Breviarium.zip";
		else if(name.contains("04-Bibbia"))
			url = "http://www.maranatha.it/Dwl-Edition/04-BibbiaCEI-74.zip";
		else if(name.contains("05-Magist"))
			url = "http://www.maranatha.it/Dwl-Edition/05-MagistChiesa.zip";
		else if(name.contains("06-Preghiere"))
			url = "http://www.maranatha.it/Dwl-Edition/06-Preghiere.zip";
		else if(name.contains("07-Missale"))
			url = "http://www.maranatha.it/Dwl-Edition/07-MissaleVO.zip";
		else
			url = "http://www.maranatha.it";
		return url;
	}

	Handler h = new Handler() {
		public void handleMessage(Message msg) {
			if(msg.arg2==0){
			    removeDialog(msg.arg1); 
				startActivity(new Intent(LiturgiaOre.this, LiturgiaOre.class));
				finish();
			}
			else if(msg.arg2==1){
				showDialog(msg.arg1, msg.getData());
			}
		}
	};

	public void unzip(final String filename, final String path) {
		t = new UnzipThread(h, new Runnable() {
			@Override
			public void run() {
				try {
					_Sunzip.acquire();
					File file = new File(filename);
					if(!file.exists()){
						Toast.makeText(getApplicationContext(), "File "+file.getAbsolutePath()+" inesistente. Errore durante il download", Toast.LENGTH_LONG).show();
						return;
					}
					Bundle b = new Bundle();
					b.putString("name", filename);
					b.putString("path", path);
					Message m1 = new Message();
					m1.arg1 = 0;	// id dialog
					m1.arg2 = 1; // show
					m1.setData(b);
					h.sendMessage(m1);
					
					
					ZipFile f = new ZipFile(file);
					Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) f.entries();

					Log.println(Log.INFO, "Unzip", "extracting "+ file.getName() + " into " + "/mnt" + path);
					while (entries.hasMoreElements()) {
						ZipEntry entry = (ZipEntry) entries.nextElement();
						// System.out.println("unzip "+entry.getName()+" into "+"/mtn"+path);
						BufferedInputStream in = new BufferedInputStream(f.getInputStream(entry), 8192);
						File newfile = new File("/mnt"+path+"/"+entry.getName());
						if (entry.isDirectory()) {
							boolean res = newfile.mkdir();
						} 
						else {
							// System.out.println("\t"+path+entry.getName());
							BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newfile), 8192);
							byte[] buffer = new byte[8192];
							int n = 0;
							while ((n = in.read(buffer)) >= 0) {
								out.write(buffer, 0, n);
							}
							out.flush();
						}
						in.close();
					}
					file.delete();
					f.close();
					Message m = new Message();
					m.arg1 = 0;	// id dialog
					m.arg2 = 0; // dismiss
					h.sendMessage(m);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (ZipException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				_Sunzip.release();
			}
		});
		t.start();
	}
	
	public void download(final String name, final String url){
		t = new DownloadThread(h, new Runnable(){
			@Override
			public void run() {
				Log.println(Log.INFO, "LiturgiaOre", "download "+url+" in "+path);
				try{
					_Sdownload.acquire();
					Bundle b = new Bundle();
					b.putString("name", name);
					b.putString("path", path);
					Message m1 = new Message();
					m1.arg1 = 3;	// id dialog
					m1.arg2 = 1; // show
					m1.setData(b);
					h.sendMessage(m1);
//					showDialog(3, b);
					
					URL href_url = new URL(url);
					BufferedInputStream in = new BufferedInputStream(href_url.openStream(), 8192);
					File dir = new File(path);
					if(!dir.exists())
						dir.mkdirs();
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(path+"/"+name+".zip")), 8192);

					byte[] buffer = new byte[8192];
					int n = 0;
					while ((n = in.read(buffer)) >= 0) {
						out.write(buffer, 0, n);
					}
					out.flush();
					in.close();
					
					Message m = new Message();
					m.arg1 = 3;	// id dialog
					m.arg2 = 0; // dismiss
					h.sendMessage(m);
				} 
				catch(IOException e){
					/* show alert dialog when connection is not available */
					if(e.getClass()==UnknownHostException.class){
						Message m = new Message();
						m.arg1 = 4;	// id dialog
						m.arg2 = 1; // show
						h.sendMessage(m);
					}	
					else e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				_Sdownload.release();
				_Sunzip.release();
			}
		});
		t.start();
	}
}