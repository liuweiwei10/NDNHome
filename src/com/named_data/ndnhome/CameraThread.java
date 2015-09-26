package com.named_data.ndnhome;

import java.util.HashMap;

import com.named_data.ndnhome.util.HmacHelper;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

public class CameraThread extends Thread {
	private static final String TAG = "NDNHome";
	private String devicePrefix;	
	private Handler actionHandler;
	private String msgStr = "Capture picture";
	private HmacHelper hmacHelper = null;
    private int segNo;
	private int packetNo; 
	private long timestamp;

	public CameraThread(String devicePrefix, Handler actionHandler, int segNo, int packetNo, long timestamp) {
		this.devicePrefix = devicePrefix;
		this.actionHandler = actionHandler;
		this.segNo = segNo;
		this.packetNo = packetNo;
		this.timestamp = timestamp;
	}

	@Override
	public void run() {

		try {				
				for(int i= 0; i< packetNo; i++) {
					Face face = new Face();
					// interest format: capture new picture: devicePrefix/capture/timestamp/segNo
					CameraTimer timer = new CameraTimer(actionHandler, hmacHelper, msgStr);
					String interestName = devicePrefix;
					Name name = new Name(interestName);
					name.append("capture");								   
				    name.appendTimestamp(timestamp);
				    name.appendSegment(segNo);				    
				    segNo++;
					Interest interest = new Interest(name);
					Log.i(TAG, "Express name " + interest.getName().toUri());
					timer.startUp();
					face.expressInterest(interest, timer, timer);
					
					// The main event loop.
					while (timer.callbackCount_ < 1) {
						face.processEvents();
					}
					face.shutdown();	
				}		
		}

		catch (Exception e) {
			Log.i(TAG, "exception: " + e.getMessage());
			Message msg = new Message();
			msg.what = -1;
			msg.obj = msgStr + " execption occurs.";
			actionHandler.sendMessage(msg);
			e.printStackTrace();
		}
	}
}

