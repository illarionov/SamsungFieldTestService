package ru0xdc.sfts.service;

oneway interface IOemRawResultListener {
	void onRawResult(int id, in List<String> result);
}
