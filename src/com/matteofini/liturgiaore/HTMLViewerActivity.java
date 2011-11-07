package com.matteofini.liturgiaore;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

public class HTMLViewerActivity extends Activity{

	private WebView wv;
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
		
		/*@Override
		public void onPageFinished(WebView view, String url) {
			//view.loadUrl("javascript:(document.style.background='#000000';)()");  
			view.loadUrl("javascript:(function(){loaderScript=document.createElement('SCRIPT');loaderScript.type='text/javascript';loaderScript.src='content://com.ideal.webaccess.localjs/ideal-loader.js';document.getElementsByTagName('head')[0].appendChild(loaderScript);})();");  
		}*/
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
		wv = (WebView) v.findViewById(R.id.webView1);
		wv.setFocusable(true);
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
}
