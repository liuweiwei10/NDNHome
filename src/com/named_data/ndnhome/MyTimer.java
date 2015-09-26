package com.named_data.ndnhome;

import com.named_data.ndnhome.util.HmacHelper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

public class MyTimer implements OnData, OnTimeout {
	private static final String TAG = "NDNHome";

	private long startTime;
	private Handler actionHandler;
	private HmacHelper hmacHelper;
	private String msgStr;
	private int code;
	
	public MyTimer(Handler actionHandler, HmacHelper hmacHelper, String msgStr, int code) {
		this.actionHandler = actionHandler;
		this.hmacHelper = hmacHelper;
		this.msgStr = msgStr;
		this.code = code;
	}

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
				msg.what = 400 + code;	
				msg.obj = msgStr + " exception: fail to verify data";
				actionHandler.sendMessage(msg);
	
			} else {
				Log.i(TAG, "verify data: succeed");
				String contentStr = data.getContent().toString();

				Log.i(TAG, "Content " + contentStr);

				// Send a result to Screen
				
	            if( contentStr.length() > 0) {
	    			Message msg = new Message();

	    			msg.what = 200 + code; // Result Code ex) Success code: 200 , Fail Code:
	    							// 400 ...
	    			msg.obj =  contentStr;

	    			actionHandler.sendMessage(msg);
	            } else {
	                Message msg = new Message();	                
	                msg.what = 400+code;
	                msg.obj = msgStr + " exception: empty data packet";
	                actionHandler.sendMessage(msg);
	            }
			}
		} catch (Exception e) {
			e.printStackTrace();
		    Message msg = new Message();        
            msg.what = 400 + code;
            msg.obj= msgStr + "exception occurs";
            actionHandler.sendMessage(msg);
		}
	}

	public int callbackCount_ = 0;

	public void onTimeout(Interest interest)

	{

		++callbackCount_;
        Message msg = new Message();
        
        msg.what = 400+ code; //time out
        msg.obj = "can't connect to controller: time out";
       
        actionHandler.sendMessage(msg);
		Log.i(TAG, "Time out for interest " + interest.getName().toUri());

	}

	public void startUp() {

		startTime = System.currentTimeMillis();

	}

}
