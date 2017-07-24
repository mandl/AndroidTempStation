/*
    Tempstation
    
    Copyright (C) 2016 Mandl

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tempstation;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    final String LOG_TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // ---get the SMS message passed in---
        Log.d(LOG_TAG, "onReceive");
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        String str = "";
        String mystr = "";
        if (bundle != null) {
            // ---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                str += "SMS from " + msgs[i].getOriginatingAddress();
                str += " :";
                str += msgs[i].getMessageBody().toString();
                mystr += msgs[i].getMessageBody().toString();
                mystr = mystr.trim();
                mystr = mystr.toLowerCase();

                if (mystr.equals("temp") || mystr.equals("temperature")) {
                    Intent a = new Intent(context, BluetoothTempService.class);
                    a.setAction(BluetoothTempService.TEMP_SMS);
                    context.startService(a);
                    this.abortBroadcast();
                    Log.d(LOG_TAG, "got Temp. SMS");

                }
                str += "\n";
            }

        }

    }
}