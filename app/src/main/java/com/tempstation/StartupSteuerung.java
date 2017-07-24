/*
This file is part of GPSLocater.

    GPSLocater is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GPSLocater is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.tempstation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupSteuerung extends BroadcastReceiver {
    private final static String TAG_STEUERUNG = "StartupTemp";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG_STEUERUNG, "onReceive");
        Intent startActivity = new Intent(context, MainActivity.class);
        startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity.putExtra("immediate", true);
        context.startActivity(startActivity);

    }

}
