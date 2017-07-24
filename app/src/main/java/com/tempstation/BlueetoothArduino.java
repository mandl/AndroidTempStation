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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.location.GpsStatus.NmeaListener;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BlueetoothArduino {

    /**
     * Tag used for log messages
     */
    private static final String LOG_TAG = "BlueArduino";
    private boolean FIX_BUG = true;
    private Service callingService;
    private BluetoothSocket gpsSocket;
    private String gpsDeviceAddress;
    private boolean enabled = false;
    private ExecutorService notificationPool;
    private ScheduledExecutorService connectionAndReadingPool;
    private List<NmeaListener> nmeaListeners = Collections
            .synchronizedList(new LinkedList<NmeaListener>());
    private ConnectedArduino connectedGps;
    private int maxConnectionRetries;
    private int nbRetriesRemaining;
    private boolean connected = false;

    /**
     * @param callingService
     * @param deviceAddress
     * @param maxRetries
     */
    public BlueetoothArduino(Service callingService, String deviceAddress,
                             int maxRetries) {
        this.gpsDeviceAddress = deviceAddress;
        this.callingService = callingService;
        this.maxConnectionRetries = maxRetries;
        this.nbRetriesRemaining = 1 + maxRetries;

    }

    /**
     * @return true if the bluetooth is enabled
     */
    public synchronized boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables the bluetooth Provider.
     *
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public synchronized boolean enable() {

        if (!enabled) {
            Log.d(LOG_TAG, "enabling Bluetooth GPS manager");
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                    .getDefaultAdapter();
            if (bluetoothAdapter == null) {
                // Device does not support Bluetooth
                Log.e(LOG_TAG, "Device does not support Bluetooth");

            } else if (!bluetoothAdapter.isEnabled()) {
                // Intent enableBtIntent = new
                // Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                Log.e(LOG_TAG, "Bluetooth is not enabled");

            } else {
                final BluetoothDevice gpsDevice = bluetoothAdapter
                        .getRemoteDevice(gpsDeviceAddress);
                if (gpsDevice == null) {
                    Log.e(LOG_TAG, "GPS device not found");

                } else {
                    Log.e(LOG_TAG, "current device: " + gpsDevice.getName()
                            + " -- " + gpsDevice.getAddress());

                    if (FIX_BUG == true) {

                        try {
                            Method m;
                            m = gpsDevice.getClass().getMethod(
                                    "createRfcommSocket",
                                    int.class);
                            gpsSocket = (BluetoothSocket) m
                                    .invoke(gpsDevice, 1);
                        } catch (NoSuchMethodException e) {
                            gpsSocket = null;
                        } catch (IllegalArgumentException e) {
                            gpsSocket = null;
                        } catch (IllegalAccessException e) {
                            gpsSocket = null;
                        } catch (InvocationTargetException e) {
                            gpsSocket = null;
                        }

                    } else {
                        try {
                            gpsSocket = gpsDevice
                                    .createRfcommSocketToServiceRecord(UUID
                                            .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Error during connection", e);
                            gpsSocket = null;
                        }
                    }

                    if (gpsSocket == null) {
                        Log.e(LOG_TAG,
                                "Error while establishing connection: no socket");

                    } else {
                        Runnable connectThread = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    connected = false;
                                    Log.v(LOG_TAG, "current device: "
                                            + gpsDevice.getName() + " -- "
                                            + gpsDevice.getAddress());
                                    if ((bluetoothAdapter.isEnabled())
                                            && (nbRetriesRemaining > 0)) {
                                        try {
                                            if (connectedGps != null) {
                                                connectedGps.close();
                                            }
                                            if ((gpsSocket != null)
                                                    && ((connectedGps == null) || (connectedGps.socket != gpsSocket))) {
                                                Log.d(LOG_TAG,
                                                        "trying to close old socket");
                                                SystemClock.sleep(5000); // sometimes
                                                // a
                                                // connect
                                                // deadlocks
                                                // -
                                                // wait
                                                // some
                                                // time
                                                // may
                                                // help
                                                gpsSocket.close();
                                                SystemClock.sleep(5000);
                                            }
                                        } catch (IOException e) {
                                            Log.e(LOG_TAG,
                                                    "Error during disconnection",
                                                    e);
                                            SystemClock.sleep(5000);
                                        }

                                        if (FIX_BUG == true)

                                            try {
                                                Method m;
                                                m = gpsDevice
                                                        .getClass()
                                                        .getMethod(
                                                                "createRfcommSocket",
                                                                int.class);
                                                gpsSocket = (BluetoothSocket) m
                                                        .invoke(gpsDevice, 1);
                                            } catch (NoSuchMethodException e) {
                                                Log.e(LOG_TAG,
                                                        "Error during connection",
                                                        e);
                                                gpsSocket = null;
                                            } catch (IllegalArgumentException e) {
                                                Log.e(LOG_TAG,
                                                        "Error during connection",
                                                        e);
                                                gpsSocket = null;
                                            } catch (IllegalAccessException e) {
                                                Log.e(LOG_TAG,
                                                        "Error during connection",
                                                        e);
                                                gpsSocket = null;
                                            } catch (InvocationTargetException e) {
                                                Log.e(LOG_TAG,
                                                        "Error during connection",
                                                        e);
                                                gpsSocket = null;
                                            }
                                        else {
                                            try {

                                                gpsSocket = gpsDevice
                                                        .createRfcommSocketToServiceRecord(UUID
                                                                .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                                            } catch (IOException e) {
                                                Log.e(LOG_TAG,
                                                        "Error during connection",
                                                        e);
                                                gpsSocket = null;
                                            }
                                        }

                                        if (gpsSocket == null) {
                                            Log.e(LOG_TAG,
                                                    "Error while establishing connection: no socket");

                                        } else {
                                            // Cancel discovery because it will
                                            // slow down the connection
                                            bluetoothAdapter.cancelDiscovery();
                                            // we increment the number of
                                            // connection tries
                                            // Connect the device through the
                                            // socket. This will block
                                            // until it succeeds or throws an
                                            // exception
                                            SystemClock.sleep(10000);
                                            Log.v(LOG_TAG,
                                                    "connecting to socket");
                                            gpsSocket.connect();
                                            Log.d(LOG_TAG,
                                                    "connected to socket");
                                            connected = true;
                                            // reset eventual disabling cause
                                            // setDisableReason(0);
                                            // connection obtained so reset the
                                            // number of connection try
                                            nbRetriesRemaining = 1 + maxConnectionRetries;

                                            Log.v(LOG_TAG,
                                                    "starting socket reading task");
                                            connectedGps = new ConnectedArduino(
                                                    gpsSocket);
                                            connectionAndReadingPool
                                                    .execute(connectedGps);
                                            Log.v(LOG_TAG,
                                                    "socket reading thread started");
                                        }
                                        // } else if (!
                                        // bluetoothAdapter.isEnabled()) {
                                        // setDisableReason(R.string.msg_bluetooth_disabled);
                                    }
                                } catch (IOException connectException) {
                                    // Unable to connect
                                    SystemClock.sleep(5000);
                                    Log.e(LOG_TAG,
                                            "error while connecting to socket");
                                    // disable(R.string.msg_bluetooth_gps_unavaible);
                                } finally {
                                    nbRetriesRemaining--;
                                    if (!connected) {
                                        disableIfNeeded();
                                    }
                                }
                            }
                        };
                        this.enabled = true;
                        Log.d(LOG_TAG, "Bluetooth manager enabled");
                        Log.v(LOG_TAG, "starting notification thread");
                        notificationPool = Executors.newSingleThreadExecutor();
                        Log.v(LOG_TAG, "starting connection and reading thread");
                        connectionAndReadingPool = Executors
                                .newSingleThreadScheduledExecutor();
                        Log.v(LOG_TAG, "starting connection to socket task");
                        connectionAndReadingPool.scheduleWithFixedDelay(
                                connectThread, 5000, 10000,
                                TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
        return this.enabled;
    }

    /**
     * Disables the bluetooth GPS Provider if the maximal number of connection
     * retries is exceeded. This is used when there are possibly non fatal
     * connection problems. In these cases the provider will try to reconnect
     * with the bluetooth device and only after a given retries number will give
     * up and shutdown the service.
     */
    private synchronized void disableIfNeeded() {
        if (enabled) {
            if (nbRetriesRemaining > 0) {
                // Unable to connect
                Log.e(LOG_TAG, "Unable to establish connection");

            } else {

                Log.e(LOG_TAG, "two_many_connection_problems");

            }
        }
    }

    private void notifySentence(final String Sentence) {

        ((BluetoothTempService) callingService).publishUpdate(Sentence);

    }

    /**
     * Disables the bluetooth GPS provider.
     * <p>
     * It will:
     * <ul>
     * <li>close the connection with the bluetooth device</li>
     * <li>disable the Mock Location Provider used for the bluetooth GPS</li>
     * <li>stop the BlueGPS4Droid service</li>
     * </ul>
     * If the bluetooth provider is closed because of a problem, a notification
     * is displayed.
     */
    public synchronized void disable() {

        if (enabled) {
            Log.d(LOG_TAG, "disabling Bluetooth GPS manager");
            enabled = false;
            connectionAndReadingPool.shutdown();
            Runnable closeAndShutdown = new Runnable() {
                @Override
                public void run() {
                    try {
                        connectionAndReadingPool.awaitTermination(10,
                                TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!connectionAndReadingPool.isTerminated()) {
                        connectionAndReadingPool.shutdownNow();
                        if (connectedGps != null) {
                            connectedGps.close();
                        }
                        if ((gpsSocket != null)
                                && ((connectedGps == null) || (connectedGps.socket != gpsSocket))) {
                            try {
                                Log.d(LOG_TAG, "closing Bluetooth GPS socket");
                                SystemClock.sleep(5000); // sometimes a connect
                                // deadlocks - wait
                                // some time may
                                // help
                                gpsSocket.close();
                                SystemClock.sleep(5000);
                            } catch (IOException closeException) {
                                Log.e(LOG_TAG, "error while closing socket",
                                        closeException);
                                SystemClock.sleep(5000);
                            }
                        }
                    }
                }
            };
            notificationPool.execute(closeAndShutdown);
            nmeaListeners.clear();
            // disableMockLocationProvider();
            notificationPool.shutdown();
            // connectionAndReadingPool.shutdown();
            callingService.stopSelf();
            Log.d(LOG_TAG, "Bluetooth GPS manager disabled");
        }
    }

    /**
     * A utility class used to manage the communication with the bluetooth
     * when the connection has been established. It is used to read  data
     */
    private class ConnectedArduino extends Thread {
        /**
         * bluetooth socket used for communication.
         */
        private final BluetoothSocket socket;
        /**
         * InputStream from which we read data.
         */
        private final InputStream in;
        /**
         * output stream to which we send data
         */
        private final OutputStream out;
        /**
         * output stream to which we send data
         */
        private final PrintStream out2;
        /**
         * A boolean which indicates if the GPS is ready to receive data. In
         * fact we consider that the GPS is ready when it begins to sends
         * data...
         */
        private boolean ready = false;

        public ConnectedArduino(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            PrintStream tmpOut2 = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                if (tmpOut != null) {
                    tmpOut2 = new PrintStream(tmpOut, false, "US-ASCII");
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while getting socket streams", e);
            }
            in = tmpIn;
            out = tmpOut;
            out2 = tmpOut2;
        }


        public void run() {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, "US-ASCII"), 1024);
                String s;
                long now = SystemClock.uptimeMillis();
                long lastRead = now;
                while ((enabled) && (now < lastRead + 20000)) {
                    if (reader.ready()) {
                        s = reader.readLine();
                        Log.v(LOG_TAG, "data: " + System.currentTimeMillis()
                                + " " + s);
                        notifySentence(s);
                        ready = true;
                        lastRead = SystemClock.uptimeMillis();
                    } else {
                        Log.d(LOG_TAG,
                                "data: not ready " + System.currentTimeMillis());
                        SystemClock.sleep(2000);
                    }
                    now = SystemClock.uptimeMillis();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while getting data", e);

            } finally {
                // cleanly closing everything...
                this.close();
                disableIfNeeded();
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                do {
                    Thread.sleep(100);
                } while ((enabled) && (!ready));
                if ((enabled) && (ready)) {
                    out.write(buffer);
                    out.flush();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The data to write
         */
        public void write(String buffer) {
            try {
                do {
                    Thread.sleep(100);
                } while ((enabled) && (!ready));
                if ((enabled) && (ready)) {
                    out2.print(buffer);
                    out2.flush();
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Exception during write", e);
            }
        }

        public void close() {
            ready = false;
            try {
                Log.d(LOG_TAG, "closing Bluetooth output sream");
                in.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "error while closing output stream", e);
            } finally {
                try {
                    Log.d(LOG_TAG, "closing Bluetooth input streams");
                    out2.close();
                    out.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "error while closing input streams", e);
                } finally {
                    try {
                        Log.d(LOG_TAG, "closing Bluetooth socket");
                        SystemClock.sleep(5000);
                        socket.close();
                        SystemClock.sleep(5000);
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "error while closing socket", e);
                        SystemClock.sleep(5000);
                    }
                }
            }
        }
    }

}
