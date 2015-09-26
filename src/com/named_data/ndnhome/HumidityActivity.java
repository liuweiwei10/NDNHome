package com.named_data.ndnhome;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.named_data.ndnhome.util.Util;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;


public class HumidityActivity extends Activity {

	private static final String TAG = "NDNHome";
	private static final String STATUS_MSG = "Read status";
	private static final int INFO_CODE = 0;
	private TextView _tvDeviceName;
	private TextView _tvHumidity;
	private TextView _tvTemperature;
	private Button _btUpdate;
	private String _humiditySuffix = " %";
	private String _temperatureSuffix = "°C";
	private ProgressDialog _proDlg1 = null;
	private ProgressDialog _proDlg2 = null;
	private String _username;
	private String _password;
	private String _devicePrefix;
	private List<AccessTokenInfo> _infoList = null;
	private HashMap<String, String> commandMsgPair;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.humidity);

		_tvDeviceName = (TextView) findViewById(R.id.tv_device_name);
		_tvHumidity = (TextView) findViewById(R.id.tv_humidity);
		_tvTemperature = (TextView) findViewById(R.id.tv_temperature);
		_btUpdate = (Button) findViewById(R.id.bt_update);

		SharedPreferences sp = getSharedPreferences("account", 0);
		_username = sp.getString("username", "");
		_password = sp.getString("password", "");

		commandMsgPair = new HashMap<String, String>();
		commandMsgPair.put("status", STATUS_MSG);

		// get device name
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			_devicePrefix = extras.getString("device");
			_tvDeviceName.setText(Util.getDeviceNameFromPrefixStr(_devicePrefix));
		}
		
		retrieveAccessTokenInfo(_devicePrefix);

		_btUpdate.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (_infoList != null) {
					AccessTokenInfo accessTokenInfo = Util.getAccessTokenInfoFromList(_infoList, "status");
					retrieveDeviceStatus(_devicePrefix, accessTokenInfo);
				} else {
					Toast.makeText(HumidityActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
				}
			}

		});

	}

	private void retrieveAccessTokenInfo(String devicePrefix) {
		// strip off "/home" from device prefix
		String deviceName = devicePrefix.substring(5);

		_proDlg1 = ProgressDialog.show(this, "", "waiting...");
		InfoThread thread = new InfoThread(_username, _password, deviceName, actionHandler, 0);
		thread.start();
	}

	private void retrieveDeviceStatus(String devicePrefix, AccessTokenInfo accessTokenInfo) {
		_proDlg2 = ProgressDialog.show(this, "", "waiting...");
		ControlThread thread = new ControlThread(devicePrefix, accessTokenInfo, actionHandler, commandMsgPair, 1);
		thread.start();

	}

	private Handler actionHandler = new Handler() {

		public void handleMessage(Message msg) {

			switch (msg.what) { // Result Code

			case 200: // request access token info: Success: 200

				try {
					JSONArray jsonArr = new JSONArray(msg.obj.toString());
					// get AccessTokenInfo list from JsonArray;
					_infoList = Util.getAccessTokenInfoList(jsonArr);

					// get access token info object for command "status"
					AccessTokenInfo accessTokenInfo = Util.getAccessTokenInfoFromList(_infoList, "status");

					// retrieve device status with access token info for command
					// "status"
					retrieveDeviceStatus(_devicePrefix, accessTokenInfo);

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					Log.i(TAG, "fail to build json array.");
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case 201: // request status: success
				String status = msg.obj.toString();
				Log.d(TAG, "humidity status:" + status);
				String humidityStr = null;
				String temperatureStr= null;
				try {
					humidityStr = getHumidityStr(status);
					temperatureStr = getTemperatureStr(status);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// show status
				_tvHumidity.setText(humidityStr + _humiditySuffix);
				_tvTemperature.setText(temperatureStr +  _temperatureSuffix);
				break;

			default:
				Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			}

			if (_proDlg1 != null)
				_proDlg1.dismiss();
			if (_proDlg2 != null)
				_proDlg2.dismiss();

		}
	};

	private String getHumidityStr(String status) throws JSONException{
		JSONObject json = new JSONObject(status);		
		return json.get("humidity").toString();
	}
	private String getTemperatureStr(String status) throws JSONException {
		JSONObject json = new JSONObject(status);	
		return json.get("temperature").toString();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent();
			intent.setClass(HumidityActivity.this, DevicesActivity.class);
			startActivity(intent);
			HumidityActivity.this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
