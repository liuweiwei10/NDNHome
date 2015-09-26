package com.named_data.ndnhome.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.named_data.ndnhome.AccessTokenInfo;

import android.util.Base64;
import android.util.Log;


public class Util {
	private static final String TAG = "NDNHome";
	
	public static String getDeviceNameFromPrefixStr(String prefixStr) {
		String deviceName;
		String[] components = prefixStr.split("/");
		deviceName = components[components.length-2] + ":" + components[components.length-1];
		return deviceName;
	}

	public static byte[] hash256(String data) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(data.getBytes());
		return md.digest();
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuffer result = new StringBuffer();
		for (byte byt : bytes)
			result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
		return result.toString();
	}

	public static byte[] hmacEncode(byte[] key, ByteBuffer message) throws Exception {

		SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(keySpec);
		byte[] byteArray = new byte[message.remaining()];
		message.get(byteArray);
		byte[] rawHmac = mac.doFinal(byteArray);

		return rawHmac;
	}
	
	public static byte[] hmacEncode(byte[] key, String message)throws Exception {

		SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");

		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(keySpec);
		byte[] rawHmac = mac.doFinal(message.getBytes());

		return rawHmac;
	}

	public static HashMap<String, String> getDeviceNames(JSONArray jsonArr) {
		HashMap<String, String> map = new HashMap<String, String>();
		int len = jsonArr.length();
		try {
			for (int i = 0; i < len; i++) {
				JSONObject jsonObj;
				jsonObj = jsonArr.getJSONObject(i);

				String prefix = jsonObj.getString("_prefix");
				String type = jsonObj.getString("_type");
                map.put(prefix, type);
                //Log.d(TAG, "add to map (" + prefix +"," +type +")" );
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.i(TAG, "error occurs when parsing json");
			e.printStackTrace();
		}
		return map;
	}
	
	public static List<AccessTokenInfo> getAccessTokenInfoList(JSONArray jsonArr) throws UnsupportedEncodingException {
		List<AccessTokenInfo> infoList = new ArrayList<AccessTokenInfo>();
		int len =  jsonArr.length();
		try {
			for(int i = 0; i < len; i++) {

				JSONObject jsonObj;
				jsonObj = jsonArr.getJSONObject(i);
				String command = jsonObj.getString("command");
				int commandTokenSequence = jsonObj.getInt("commandTokenSequence");
				int seedSequence = jsonObj.getInt("seedSequence");
				String accessTokenStr = jsonObj.getString("accessToken");
				//Log.d(TAG, jsonObj.get("accessToken").toString());
				byte[] accessToken = Base64.decode(accessTokenStr,Base64.NO_WRAP);

				//Log.d(TAG, "access token original string:" + accessTokenStr);
				//Log.d(TAG, "access token decoded(hex):" + bytesToHex(accessToken));
/*				Log.d(TAG, "access token byte array default:" + accessTokenStr.getBytes());
				Log.d(TAG, "access token byte array latin1:" + accessTokenStr.getBytes(Charset.forName("ISO-8859-1")));
				Log.d(TAG, "access token byte array utf8" + accessTokenStr.getBytes(Charset.forName("UTF-8")));*/
				String accessTokenName = jsonObj.getString("accessTokenName");
				AccessTokenInfo accessTokenInfo = new AccessTokenInfo(command, commandTokenSequence, seedSequence, accessToken, accessTokenName);
			    infoList.add(accessTokenInfo);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			Log.i(TAG, "error occurs when parsing json");
			e.printStackTrace();
		}
		return infoList;
	}
	
		 
	 
	public static AccessTokenInfo getAccessTokenInfoFromList(List<AccessTokenInfo> infoList, String command) {
		AccessTokenInfo accessTokenInfo = null;
		Iterator<AccessTokenInfo> iter = infoList.iterator();
		while(iter.hasNext()) {
			AccessTokenInfo curToken = iter.next();
			if(curToken.getCommandName().equals(command)) {
				accessTokenInfo = curToken;
				break;
			}
		}		
		return accessTokenInfo;		
	}

}
