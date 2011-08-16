package com.matteofini.liturgiaore;

import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HTMLViewerActivity extends Activity{

	private WebView wv;
	private TextToSpeech mTts;
	private ReentrantLock TTSlock;
/*	
	public void onInit(int status) {
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
    }
*/	
	public class MyWebClient extends WebViewClient{

/*
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			//Log.println(Log.INFO, "MyWebClient", "\t\t onPageStarted "+url);
			super.onPageStarted(view, url, favicon);
			mTts.stop();
		}
*/
/*		
		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			if(url.endsWith("htm")||url.endsWith("html")){
				File file = new File(url);
				try {
					String str="";
					url = url.substring(6);
					FileInputStream is = new FileInputStream(url);
					BufferedInputStream buf = new BufferedInputStream(is);
					DataInputStream dis = new DataInputStream(buf);
					while(dis.available()!=0){
						str+=dis.readLine();
					}
						//	Log.println(Log.INFO, "MyWebClient", "\t "+str);
						//	>([\t\s\r\n]*[a-zA-Z0-9.\-/]+[\t\s\r\n]*)+
					String re = ">([\\t\\s\\r\\n]*[a-zA-Z0-9,'!?*^:;.\\-/]+[\\t\\s\\r\\n]*)+";
					jregex.Pattern p = new jregex.Pattern(re);
					jregex.Matcher m = p.matcher(str);
					
					jregex.MatchIterator m_it = m.findAll();
					while(m_it.hasMore()){
						jregex.MatchResult res = (jregex.MatchResult) m_it.nextMatch();
						String match = res.toString().substring(1);
						match = match.replaceAll("[ ]+", " ");
							//	System.out.println(match);
						if(TTSlock.isLocked()){
							TTSlock.tryLock(100, TimeUnit.MILLISECONDS);	// throws InterruptedException
							TTSlock.unlock();	// never locked again
						}
						mTts.speak(match, TextToSpeech.QUEUE_ADD, null);
					}	
				} catch (FactoryConfigurationError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
*/		
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
		
		this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		wv = new WebView(this);
		setContentView(wv);
//		mTts = new TextToSpeech(this, this);
//		TTSlock = new ReentrantLock();
//		TTSlock.lock();

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

	}
}
