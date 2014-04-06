package ru0xdc.sfts.app;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.cyanogenmod.samsungservicemode.OemCommands;

public class FieldTestService extends Service {

	private static final String TAG = "FieldTestService";

    private static final int ID_REQUEST_START_SERVICE_MODE_COMMAND = 6008;
    private static final int ID_REQUEST_FINISH_SERVICE_MODE_COMMAND = 6009;
    private static final int ID_REQUEST_PRESS_A_KEY = 6010;
    private static final int ID_REQUEST_REFRESH = 6011;

    private static final int ID_RESPONSE = 7008;
    private static final int ID_RESPONSE_FINISH_SERVICE_MODE_COMMAND = 7009;
    private static final int ID_RESPONSE_PRESS_A_KEY = 7010;

    private static final int REQUEST_TIMEOUT = 10000; // ms

    static final KeyStep GET_BASIC_INFO_KEY_SEQ[] = new KeyStep[] {
            new KeyStep('\0', false),
            new KeyStep('1', false), // [1] DEBUG SCREEN
            new KeyStep('1', true), // [1] BASIC INFORMATION
    };
    static final KeyStep GET_NEIGHBOURS_KEY_SEQ[] = new KeyStep[] {
            new KeyStep('\0', false),
            new KeyStep('1', false), // [1] DEBUG SCREEN
            new KeyStep('4', true), // [4] NEIGHBOUR CELL
    };

    private final Object mLastResponseLock = new Object();
    private volatile List<String> mLastResponse;
    
    private final ConditionVariable mRequestCondvar = new ConditionVariable();
    
    private Handler mHandler;
    private Phone mPhone;
    private OemRilRequestHandler mOemRilRequestHandler;

	@Override
    public void onCreate() {
        super.onCreate();
        mPhone = PhoneFactory.getDefaultPhone();
        mHandler = new Handler(new MyHandler());
        mOemRilRequestHandler = new OemRilRequestHandler(this);
    }

