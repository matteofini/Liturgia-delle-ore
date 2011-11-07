package com.matteofini.liturgiaore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.app.IntentService;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import com.matteofini.liturgiaore.LiturgiaOreAbstr.DIALOG_CODE;

public class DownloadService extends IntentService {

	public DownloadService() {
		super("DownloadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		download_nothread(intent.getStringExtra("mName"), intent.getStringExtra("url"), intent.getStringExtra("path"));
	}
		
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}
	
	
	
	private void download_nothread(final String name, final String url, final String path){
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
				Intent i = new Intent("com.matteofini.liturgiaore.ACTION_SHOWDIALOG");
				i.putExtra("dialog", DIALOG_CODE.ERROR_FILE_SIZE);
				sendBroadcast(i);
			}
			else
				Log.println(Log.INFO, "LiturgiaOre", "\t downloaded "+downloaded+"B");
			out.flush();
			in.close();
			
			File newfile = new File(path+"/"+name+".zip");
			Intent i = new Intent(LiturgiaOreAbstr.ACTION_SHOWDIALOG);
			i.putExtra("dialog", DIALOG_CODE.DOWNLOAD);
			sendBroadcast(i);
			//sendBroadcast(new Intent("com.matteofini.liturgiaore.ACTION_DOWNLOAD_END"));
			if(newfile.exists()){
				Vibrator VV = (Vibrator) getSystemService(VIBRATOR_SERVICE);
				VV.vibrate(500);
				unzip_nothread(newfile, path);
			}
			else
				Log.println(Log.WARN, "LiturgiaOre", "\t An error has occurred during download. File "+name+" not created.");
		} catch (IOException e) {
			if (e.getClass() == UnknownHostException.class) {
				Intent i = new Intent("com.matteofini.liturgiaore.ACTION_SHOWDIALOG");
				i.putExtra("dialog", DIALOG_CODE.ERROR_CONNECTION);
				sendBroadcast(i);
				//sendBroadcast(new Intent("com.matteofini.liturgiaore.ACTION_ERROR_CONNECTION"));
			} 
			else if(e.getClass() == FileNotFoundException.class){
				Log.println(Log.WARN, "LiturgiaOre", "\t file all'indirizzo "+url+" non trovato");
				Intent i = new Intent("com.matteofini.liturgiaore.ACTION_SHOWDIALOG");
				i.putExtra("dialog", DIALOG_CODE.ERROR_FILE_NOT_FOUND);
				sendBroadcast(i);
				//sendBroadcast(new Intent("com.matteofini.liturgiaore.ACTION_ERROR_FILE_NOT_FOUND"));
			} 
			else if(e.getClass()== SocketTimeoutException.class){
				Log.println(Log.WARN, "LiturgiaOre", "\t Timeout della connessione. Non è stato possibile contattare "+url);
				Intent i = new Intent("com.matteofini.liturgiaore.ACTION_SHOWDIALOG");
				i.putExtra("dialog", DIALOG_CODE.CONNECTION_TIMEOUT);
				sendBroadcast(i);
				//sendBroadcast(new Intent("com.matteofini.liturgiaore.ACTION_ERROR_CONNECTION_TIMEOUT"));
			}
			else
				e.printStackTrace();
		}
	}
	
	protected void unzip_nothread(final File newfile, final String path){
		try {
			ZipFile zipfile = new ZipFile(newfile);
			Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipfile.entries();

			Log.println(Log.INFO, "LiturgiaOre", "\t Extracting "+newfile.getName()+" into "+path);
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				BufferedInputStream in = new BufferedInputStream(zipfile.getInputStream(entry), 8192);
				File newfile2 = new File(path + "/" + entry.getName());
				if (entry.isDirectory()) {
					if (!newfile2.mkdirs()) {
						if(!newfile2.exists()){	// mkdirs() returns false if newfile already exists
							Log.println(Log.INFO, "\t LiturgiaOre.unzip()", "\t directory"+entry.getName()+" NON creata");
						}
					}
				} else {
					FileOutputStream fos = new FileOutputStream(newfile2);	//if newfile exists it will be overwritten
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
			Intent i = new Intent("com.matteofini.liturgiaore.ACTION_SHOWDIALOG");
			i.putExtra("dialog", DIALOG_CODE.UNZIP);
			sendBroadcast(i);
			//sendBroadcast(new Intent("com.matteofini.liturgiaore.ACTION_UNZIP_END"));
			//startActivity(new Intent(getApplicationContext(), LiturgiaOre.class));
			//finish();
			Vibrator VV = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			VV.vibrate(500);
		} catch (FileNotFoundException e) {
			Log.println(Log.ERROR, "LiturgiaOre.unzip", "Impossibile aprire il file da estrarre per la scrittura");
			e.printStackTrace();
		} catch (ZipException e) {
			Log.println(Log.ERROR, "LiturgiaOre.unzip", "Errore durante la costruzione dello ZipFile");
			e.printStackTrace();
		} catch (IOException e) {
			Log.println(Log.ERROR, "LiturgiaOre.unzip", "Errore durante l'apertura dell'InputStream sui dati dello ZipFile oppure Errore durante la scrittura write() sull'OutputStream");					
			e.printStackTrace();
		}
	}
}
