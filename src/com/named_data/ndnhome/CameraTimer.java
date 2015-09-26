package com.named_data.ndnhome;


import com.named_data.ndnhome.util.HmacHelper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

public class CameraTimer implements OnData, OnTimeout {
	private static final String TAG = "NDNHome";

	private long startTime;
	private Handler actionHandler;
	private HmacHelper hmacHelper;
	private String msgStr;
	private int curSegNo;
	
	public CameraTimer(Handler actionHandler, HmacHelper hmacHelper, String msgStr) {
		this.actionHandler = actionHandler;
		this.hmacHelper = hmacHelper;
		this.msgStr = msgStr;
	}

	public void onData(Interest interest, Data data)

	{
		++callbackCount_;

		long elapsedTime = System.currentTimeMillis() - this.startTime;

		Log.i(TAG, "Got data packet with name " + data.getName().toUri() + ":" + String.valueOf(elapsedTime) + " ms") ;  
        //Log.i(TAG, "Verifying data:");

		try {
			Log.d(TAG, "seg no:" + String.valueOf(data.getName().get(6).toSegment())); 
			
	        curSegNo = (int) data.getName().get(6).toSegment();
	        int finalBlockId = Integer.parseInt(data.getMetaInfo().getFinalBlockId().getValue().toString());
	        //Log.d(TAG, data.getMetaInfo().getFinalBlockId().getValue().toString()); 
			
			
		   	// need to implement 'verifyData(data)'			
			boolean verifyResult = true;  //_hmacHelper.verifyData(data)
			
			if(!verifyResult){
				Log.i(TAG, "wrong signature");
				Message msg = new Message();
				msg.what = -1;	
				msg.obj = msgStr + " exception: fail to verify data";
				actionHandler.sendMessage(msg);
	
			} else {
				//Log.i(TAG, "verify data: succeed");
				Blob content = data.getContent();

				//Log.i(TAG, "Content " + content);

				// Send a result to Screen
				if(curSegNo == 0) {
		            if(content.size() > 0) {
		    			Message msg = new Message();
		    			msg.what = 200 + finalBlockId; // msg.what > 200, first segment is recieved  
		    			msg.obj =  content;

		    			actionHandler.sendMessage(msg);
		            } else {
		                Message msg = new Message();	                
		                msg.what = -1;
		                msg.obj = msgStr + " exception: empty data packet";
		                actionHandler.sendMessage(msg);
		            }
				} else  {
		            if(content.size() > 0) {
				    			Message msg = new Message();
				    			msg.what = curSegNo; // msg.what < 200, not the first segment
				    			msg.obj =  content;

				    			actionHandler.sendMessage(msg);
				            } else {
				                Message msg = new Message();	                
				                msg.what = -1;
				                msg.obj = msgStr + " exception: empty data packet";
				                actionHandler.sendMessage(msg);
				            }
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		    Message msg = new Message();        
            msg.what = -1;
            msg.obj= msgStr + "exception occurs";
            actionHandler.sendMessage(msg);
		}
	}

	public int callbackCount_ = 0;

	public void onTimeout(Interest interest)

	{

		++callbackCount_;
        Message msg = new Message();
        
        msg.what = -1; //time out
        msg.obj = "can't connect to controller: time out";
       
        actionHandler.sendMessage(msg);
		Log.i(TAG, "Time out for interest " + interest.getName().toUri());

	}

	public void startUp() {

		startTime = System.currentTimeMillis();

	}

}
