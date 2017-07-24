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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.example.testbluetooth.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mandl
 */
public class MainActivity extends Activity {

    final String LOG_TAG = "MainActivity";
    TextView myView;
    private BluetoothTempService s;
    private ListView lv;

    private List<TempStation> list = new ArrayList<TempStation>();

    private ArrayAdapterItem adapter;
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            BluetoothTempService.MyBinder b = (BluetoothTempService.MyBinder) binder;
            s = b.getService();

        }

        public void onServiceDisconnected(ComponentName className) {
            s = null;
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle bundle = intent.getExtras();
            if (bundle != null) {

                if (s != null) {
                    list.clear();
                    list.addAll(s.getTempStationList());
                    adapter.notifyDataSetChanged();
                } else {
                    Log.d(LOG_TAG, "s == null");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_main);

        myView = findViewById(R.id.textView1);

        // We get the ListView component from the layout
        lv = findViewById(R.id.listView1);

        // our adapter instance
        adapter = new ArrayAdapterItem(this, list);

        lv.setAdapter(adapter);

        Intent i = new Intent(getBaseContext(), BluetoothTempService.class);

        i.setAction(BluetoothTempService.ACTION_START_TEMP_PROVIDER);

        startService(i);

        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                TempStation a = (TempStation) parent.getAdapter().getItem(
                        position);
                a.getId();

            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume");
        registerReceiver(receiver, new IntentFilter(
                BluetoothTempService.INFORM_APP));
        Intent intent = new Intent(this, BluetoothTempService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // update list
        if (s != null) {
            list.clear();
            list.addAll(s.getTempStationList());
            adapter.notifyDataSetChanged();
        } else {
            Log.d(LOG_TAG, "s == null");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
        unregisterReceiver(receiver);
        unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                Intent i = new Intent(this, UserSettingActivity.class);
                startActivity(i);
                break;
        }
        return true;
    }

}
