package com.matteofini.liturgiaore;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class myWebView extends WebView {
	
	
public myWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
/*
		
*/
	public int VerticalScrollExtent;
	public int VerticalScrollOffset;
	public int VerticalScrollRange;

	@Override
	public int computeVerticalScrollExtent() {
		return super.computeVerticalScrollExtent();
	}
	@Override
	public int computeVerticalScrollOffset() {
		return super.computeVerticalScrollOffset();
	}
	@Override
	public int computeVerticalScrollRange() {
		return super.computeVerticalScrollRange();
	}
	
}
