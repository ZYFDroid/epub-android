package com.zy.myapplication.utils;

import android.util.Log;

public class LogUtils {

	private static final boolean isLog = true;

	public static void d(String tag, String msg) {
		if (!isLog) {
			Log.d(tag, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (isLog) {
			Log.e(tag, msg);
		}
	}

	public static void e(String tag, String string, Exception e) {
		if (isLog) {
			Log.e(tag, string, e);
		}
	}

	public static void i(String tag, String msg) {
		if (isLog) {
			Log.i(tag, msg);
		}
	}

	public static void v(String tag, String msg) {
		if (isLog) {
			Log.v(tag, msg);
		}
	}

	public static void w(String tag, String msg) {
		if (isLog) {
			Log.w(tag, msg);
		}
	}

	public static void wtf(String tag, String msg) {
		if (isLog) {
			Log.wtf(tag, msg);
		}
	}

	public static void LogHexD(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		d(tag, hexStr);
	}

	public static void LogHexE(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		e(tag, hexStr);
	}

	public static void LogHexI(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		i(tag, hexStr);
	}

	public static void LogHexV(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		v(tag, hexStr);
	}

	public static void LogHexW(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		w(tag, hexStr);
	}

	public static void LogHexWTF(String tag, byte[] data, int datalen) {
		String hexStr = "";
		for (int i = 0; i < datalen; i++) {
			hexStr += Integer.toHexString(data[i] & 0xff) + " ";
		}
		wtf(tag, hexStr);
	}
}