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

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class HTMLViewerActivity extends Activity{

	private myWebView wv;
	Thread t_autoscroll = null;
	//private TextToSpeech mTts;
	//private ReentrantLock TTSlock;

	/*	public void onInit(int status) {
    	if(TextToSpeech.SUCCESS==status){
    		int res = mTts.setLanguage(Locale.ITALY);
    		if(res==TextToSpeech.LANG_NOT_SUPPORTED || res==TextToSpeech.LANG_MISSING_DATA){
    			Log.println(Log.ERROR, "Maranatha.OnInit", "TTS Locale Language error. Not supported or missing data.");
    		}
    		TTSlock.unlock();	// waiting thread will be able to acquire the lock (and just unlock for the last time) #81 
    	}
    	else{
    		Log.println(Log.ERROR, "Maranatha.OnInit", "TTS status not SUCCESS");
    	}
    }	*/	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}	
	
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
				wv.loadUrl("javascript:(function() {"+buf+"})()");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			// view.loadUrl("javascript:(function(){loaderScript=document.createElement('SCRIPT');loaderScript.type='text/javascript';loaderScript.src='content://com.ideal.webaccess.localjs/ideal-loader.js';document.getElementsByTagName('head')[0].appendChild(loaderScript);})();");
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
        //wv = new WebView(HTMLViewerActivity.this);
        //v.addView(wv);
		wv = (myWebView) v.findViewById(R.id.myWebView1);
		wv.setFocusable(true);
		v.findViewById(R.id.scrollbutton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				t_autoscroll = new Thread(new Runnable() {
					@Override
					public void run() {
						while(wv.getScrollY()<wv.getContentHeight()){
							runOnUiThread(new Runnable() {
								public void run() {
									wv.scrollBy(0, 1);
								}
							});
							
							SystemClock.sleep(50);
						}
					}
				});
				t_autoscroll.start();
			}
		});
		wv.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if(t_autoscroll!=null)
					h.sendEmptyMessage(800);
				return false;
			}
		});
		
		setContentView(v);
/*		mTts = new TextToSpeech(this, this);
		TTSlock = new ReentrantLock();
		TTSlock.lock();	*/

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
	}
	
	Handler h = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			
			return false;
		}
	});
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.webmenu, menu);
		menu.getItem(0).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						while(wv.getScrollY()<wv.getContentHeight()){
							Message m = h.obtainMessage();
							runOnUiThread(new Runnable() {
								public void run() {
									wv.scrollBy(0, 10);
								}
							});
							
							SystemClock.sleep(100);
						}
					}
				}).start();
				return true;
			}
		});
		return true;
	}
}
