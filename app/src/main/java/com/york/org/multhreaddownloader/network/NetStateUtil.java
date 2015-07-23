package com.york.org.multhreaddownloader.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * 网络状态util
 */
public class NetStateUtil {

	/**
	 * 
	 * 
	 * @param context
	 * @return
	 */
	public static NetType getNetType(Context context) {
		NetType netType = new NetType();
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		 
		telephonyManager.getNetworkType();
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null) {
			NetworkInfo info = connectivityManager.getActiveNetworkInfo();
			if (info != null && info.isAvailable()) {
				int type = info.getType();
				if (type == ConnectivityManager.TYPE_WIFI) {
					netType.setConnType(NetType.CONN_TYPE_WIFI);
				} else if (type == ConnectivityManager.TYPE_MOBILE) { 
					netType.setConnType(getNetworkClass(telephonyManager.getNetworkType()));
				}
				String proxyHost = android.net.Proxy.getDefaultHost();
				 if ((null == proxyHost || proxyHost.length() <= 0)
							&& ("cmwap".equalsIgnoreCase(info.getExtraInfo())
									|| "3gwap".equalsIgnoreCase(info
											.getExtraInfo()) || "uniwap"
										.equalsIgnoreCase(info.getExtraInfo()))) {// 用户选择apn为wap但是没设置代理
						netType.setProxy("10.0.0.172");
						netType.setPort(80);
						
				}
			}
		}
		return netType;
	}
	public static int getNetworkClass(int networkType) {
		switch (networkType) {
		case TelephonyManager.NETWORK_TYPE_GPRS:
		case TelephonyManager.NETWORK_TYPE_EDGE:
		case TelephonyManager.NETWORK_TYPE_CDMA:
		case TelephonyManager.NETWORK_TYPE_1xRTT:
		case TelephonyManager.NETWORK_TYPE_IDEN:
		return NetType.CONN_TYPE_MOBILE_2G;//2g
		
		case TelephonyManager.NETWORK_TYPE_UMTS:
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
		case TelephonyManager.NETWORK_TYPE_HSDPA:
		case TelephonyManager.NETWORK_TYPE_HSUPA:
		case TelephonyManager.NETWORK_TYPE_HSPA:
		case TelephonyManager.NETWORK_TYPE_EVDO_B:
		case TelephonyManager.NETWORK_TYPE_EHRPD:
		case TelephonyManager.NETWORK_TYPE_HSPAP:
		return NetType.CONN_TYPE_MOBILE_3G; //3g
		
		case TelephonyManager.NETWORK_TYPE_LTE: //4g
		return NetType.CONN_TYPE_MOBILE_4G;
		
		default:
		return NetType.CONN_TYPE_MOBILE_4G;
		}
		}
	/**
	 * 
	 * 自定义网络类型
	 * 
	 */
	public static class NetType {
		public static final int CONN_TYPE_NONE = 0;// 无连接
		public static final int CONN_TYPE_WIFI = 1;// wifi连接
		public static final int CONN_TYPE_MOBILE_2G = 2;// 2g网络
		public static final int CONN_TYPE_MOBILE_3G  = 3;//3g网络
		public static final int CONN_TYPE_MOBILE_4G  = 4;//3g网络

		private String proxy;
		private int port;
		private int connType = CONN_TYPE_NONE;

		public String getProxy() {
			return proxy;
		}

		public void setProxy(String proxy) {
			this.proxy = proxy;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getConnType() {
			return connType;
		}

		public void setConnType(int connType) {
			this.connType = connType;
		}
	}

}
