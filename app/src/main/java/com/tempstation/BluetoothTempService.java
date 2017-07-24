/*
    Tempstation
    
    Copyright (C) 2016 Mandl
    Copyright (C) 2010, 2011, 2012 Herbert von Broeuschmeul
    Copyright (C) 2010, 2011, 2012 BluetoothGPS4Droid Project

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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Config;
import android.util.Log;
import android.util.SparseArray;

import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothTempService extends Service implements OnSharedPreferenceChangeListener {

    public static final String ACTION_START_TEMP_PROVIDER = "action.START_TEMP_PROVIDER";
    public static final String ACTION_DATA = "mandl.new.TEMP";
    public static final String INFORM_APP = "mandl.inform.APP";
    public static final String TEMP_SMS = "mandl.temp.sms";
    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BluetoothTempService";
    private final IBinder mBinder = new MyBinder();
    int maxConRetries = 100000;
    private PowerManager.WakeLock wl;
    private boolean locklist;
    private SparseArray<String> idmap = new SparseArray<String>();
    private CopyOnWriteArrayList<TempStation> list = new CopyOnWriteArrayList<TempStation>();
    private BlueetoothArduino gpsManager = null;

    private com.tempstation.SmsReceiver mSMSreceiver;
    private String smsstr = "";
    private String smsReceiverNumber;

    public CopyOnWriteArrayList<TempStation> getTempStationList() {
        return list;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TempService");
        wl.acquire();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String a = sharedPreferences.getString("out", "0");
        String b = sharedPreferences.getString("in", "1");
        String wo = sharedPreferences.getString("wo", "3");
        String mobile = sharedPreferences.getString("mobile", "4");
        String roof = sharedPreferences.getString("roof", "5");

        smsReceiverNumber = sharedPreferences.getString("smsReceiverNumber", "");

        if (list.size() == 0) {
            // List is empty. Do not lock
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("prefLockStation", false);
            editor.commit();
        }

        locklist = sharedPreferences.getBoolean("prefLockStation", false);

        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        int a1 = Integer.valueOf(a);
        int b1 = Integer.valueOf(b);
        int wo1 = Integer.valueOf(wo);
        int mobile1 = Integer.valueOf(mobile);
        int roof1 = Integer.valueOf(roof);


        idmap.put(a1, "Draussen");
        idmap.put(b1, "Stube");
        idmap.put(wo1, "Wohnen");
        idmap.put(mobile1, "Schlafen");
        idmap.put(roof1, "Dach");


        // SMS event receiver
        mSMSreceiver = new SmsReceiver();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        // mIntentFilter.setPriority(2147483646);
        registerReceiver(mSMSreceiver, mIntentFilter);

    }

    @Override
    public void onStart(Intent intent, int startId) {

        Log.d(LOG_TAG, "onStart()");
        // super.onStart(intent, startId);

        // String deviceAddress =
        // sharedPreferences.getString(PREF_BLUETOOTH_DEVICE, null);
        // String deviceAddress ="00:0D:B5:82:22:44";
        String deviceAddress = "00:13:EF:01:11:7A";

        Log.d(LOG_TAG, "prefs device addr: " + deviceAddress);
        if (ACTION_START_TEMP_PROVIDER.equals(intent.getAction())) {
            if (gpsManager == null) {
                if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {

                    gpsManager = new BlueetoothArduino(this, deviceAddress, maxConRetries);
                    boolean enabled = false;

                    try {
                        enabled = gpsManager.enable();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    if (enabled) {
                        Log.d(LOG_TAG, "gpsManager enable");

                    } else {
                        Log.e(LOG_TAG, "gpsManager not enabled");
                        stopSelf();
                    }
                } else {

                    Log.e(LOG_TAG, "checkBluetoothAddress fail");
                    stopSelf();
                }
            }
        } else {

            Log.e(LOG_TAG, "Action unknown");
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand()");
        onStart(intent, startId);
        if (intent != null) {
            doAction(intent);
        }
        return Service.START_NOT_STICKY;
    }

    private void doAction(Intent intent) {
        Log.d(LOG_TAG, "doAction()");
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(TEMP_SMS)) {
                SMSStatus();
            }
        }
    }

    private void SMSStatus() {

        smsstr = "";
        if (!list.isEmpty()) {
            for (TempStation station : list) {
                smsstr += "( ID: " + station.getTemp() + " " + station.getLocation() + " " + station.getDate() + ")   ";

            }
            SendSMS(smsstr);
        } else {
            SendSMS("Keine Temperaturdaten");
        }
    }

    private void SendSMS(String text) {
        SmsManager manager = SmsManager.getDefault();
        String Number;
        boolean simualtionMode = false;
        if (simualtionMode) {
            Number = "5556";
        } else {
            Number = smsReceiverNumber;

        }
        Log.d(LOG_TAG, "Sende sms: " + text);
        manager.sendTextMessage(Number, null, text, null, null);
    }

    @Override
    public void onDestroy() {

        Log.d(LOG_TAG, "onDestroy");
        if (wl != null) {
            wl.release();
        }
        BlueetoothArduino manager = gpsManager;
        gpsManager = null;
        if (manager != null) {
            manager.disable();
        }
        super.onDestroy();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        if (Config.LOGD) {
            Log.d(LOG_TAG, "trying access IBinder");
        }
        return mBinder;
    }

    private void ClearMinMaxStation() {
        for (TempStation item : list) {

            item.ClearMinMax();
        }

    }

    private boolean searchItemStation(int id) {
        for (TempStation item : list) {
            if (item.getId() == id) {
                return true;
            }

        }
        return false;
    }

    private TempStation getStation(int id) {
        for (TempStation item : list) {
            if (item.getId() == id) {
                return item;
            }

        }
        return null;
    }

    private void RemoveOldStations() {

        Date current = new Date();

        for (TempStation item : list) {

            if (item.CheckOld(current)) {
                Log.d(LOG_TAG, "Remove station : " + item.getId() + " " + item.getLocation());
                list.remove(item);
            }

        }
    }

    public void publishUpdate(String data) {

        int ID;
        int Reset;
        int lowBat;

        Intent intent = new Intent(INFORM_APP);
        intent.putExtra(ACTION_DATA, data);

        String delims = "[,]";
        String[] tokens = data.split(delims);

        if (tokens.length == 6) {
            Date myDate;

            if (tokens[0].equals("frame")) try {

                myDate = new Date();

                ID = Integer.parseInt(tokens[1]);
                Reset = Integer.parseInt(tokens[3]);
                lowBat = Integer.parseInt(tokens[4]);

                if (!searchItemStation(ID)) {
                    if (!locklist) {

                        String location = idmap.get(ID);

                        list.add(new TempStation(ID, tokens[2], myDate, Reset, lowBat, location, tokens[5]));
                    }

                } else {
                    TempStation my;
                    if ((my = getStation(ID)) != null) {
                        my.setTemp(tokens[2]);
                        my.setDate(myDate);
                        my.setHygro(tokens[5]);
                        if (idmap.get(ID) != null)
                            my.setLocation(idmap.get(ID));
                    }
                }

            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            RemoveOldStations();
            sendBroadcast(intent);
        }
    }

	/*
     *
	 * frame,33,18.5,1,0,60
	 * 
	 * ID = 33 Temp = 18.5 Reset = 1 lowBat = 0 Hygro=60
	 */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        switch (key) {
            case "out":
            case "wo":
            case "mobile":
            case "in":
            case "roof":

                String a = sharedPreferences.getString("out", "0");
                String b = sharedPreferences.getString("in", "1");
                String wo = sharedPreferences.getString("wo", "3");
                String mobile = sharedPreferences.getString("mobile", "4");
                String roof = sharedPreferences.getString("roof", "5");


                int a1 = Integer.valueOf(a);
                int b1 = Integer.valueOf(b);
                int wo1 = Integer.valueOf(wo);
                int mobile1 = Integer.valueOf(mobile);
                int roof1 = Integer.valueOf(roof);

                idmap.clear();
                idmap.put(a1, "Draussen");
                idmap.put(b1, "Stube");
                idmap.put(wo1, "Wohnen");
                idmap.put(mobile1, "Schlafen");
                idmap.put(roof1, "Dach");

                break;
            case "reset":

                list.clear();
                break;
            case "resetMinMax":

                ClearMinMaxStation();

                break;
            case "prefLockStation":

                locklist = sharedPreferences.getBoolean("prefLockStation", false);

                break;
        }
    }

    public class MyBinder extends Binder {
        BluetoothTempService getService() {
            return BluetoothTempService.this;
        }
    }
}
