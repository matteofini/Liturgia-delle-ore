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