	@Override
	public void onDestroy() {
        super.onDestroy();
        mOemRilRequestHandler.onDestroy();
        mOemRilRequestHandler = null;
        mPhone = null;
        mHandler = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private final ISamsungServiceMode.Stub mBinder = new ISamsungServiceMode.Stub() {

		@Override
		public List<String> getCipheringInfo() throws RemoteException {
			return FieldTestService.this.getCipheringInfo();
		}

        @Override
        public List<String> getAllVersion() throws RemoteException {
            return FieldTestService.this.getAllVersion();
        }

        @Override
        public List<String> getBasicInfo() throws RemoteException {
            return FieldTestService.this.getBasicInfo();
        }

        @Override
        public List<String> getNeighbours() throws RemoteException {
            return FieldTestService.this.getNeighbours();
        }

        @Override
		public void invokeOemRilRequestRaw(int id, byte[] data) throws RemoteException {
            mOemRilRequestHandler.invokeOemRilRequestRaw(id, data);
		}

		@Override
		public void registerOnOemRawResultListener(IOemRawResultListener listener)
				throws RemoteException {
            mOemRilRequestHandler.registerOnOemRawResiltListener(listener);
		}

		@Override
		public void unregisterOnOemRawResultListener(
				IOemRawResultListener listener) throws RemoteException {
            mOemRilRequestHandler.unregisterOnOemRawResiltListener(listener);
		}
	};

    synchronized List<String> getCipheringInfo() throws RemoteException {
        return executeServiceModeCommand(
                OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                OemCommands.OEM_SM_TYPE_SUB_CIPHERING_PROTECTION_ENTER,
                null
        );
    }

    synchronized List<String> getAllVersion() throws RemoteException {
        return executeServiceModeCommand(
                OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                OemCommands.OEM_SM_TYPE_SUB_ALL_VERSION_ENTER,
                null
        );
    }

    List<String> getBasicInfo() throws RemoteException {
        return executeServiceModeCommand(
                OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                OemCommands.OEM_SM_TYPE_SUB_ENTER,
                Arrays.asList(GET_BASIC_INFO_KEY_SEQ)
        );
    }

    List<String> getNeighbours() throws RemoteException {
        return executeServiceModeCommand(
                OemCommands.OEM_SM_TYPE_TEST_MANUAL,
                OemCommands.OEM_SM_TYPE_SUB_ENTER,
                Arrays.asList(GET_NEIGHBOURS_KEY_SEQ)
        );
    }

    private synchronized List<String> executeServiceModeCommand(int type,int subtype,
        java.util.Collection<? extends KeyStep> keySeqence) {
        if (mPhone == null) {
            Log.e(TAG, "default phone is null");
            return null;
        }

        mRequestCondvar.close();
        mHandler.obtainMessage(ID_REQUEST_START_SERVICE_MODE_COMMAND,
                type,
                subtype,
                keySeqence).sendToTarget();
        if (!mRequestCondvar.block(REQUEST_TIMEOUT)) {
            Log.e(TAG, "request timeout");
            return null;
        } else {
            synchronized (mLastResponseLock) {
                return mLastResponse;
            }
        }
    }

    static class KeyStep {
        public final char keychar;
        public boolean captureResponse;

        public KeyStep(char keychar, boolean captureResponse) {
            this.keychar = keychar;
            this.captureResponse = captureResponse;
        }

        public static KeyStep KEY_START_SERVICE_MODE = new KeyStep('\0', true);
    }

	private class MyHandler implements Handler.Callback {

        private int mCurrentType;
        private int mCurrentSubtype;

        private Queue<KeyStep> mKeySequence;

		@Override
		public boolean handleMessage(Message msg) {
            byte[] requestData;
            Message responseMsg;
            KeyStep lastKeyStep;

            switch (msg.what) {
                case ID_REQUEST_START_SERVICE_MODE_COMMAND:
                    mCurrentType = msg.arg1;
                    mCurrentSubtype = msg.arg2;
                    mKeySequence = new ArrayDeque<KeyStep>(3);
                    if (msg.obj != null) {
                        mKeySequence.addAll((java.util.Collection<? extends KeyStep>) msg.obj);
                    } else {
                        mKeySequence.add(KeyStep.KEY_START_SERVICE_MODE);
                    }
                    synchronized (mLastResponseLock) {
                        mLastResponse = new ArrayList<String>();
                    }
                    requestData = OemCommands.getEnterServiceModeData(
                            mCurrentType, mCurrentSubtype, OemCommands.OEM_SM_ACTION);
                    responseMsg = mHandler.obtainMessage(ID_RESPONSE);
                    mPhone.invokeOemRilRequestRaw(requestData, responseMsg);
                    break;
                case ID_REQUEST_FINISH_SERVICE_MODE_COMMAND:
                    requestData = OemCommands.getEndServiceModeData(mCurrentType);
                    responseMsg = mHandler.obtainMessage(ID_RESPONSE_FINISH_SERVICE_MODE_COMMAND);
                    mPhone.invokeOemRilRequestRaw(requestData, responseMsg);
                    break;
                case ID_REQUEST_PRESS_A_KEY:
                    requestData = OemCommands.getPressKeyData(msg.arg1, OemCommands.OEM_SM_ACTION);
                    responseMsg = mHandler.obtainMessage(ID_RESPONSE_PRESS_A_KEY);
                    mPhone.invokeOemRilRequestRaw(requestData, responseMsg);
                    break;
                case ID_REQUEST_REFRESH:
                    requestData = OemCommands.getPressKeyData('\0', OemCommands.OEM_SM_QUERY);
                    responseMsg = mHandler.obtainMessage(ID_RESPONSE);
                    mPhone.invokeOemRilRequestRaw(requestData, responseMsg);
                    break;
                case ID_RESPONSE:
                    lastKeyStep = mKeySequence.poll();
                    try {
                        AsyncResult result = (AsyncResult) msg.obj;
                        if (result == null) {
                            Log.e(TAG, "result is null");
                            break;
                        }
                        if (result.exception != null) {
                            Log.e(TAG, "", result.exception);
                            break;
                        }
                        if (result.result == null) {
                            Log.v(TAG, "No need to refresh.");
                            break;
                        }
                        if (lastKeyStep.captureResponse) {
                            synchronized (mLastResponseLock) {
                                mLastResponse.addAll(Utils.unpackListOfStrings((byte[]) result.result));
                            }
                        }
                    } finally {
                        if (mKeySequence.isEmpty()) {
                            mHandler.obtainMessage(ID_REQUEST_FINISH_SERVICE_MODE_COMMAND).sendToTarget();
                        } else {
                            mHandler.obtainMessage(ID_REQUEST_PRESS_A_KEY, mKeySequence.element().keychar, 0).sendToTarget();
                        }
                    }
                    break;
                case ID_RESPONSE_PRESS_A_KEY:
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(ID_REQUEST_REFRESH), 10);
                    break;
                case ID_RESPONSE_FINISH_SERVICE_MODE_COMMAND:
                    mRequestCondvar.open();
                    break;

            }
			return true;
		}
	}

}
