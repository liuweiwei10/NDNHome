package com.named_data.ndnhome;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.named_data.ndnhome.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ToggleButton;
import net.named_data.jndn.util.Blob;

public class CameraActivity extends Activity {
	private static final String TAG = "NDNHome";
	private TextView _tvDeviceName;
	private ImageView _ivImage;
	private Button _btTakePicture;
	private String _username;
	private String _password;
	private String _devicePrefix;
	private ProgressDialog _proDlg = null;
	private List<AccessTokenInfo> _infoList = null;
	private HashMap<String, String> commandMsgPair;
    private HashMap<Integer, byte[]> segments = new HashMap<Integer,byte[]>();
	private int packetRec = 0;
	private int finalSegNo = 0;
	private long timestamp;
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera);
		_tvDeviceName = (TextView) findViewById(R.id.tv_device_name);
		_ivImage = (ImageView) findViewById(R.id.im_picture);
		_btTakePicture = (Button) findViewById(R.id.bt_take_picture);

		SharedPreferences sp = getSharedPreferences("account", 0);
		_username = sp.getString("username", "");
		_password = sp.getString("password", "");

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			_devicePrefix = extras.getString("device");
			_tvDeviceName.setText(Util.getDeviceNameFromPrefixStr(_devicePrefix));
		}

		/*_proDlg = ProgressDialog.show(this, "", "waiting...");*/
		/*CameraThread thread = new CameraThread(_devicePrefix, actionHandler, 0, 1);
		thread.start();*/
		
		_btTakePicture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				_proDlg = ProgressDialog.show(CameraActivity.this, "", "waiting...");
				timestamp = System.currentTimeMillis();
				CameraThread thread = new CameraThread(_devicePrefix, actionHandler, 0, 1, timestamp);
				thread.start();
			}

		});
	}

	private Handler actionHandler = new Handler() {

		public void handleMessage(Message msg) {
			Blob content = (Blob) msg.obj;
		    byte[] seg = new byte[content.buf().remaining()];
		    content.buf().get(seg);
		    
			packetRec ++ ;
			if(msg.what > 200) {			
				segments.put(0, seg);			
				finalSegNo= msg.what-200;
				for(int i =1; i<= finalSegNo; i++) {
					CameraThread thread = new CameraThread(_devicePrefix, actionHandler, i, 1, timestamp);
					thread.start();	
				}
				/*CameraThread thread = new CameraThread(_devicePrefix, actionHandler, 1, finalSegNo );
				thread.start();	*/
			}else if(msg.what > 0){ 
			    segments.put(msg.what, seg);
			    if(packetRec == finalSegNo + 1) {
			    
			    	try {
			    		//dismiss the loading dialog
			    		if (_proDlg != null)
							_proDlg.dismiss();
			    		Log.d(TAG, "whole image received");
			    		
			    		//assemble segments to Bitmap
			    		Bitmap bitmap = assembleSegments(segments);
						
						//saveBitmap(bitmap);
						_ivImage.setImageBitmap(bitmap);					
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			    	
			    	segments.clear();
			    	packetRec = 0;
			    	finalSegNo = 0;

			    	//send a new take-picture interest
				/*	CameraThread thread = new CameraThread(_devicePrefix, actionHandler, 0, 1);
					thread.start();*/
			    }
			}else {
				if (_proDlg != null)
					_proDlg.dismiss();
				Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
			}
			
		}
	};

	private Bitmap assembleSegments(HashMap<Integer, byte[]> segments) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for(int i = 0; i <= finalSegNo; i++) {
			if(segments.containsKey(i)){
				out.write(segments.get(i));		
			} else {
				Log.d(TAG, "missing segment " + i );
			}	
		}
		byte[] image = out.toByteArray();
		out.close();
		Bitmap bitmap = BitmapFactory.decodeByteArray(image , 0, image.length);
		return bitmap;
	}
 	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (_proDlg != null)
				_proDlg.dismiss();
			Intent intent = new Intent();
			intent.setClass(CameraActivity.this, DevicesActivity.class);
			startActivity(intent);
			CameraActivity.this.finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
