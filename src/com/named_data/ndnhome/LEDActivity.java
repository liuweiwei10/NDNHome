package com.named_data.ndnhome;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class LEDActivity extends Activity {
	private static final String TAG = "NDNHome";
	private static final String INFO_MSG = "Retrieve access info";
	private static final String STATUS_MSG = "Read status";
	private static final String TURNON_MSG = "Turn on device";
	private static final String TURNOFF_MSG = "Turn off device";

	private TextView _tvDeviceName;
	private ImageButton _ibDeviceControl;
	private TextView _tvDeviceStatus;
	private String _status = "Unknown";
	private String _statusBase = "Current status:";
	private ProgressDialog _proDlg1 = null;
	private ProgressDialog _proDlg2 = null;
	private ProgressDialog _proDlg3 = null;
	private ProgressDialog _proDlg4 = null;
	private String _devicePrefix;
	private String _username;
	private String _password;
	private List<AccessTokenInfo> _infoList = null;
	private HashMap<String, String> commandMsgPair;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.led);
		_tvDeviceName = (TextView) findViewById(R.id.tv_device_name);
		_tvDeviceStatus = (TextView) findViewById(R.id.tv_device_status);
		_ibDeviceControl = (ImageButton) findViewById(R.id.ib_device_control);
		SharedPreferences sp = getSharedPreferences("account", 0);
		_username = sp.getString("username", "");
		_password = sp.getString("password", "");
		
		commandMsgPair = new HashMap<String, String>();
		commandMsgPair.put("status", STATUS_MSG);
		commandMsgPair.put("turnOn", TURNON_MSG);
		commandMsgPair.put("turnOff", TURNOFF_MSG);

		// get device name
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			_devicePrefix = extras.getString("device");
			_tvDeviceName.setText(Util.getDeviceNameFromPrefixStr(_devicePrefix));
		}

		retrieveAccessTokenInfo(_devicePrefix);

		// retrieve device status from controller

		_ibDeviceControl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (_status == "On") {
					if (_infoList != null) {
						AccessTokenInfo accessTokenInfo = Util.getAccessTokenInfoFromList(_infoList, "turnOff");
						turnOffLED(_devicePrefix, accessTokenInfo);
					} else {
						Toast.makeText(LEDActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
					}

				} else if (_status == "Off") {
					if (_infoList != null) {
						AccessTokenInfo accessTokenInfo = Util.getAccessTokenInfoFromList(_infoList, "turnOn");
						turnOnLED(_devicePrefix, accessTokenInfo);
					} else {
						Toast.makeText(LEDActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
					}

				} else if (_status == "Unknown") {
					Toast.makeText(LEDActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
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

	private void turnOnLED(String devicePrefix, AccessTokenInfo accessTokenInfo) {
		_proDlg3 = ProgressDialog.show(this, "", "waiting...");
		ControlThread thread = new ControlThread(devicePrefix, accessTokenInfo, actionHandler, commandMsgPair, 2);
		thread.start();
	}

	private void turnOffLED(String devicePrefix, AccessTokenInfo accessTokenInfo) {
		_proDlg4 = ProgressDialog.show(this, "", "waiting...");
		ControlThread thread = new ControlThread(devicePrefix, accessTokenInfo, actionHandler, commandMsgPair, 3);
		thread.start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent();
			intent.setClass(LEDActivity.this, DevicesActivity.class);
			startActivity(intent);
			LEDActivity.this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
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
				// show status
				if (status.equalsIgnoreCase("on")) {
					_status = "On";
					_ibDeviceControl.setImageResource(R.drawable.led_on);

				} else {
					_status = "Off";
					_ibDeviceControl.setImageResource(R.drawable.led_off);
				}
				_tvDeviceStatus.setText(_statusBase + _status);
				break;

			case 202: // turn on LED: success
				if (msg.obj.toString().contains("success")) {
					_status = "On";
					_tvDeviceStatus.setText(_statusBase + _status);
					_ibDeviceControl.setImageResource(R.drawable.led_on);

				} else {
					Toast.makeText(LEDActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
				}
				break;

			case 203:// turn off LED: success
				if (msg.obj.toString().contains("success")) {
					_status = "Off";
					_tvDeviceStatus.setText(_statusBase + _status);
					_ibDeviceControl.setImageResource(R.drawable.led_off);

				} else {
					Toast.makeText(LEDActivity.this, "can't operate this device!", Toast.LENGTH_SHORT).show();
				}
				break;
			
			default:
				Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
				break;
			}

			if (_proDlg1 != null)
				_proDlg1.dismiss();
			if (_proDlg2 != null)
				_proDlg2.dismiss();
			if (_proDlg3 != null)
				_proDlg3.dismiss();
			if (_proDlg4 != null)
				_proDlg4.dismiss();

		}
	};

}
