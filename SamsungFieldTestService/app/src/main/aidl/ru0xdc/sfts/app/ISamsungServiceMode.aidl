package ru0xdc.sfts.app;

import ru0xdc.sfts.app.IOemRawResultListener;

interface ISamsungServiceMode {

	List<String> getCipheringInfo();
	List<String> getAllVersion();
    List<String> getBasicInfo();
    List<String> getNeighbours();

    void registerOnOemRawResultListener(in IOemRawResultListener listener);
   	void unregisterOnOemRawResultListener(in IOemRawResultListener listener);
	void invokeOemRilRequestRaw(int id, in byte[] data);
}
