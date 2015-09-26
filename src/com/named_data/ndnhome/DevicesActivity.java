package com.named_data.ndnhome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import com.named_data.ndnhome.util.HmacHelper;
import com.named_data.ndnhome.util.Util;

import android.app.Activity;
import android.app.ProgressDialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

public class DevicesActivity extends Activity {
	private static final String TAG = "NDNHome";

	private TextView _tvWelcome;
	private ListView _lvDevices;
	private ProgressDialog _proDlg;
	private HmacHelper _hmacHelper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.devices);
		_tvWelcome =(TextView) findViewById(R.id.tv_welcome);
		_lvDevices = (ListView) findViewById(R.id.lv_devices);

		SharedPreferences sp = getSharedPreferences("account",0);
		String username = sp.getString("username","");
		String password = sp.getString("password","");		
		String welcomeStr =  "welcome " + username + " !";
		_tvWelcome.setText(welcomeStr);
		
		_proDlg = ProgressDialog.show(this, "", "waiting...");
		NetThread thread = new NetThread(username, password);
		thread.start();
	}
	

	private class NetThread extends Thread {
        private String username;
        private String password;
		public NetThread(String username, String password) {
            this.username = username;
            this.password = password;
		}

		@Override
		public void run() {
			Face face = new Face();
			try {

				//face = new Face(); //192.168.50.1

				PingTimer timer = new PingTimer();

				String interestName = "/home/controller/deviceList/user";
				Name name = new Name(interestName);
				name.append(username);	
				//Log.d(TAG, "password:" + password);
				byte[] key =  Util.hash256(password);
				
				Log.d(TAG, "key:" + Arrays.toString(key));
				
				_hmacHelper = new HmacHelper(key);
				
				Name keyName = new Name("user_key");			
                Interest interest =  new Interest(name);
				
				_hmacHelper.signInterest(interest, keyName);
				
				Log.i(TAG, "Express name " + interest.getName().toUri());

				timer.startUp();
				
				face.expressInterest(interest, timer, timer);

				// The main event loop.

				while (timer.callbackCount_ < 1) {

					face.processEvents();

					// We need to sleep for a few milliseconds so we don't use
					// 100% of
					// the CPU.

					Thread.sleep(5);

				}
                face.shutdown();
			}

			catch (Exception e) {
                face.shutdown();
				Log.i(TAG, "exception: " + e.getMessage());
	            Message msg = new Message();
	            
	            msg.what = 401; 
	            msg.obj = "execption occurs.";
	            actionHandler.sendMessage(msg);

				e.printStackTrace();

			}

		}

	}
	
	private class PingTimer implements OnData, OnTimeout {

		private long startTime;

		public void onData(Interest interest, Data data)

		{
			++callbackCount_;

			long elapsedTime = System.currentTimeMillis() - this.startTime;

			Log.i(TAG, "Got data packet with name " + data.getName().toUri() + ":" + String.valueOf(elapsedTime) + " ms") ;  
            Log.i(TAG, "Verifying data:");
            
			//verify data
			try {
				
			   	// need to implement 'verifyData(data)'			
				boolean verifyResult = true;  //_hmacHelper.verifyData(data)
				
				if(!verifyResult){
					Log.i(TAG, "wrong signature");
					Message msg = new Message();
					msg.what = 402;	
					msg.obj = "fail to verify data";
					actionHandler.sendMessage(msg);
		
				} else {
					Log.i(TAG, "verify data: succeed");
					String contentStr = data.getContent().toString();

					Log.i(TAG, "Content " + contentStr);

					// Send a result to Screen
					
		            if( contentStr.length() > 0) {
		    			Message msg = new Message();

		    			msg.what = 200; // Result Code ex) Success code: 200 , Fail Code:
		    							// 400 ...
		    			msg.obj =  contentStr;

		    			actionHandler.sendMessage(msg);
		            } else {
		                Message msg = new Message();
		                
		                msg.what = 400;
		                
		                actionHandler.sendMessage(msg);
		            }
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

		public int callbackCount_ = 0;

		public void onTimeout(Interest interest)

		{

			++callbackCount_;
            Message msg = new Message();
            
            msg.what = 408; //time out
            msg.obj = "can't connect to controller.";
            
            actionHandler.sendMessage(msg);

			Log.i(TAG, "Time out for interest " + interest.getName().toUri());

		}

		public void startUp() {

			startTime = System.currentTimeMillis();

		}

	}
	
	private Handler actionHandler = new Handler() {

		public void handleMessage(Message msg) {

			switch (msg.what) { // Result Code

			case 200: // Result Code Ex) Success: 200	
				
				//parse the json object to get device names
				JSONArray jsonArr;				
				try {
					//Log.d(TAG, "try to build json array from:" +msg.obj);
					String jsonStr = msg.obj.toString();
					jsonArr = new JSONArray(jsonStr);
		    			
					List<String> items = new ArrayList<String>();
					HashMap<String, String> deviceMap = Util.getDeviceNames(jsonArr);
					
				    SharedPreferences sp = getSharedPreferences("prefix_type",0);
		    		SharedPreferences.Editor editor = sp.edit();
		    		
				    SharedPreferences sp2 = getSharedPreferences("name_prefix",0);
		    		SharedPreferences.Editor editor2 = sp2.edit();
		    		
					for (HashMap.Entry<String, String> entry : deviceMap.entrySet()) { 
						items.add(Util.getDeviceNameFromPrefixStr(entry.getKey()));
						editor.putString(entry.getKey(), entry.getValue());			
						editor2.putString(Util.getDeviceNameFromPrefixStr(entry.getKey()), entry.getKey());
					}
		    		editor.commit();
		    		editor2.commit();
		    		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, items){
		    			  @Override
                          public View getView(int position, View convertView,
                                  ViewGroup parent) {
                              View view =super.getView(position, convertView, parent);

                              TextView textView=(TextView) view.findViewById(android.R.id.text1);

                              /*YOUR CHOICE OF COLOR*/
                              textView.setTextColor(Color.BLACK);

                              return view;
                          }
		    		};
		    		
                    _lvDevices.setAdapter(adapter);
		            _lvDevices.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		            _lvDevices.setOnItemClickListener(new OnItemClickListener() {  
		                @Override  
		                public void onItemClick(AdapterView<?> arg0, View view,  
		                        int position, long id) {
		                	SharedPreferences sp = getSharedPreferences("prefix_type",0);
		                	SharedPreferences sp2 = getSharedPreferences("name_prefix",0);
		                	String deviceName = (String) ((TextView) view).getText();
		                	String prefix = sp2.getString(deviceName, "");
		                	Log.d(TAG, "prefix:" + prefix);
		                	
		                	String type = sp.getString(prefix, "");
		                	Log.d(TAG, "type:" +type);
		                	
		                	switch(type) {
		                	case "LED":
		            			Intent intent = new Intent();
		            			Bundle bundle = new Bundle();
		        				intent.putExtra("device", prefix);
		        				intent.putExtras(bundle); 
			    			    intent.setClass(DevicesActivity.this, LEDActivity.class);
			    				startActivity(intent);
			    				DevicesActivity.this.finish();
			    				break;
			    				
		                	case "DHT22":
		                		Intent intent2 = new Intent();
		                		Bundle bundle2 = new Bundle();
		                		intent2.putExtra("device", prefix);
		                		intent2.putExtras(bundle2);
		                		intent2.setClass(DevicesActivity.this, HumidityActivity.class);
		                		startActivity(intent2);
		                		DevicesActivity.this.finish();		                		
		                		break;
		                		
		                	case "camera":
		                		Intent intent3 = new Intent();
		                		Bundle bundle3 = new Bundle();
		                		intent3.putExtra("device", prefix);
		                		intent3.putExtras(bundle3);
		                		intent3.setClass(DevicesActivity.this, CameraActivity.class);
		                		startActivity(intent3);
		                		DevicesActivity.this.finish();	
		                		break;
		                		
			    			default:
			    		    	Toast.makeText(getApplicationContext(),
			    						"device not supported",
			    						Toast.LENGTH_SHORT).show();
			    		    	break;
		                	}		    						            
		                }  
		            });
				} catch (JSONException e) {   
					// TODO Auto-generated catch block
					Log.i(TAG, "fail to build json array.");
					e.printStackTrace();
				}				
				break;

			default:

				Toast.makeText(getApplicationContext(),
						"Fail to retrieve device list:" + msg.obj,
						Toast.LENGTH_SHORT).show(); 

				break;

			}

			if (_proDlg != null)
				_proDlg.dismiss();

		}

	};

}
