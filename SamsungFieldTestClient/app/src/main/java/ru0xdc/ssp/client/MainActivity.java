package ru0xdc.ssp.client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.cyanogenmod.samsungservicemode.OemCommands;

import java.util.List;

import ru0xdc.sfts.app.IOemRawResultListener;
import ru0xdc.sfts.app.ISamsungServiceMode;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int ID_SERVICE_MODE_START = 0;
    private static final int ID_SERVICE_MODE_REQUEST = 1;
    private static final int ID_SERVICE_MODE_END = 2;
    private static final int ID_SERVICE_MODE_SILENT = 3;

    boolean mBound = false;
    ISamsungServiceMode mService = null;

    private TextView mVersionTextView, mBasicInfoTextView, mNeighboursTextView, mCipheringInfoTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVersionTextView = (TextView)findViewById(R.id.version);
        mBasicInfoTextView = (TextView)findViewById(R.id.basic_info);
        mNeighboursTextView = (TextView)findViewById(R.id.neighbours);
        mCipheringInfoTextView = (TextView)findViewById(R.id.ciphering_info);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent("ru0xdc.sfts.app.SERVICE_MODE");
        if (!bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            Log.e(TAG, "bindService() error");
            findViewById(R.id.button_load).setEnabled(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            try {
                mService.unregisterOnOemRawResultListener(mRawResultListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(mConnection);
            mBound = false;
        }
    }

    public void runTest(View v) {
        if (!mBound) return;
        updateAllVersion();
        updateBasicInfo();
        updateNeighbours();
        updateCipherInfo();
    }

    void updateCipherInfo() {
        try {
            List<String> info = mService.getCipheringInfo();
            if (info == null) {
                mCipheringInfoTextView.setText("null");
            } else {
                mCipheringInfoTextView.setText(TextUtils.join("\n", info));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void updateAllVersion() {
        try {
            List<String> info = mService.getAllVersion();
            if (info == null) {
                mVersionTextView.setText("null");
            } else {
                mVersionTextView.setText(TextUtils.join("\n", info));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void updateNeighbours() {
        try {
            List<String> info = mService.getNeighbours();
            if (info == null) {
                mNeighboursTextView.setText("null");
            } else {
                mNeighboursTextView.setText(TextUtils.join("\n", info));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void updateBasicInfo() {
        try {
            List<String> info = mService.getBasicInfo();
            if (info == null) {
                mBasicInfoTextView.setText("null");
            } else {
                mBasicInfoTextView.setText(TextUtils.join("\n", info));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void updateCipherInfoRawRil() {
        try {
            byte[] data = OemCommands.getEnterServiceModeData(
                    OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                    OemCommands.OEM_SM_TYPE_SUB_CIPHERING_PROTECTION_ENTER,
                    OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            data = OemCommands.getEndServiceModeData(OemCommands.OEM_SM_TYPE_TEST_MANUAL);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_END, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void getAllVersionRawRil() {
        try {
            byte[] data = OemCommands.getEnterServiceModeData(
                    OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                    OemCommands.OEM_SM_TYPE_SUB_ALL_VERSION_ENTER,
                    OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            data = OemCommands.getEndServiceModeData(OemCommands.OEM_SM_TYPE_TEST_MANUAL);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_END, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void runMonitor() {
        try {
            byte[] data = OemCommands.getEnterServiceModeData(
                    OemCommands.OEM_SM_TYPE_MONITOR,
                    0,
                    0);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            data = OemCommands.getEndServiceModeData(OemCommands.OEM_SM_TYPE_MONITOR);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_END, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void getBasicInfo() {
        try {
            byte[] data = OemCommands.getEnterServiceModeData(
                    OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                    OemCommands.OEM_SM_TYPE_SUB_ENTER,
                    OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_START, data);

            // XXX: [1] DEBUG SCREEN
            data = OemCommands.getPressKeyData('1', OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_SILENT, data);
            Thread.sleep(10);

            // REFRESH
            data = OemCommands.getPressKeyData(0, OemCommands.OEM_SM_QUERY);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_SILENT, data);
            Thread.sleep(10);

            // XXX: [1] BASIC INFORMATION
            data = OemCommands.getPressKeyData('1', OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            Thread.sleep(10);

            // REFRESH
            data = OemCommands.getPressKeyData(0, OemCommands.OEM_SM_QUERY);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            Thread.sleep(10);

            data = OemCommands.getEndServiceModeData(OemCommands.OEM_SM_TYPE_TEST_MANUAL);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_END, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void getNeighbours() {
        try {
            byte[] data = OemCommands.getEnterServiceModeData(
                    OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                    OemCommands.OEM_SM_TYPE_SUB_ENTER,
                    OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_START, data);

            // XXX: [1] DEBUG SCREEN
            data = OemCommands.getPressKeyData('1', OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_SILENT, data);
            Thread.sleep(10);

            // REFRESH
            data = OemCommands.getPressKeyData(0, OemCommands.OEM_SM_QUERY);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_SILENT, data);
            Thread.sleep(10);

            // XXX: [4] NEIGHBOUR CELL
            data = OemCommands.getPressKeyData('4', OemCommands.OEM_SM_ACTION);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            Thread.sleep(10);

            // REFRESH
            data = OemCommands.getPressKeyData(0, OemCommands.OEM_SM_QUERY);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_REQUEST, data);
            Thread.sleep(10);

            data = OemCommands.getEndServiceModeData(OemCommands.OEM_SM_TYPE_TEST_MANUAL);
            mService.invokeOemRilRequestRaw(ID_SERVICE_MODE_END, data);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private IOemRawResultListener mRawResultListener = new IOemRawResultListener.Stub() {

        @Override
        public void onRawResult(final int id, final List<String> result) throws RemoteException {
            final String res = result == null ? "null" : TextUtils.join(". ", result);
            Log.i(TAG, "pid: " + android.os.Process.myPid() + " uid: "
                    + android.os.Process.myUid()
                    + "tid: " + android.os.Process.myTid()
            );

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "onRawResult() id: " + id + " res: " + res);

                    if (id == ID_SERVICE_MODE_REQUEST) {
                        if (result == null) {
                            mBasicInfoTextView.setText("null");
                        } else {
                            mBasicInfoTextView.setText(TextUtils.join("\n", result));
                        }
                    }
                }
            });
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            mService = ISamsungServiceMode.Stub.asInterface(service);
            try {
                mService.registerOnOemRawResultListener(mRawResultListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

}
