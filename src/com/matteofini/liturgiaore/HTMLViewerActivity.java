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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class HTMLViewerActivity extends Activity{

	private myWebView wv;
	public boolean stopscroll=false;
	public int step=1;
	public int interval=100;
	
	private Handler h_autoscroll;
	private Thread t_autoscroll =  new Thread(new Runnable() {
		@Override
		public void run() {	
			Looper.prepare();
			h_autoscroll = new Handler(new Callback() {
				@Override
				public boolean handleMessage(Message msg) {
					// TODO Auto-generated method stub
					return false;
				}
			});
			Looper.loop();
		}
	}, "thread_autoscroll");
	
	public class MyWebClient extends WebViewClient{
		@Override
		public void onLoadResource(WebView view, String url) {
			//Log.println(Log.INFO, "MyWebClient", "\t onLoadResource "+url);
			super.onLoadResource(view, url);
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			// open link in a new window
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.addCategory(Intent.CATEGORY_BROWSABLE);
			i.setType("text/html");
			i.setData(Uri.parse(url));
			sendBroadcast(i);
			return true;
		}	
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			// view.loadUrl("javascript:(function(){loaderScript=document.createElement('SCRIPT');loaderScript.type='text/javascript';loaderScript.src='content://com.ideal.webaccess.localjs/ideal-loader.js';document.getElementsByTagName('head')[0].appendChild(loaderScript);})();");
			try {
				InputStream is = getResources().getAssets().open("style.js");
				StringBuffer buf = new StringBuffer();
				int c;
				int n = 0;
				while ((c = is.read()) > 0) {
					buf.append((char) c);
					n++;
				}
				//String s = new String(buf);
				//System.out.println(buf);
				DisplayMetrics display = getResources().getDisplayMetrics();
				wv.loadUrl("javascript:(function(displayWidth) {" +
						"var all_table = document.getElementsByTagName('table');\n" + 
						"for(i=0;i<all_table.length;i++){" + 
						"	all_table[i].style.width='100%';" +
						"	if(displayWidth>=800){" +
						"		all_table[i].style.fontSize='xx-large';" +
						"	}" +
						"	else if(displayWidth>=600){" +
						"		all_table[i].style.fontSize='x-large';" +
						"	}" +
						"}" +
						"})("+display.widthPixels+")");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public class MyWebChromeClient extends WebChromeClient{
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			setTitle("loading...");
			setProgress(newProgress*100);
			if(newProgress == 100)
                setTitle(R.string.app_name);
		}
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		getWindow().requestFeature(Window.FEATURE_PROGRESS);		
        getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        RelativeLayout v = (RelativeLayout) getLayoutInflater().inflate(R.layout.htmlvieweractivity, null);
		wv = (myWebView) v.findViewById(R.id.myWebView1);
		wv.setFocusable(true);
		t_autoscroll.start();
		
        WebSettings s = wv.getSettings();
		s.setBuiltInZoomControls(true);
		s.setSupportMultipleWindows(true);
		s.setJavaScriptEnabled(true);
		wv.setWebViewClient(new MyWebClient());	// catch webpage loading (loadUrl function call)
		wv.setWebChromeClient(new MyWebChromeClient());
		
		Intent i = getIntent();
		String uri = i.getDataString();
			Log.println(Log.INFO, "HTMLViewerActivity", "\t load URI "+uri);
		wv.loadUrl(uri);
		wv.getContentDescription();
		setContentView(v);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.webmenu, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);
		if(item.getTitleCondensed().equals("Start")){
			stopscroll=false;
			h_autoscroll.post(new Runnable() {
				@Override
				public void run() {
					stopscroll=false;
					while(wv.getScrollY()<wv.getContentHeight()-wv.computeVerticalScrollExtent()){
						if(stopscroll)
							break;
						else
							runOnUiThread(new Runnable() {
								public void run() {
									wv.scrollBy(0, step);
								}
							});
						SystemClock.sleep(interval);
					}
				}
			});
		}
		else if(item.getTitleCondensed().equals("Stop")){
			stopscroll=true;
		}
		else if(item.getTitleCondensed().equals("Veloce")){
			if(step==1&&interval==100)
				interval=50;
			else
				step*=2;
		}
		else if(item.getTitleCondensed().equals("Lento")){
			if(step==1&&interval==50)
				interval=100;
			else{
				int c=step/2;
				if(c<=0) step=1;
				else step=c;
			}
		}
		else if(item.getTitleCondensed().equals("Ricarica")){
			wv.reload();
		}
		return true;
	}
}
