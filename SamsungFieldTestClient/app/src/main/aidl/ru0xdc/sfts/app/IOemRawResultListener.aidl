package ru0xdc.sfts.app;

oneway interface IOemRawResultListener {
	void onRawResult(int id, in List<String> result);
}
