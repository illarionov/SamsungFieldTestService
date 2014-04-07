package ru0xdc.sfts.service;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.util.ArrayList;
import java.util.List;

public class OemRilRequestHandler {

    private static final String TAG = "OemRilRequestHandler";
    private static final String PERMISSION_ACCESS_SUPERUSER = "android.permission.ACCESS_SUPERUSER";

    private Handler mOemRilHandler;
    private RemoteCallbackList<IOemRawResultListener> mOnRawResultListeners;

    private final Phone mPhone;

    private final Context mContext;

    public OemRilRequestHandler(Context context) {
        mContext = context;
        mPhone = PhoneFactory.getDefaultPhone();
        mOemRilHandler = new Handler(new OemRilRequestResponseHandler());
        mOnRawResultListeners = new RemoteCallbackList<IOemRawResultListener>();
    }

    public void onDestroy() {
        mOnRawResultListeners.kill();
    }

    public void invokeOemRilRequestRaw(int id, byte[] data) throws RemoteException {
        if (checkPermission()) {
            Message msg = mOemRilHandler.obtainMessage(id);
            mPhone.invokeOemRilRequestRaw(data, msg);
        }
    }

    public void registerOnOemRawResiltListener(IOemRawResultListener listener)
            throws RemoteException {
        if (checkPermission()) {
            if (listener != null) {
                mOnRawResultListeners.register(listener);
            }
        }
    }

    public void unregisterOnOemRawResiltListener(IOemRawResultListener listener) throws RemoteException {
        if (checkPermission()) {
            if (listener != null) mOnRawResultListeners.unregister(listener);
        }
    }

    private boolean checkPermission() throws SecurityException {
        if (mContext.checkCallingPermission("android.permission.ACCESS_SUPERUSER") != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "", new SecurityException("android.permission.ACCESS_SUPERUSER must be granted"));
            return false;
        }
        return true;
    }

    private class OemRilRequestResponseHandler implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            AsyncResult result = (AsyncResult) msg.obj;
            broadcastOnRawResult(msg.what, result);
            return true;
        }
    }

    private void broadcastOnRawResult(final int what, AsyncResult result) {
        List<String> unpacked = null;

        if (result != null) {
            if (result.exception != null) {
                Log.e(TAG, "", result.exception);
            } else {
                unpacked = Utils.unpackListOfStrings((byte[]) result.result);
            }
        }

        Log.i(TAG, "result: " + (unpacked == null ? null : TextUtils.join("; ", unpacked)));
        int i = mOnRawResultListeners.beginBroadcast();
        while (i > 0) {
            i--;
            final IOemRawResultListener listener = mOnRawResultListeners.getBroadcastItem(i);
            final ArrayList<String> results = unpacked == null ? null : new ArrayList<String>(unpacked);
            try {
                listener.onRawResult(what, results);
            } catch (RemoteException e) {
                // The RemoteCallbackList will take care of removing
                // the dead object for us.
            }
        }
        mOnRawResultListeners.finishBroadcast();
    }
}
