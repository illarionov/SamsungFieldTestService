package ru0xdc.sfts.service;

import ru0xdc.sfts.service.IOemRawResultListener;

interface ISamsungServiceMode {

    List<String> getCpSwVersion();
    String getFtaSwVersion();
    String getFtaHwVersion();

	List<String> getCipheringInfo();
	List<String> getAllVersion();
    List<String> getBasicInfo();
    List<String> getNeighbours();

    void registerOnOemRawResultListener(in IOemRawResultListener listener);
   	void unregisterOnOemRawResultListener(in IOemRawResultListener listener);
	void invokeOemRilRequestRaw(int id, in byte[] data);
}
