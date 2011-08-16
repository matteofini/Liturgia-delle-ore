package com.matteofini.liturgiaore;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class HTMLfileReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Intent i = new Intent();
			//Log.println(Log.INFO, "HTMLfileReceiver", "\t"+arg1.getDataString());
			//Log.println(Log.INFO, "HTMLfileReceiver", "\t"+arg1.getAction());
			//Log.println(Log.INFO, "HTMLfileReceiver", "\t"+arg1.getType());
		i.setAction(Intent.ACTION_VIEW);
		i.setData(Uri.parse(arg1.getDataString()));
		i.setComponent(new ComponentName("com.matteofini.liturgiaore", "com.matteofini.liturgiaore.HTMLViewerActivity"));
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		arg0.startActivity(i);
	}

}
