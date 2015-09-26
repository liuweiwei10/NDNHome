/*
 * Copyright (C) 2014 Regents of the University of California.
 * @author: Euihyun Jung <jung@anyang.ac.kr>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * A copy of the GNU Lesser General Public License is in the file COPYING.
 */

package com.named_data.ndnhome;

import java.util.Arrays;

import com.named_data.ndnhome.util.HmacHelper;
import com.named_data.ndnhome.util.Util;

import net.named_data.jndn.Data;

import net.named_data.jndn.Face;

import net.named_data.jndn.Interest;

import net.named_data.jndn.Name;

import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import android.app.Activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import android.os.Handler;

import android.os.Message;

import android.util.Log;

import android.view.View;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "NDNHome";
	
	private EditText _etUsername;
	private EditText _etPassword; 
	private Button _btnSignin;

	private ProgressDialog _proDlg;
	private String _username;
	private String _password;
    private	HmacHelper _hmacHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.sign_in);
		this._etUsername = (EditText) findViewById(R.id.et_username);
		this._etPassword = (EditText) findViewById(R.id.et_password);
		this._btnSignin = (Button) findViewById(R.id.btn_sign_in);

		this._btnSignin.setOnClickListener(btnClickListener);

	}

	// Button Click Event Listener

	View.OnClickListener btnClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			_username = _etUsername.getText().toString();
			_password = _etPassword.getText().toString();
			if (!(_username.trim().equals("") || _password.trim().equals(""))) {
				_proDlg = ProgressDialog.show(MainActivity.this, "", "waiting...");
				NetThread thread = new NetThread(_username, _password);
				thread.start();
			
			} else {
				Toast.makeText(getApplicationContext(),
						"Please enter valid username and password.",
						Toast.LENGTH_SHORT).show();
			}
			
		}

	};

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
					
		            if( contentStr.equalsIgnoreCase("success")) {
		    			Message msg = new Message();

		    			msg.what = 200; // Result Code ex) Success code: 200 , Fail Code:
		    							// 400 ...
		    			msg.obj = String.valueOf(elapsedTime) + "ms"; // Result Object

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

				String interestName = "/home/controller/login/";
				Name name = new Name(interestName);
				name.append(username);	
				//Log.d(TAG, "password:" + password);
				byte[] key =  Util.hash256(password);
				
				//Log.d(TAG, "key:" + Arrays.toString(key));
				
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
	            
	            msg.what = 401; //time out
	            msg.obj = "execption occurs.";
	            actionHandler.sendMessage(msg);

				e.printStackTrace();

			}

		}

	}

	// UI controller

	private Handler actionHandler = new Handler() {

		public void handleMessage(Message msg) {

			switch (msg.what) { // Result Code

			case 200: // Result Code Ex) Success: 200
			    SharedPreferences sp = getSharedPreferences("account",0);
	    		SharedPreferences.Editor editor = sp.edit();
	    		editor.putBoolean("isSignin", true);
	    	    editor.putString("username",_username);
	    		editor.putString("password",_password);
	    		editor.commit();
				Toast.makeText(getApplicationContext(), "Redirecting...", 
				Toast.LENGTH_SHORT).show();					  					  
				Intent intent = new Intent();
			    intent.setClass(MainActivity.this, DevicesActivity.class);
				startActivity(intent);
				MainActivity.this.finish();

				break;

			default:

				Toast.makeText(getApplicationContext(),
						"Fail to login:" + msg.obj,
						Toast.LENGTH_SHORT).show();

				break;

			}

			if (_proDlg != null)
				_proDlg.dismiss();

		}

	};

}
