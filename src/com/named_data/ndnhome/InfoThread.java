package com.named_data.ndnhome;

import com.named_data.ndnhome.util.HmacHelper;
import com.named_data.ndnhome.util.Util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

public class InfoThread extends Thread {
    private static final String TAG="NDNHome";
    private static final String INFO_MSG = "Retrieve access info";
	private String username;
	private String password;
	private String deviceName;
	private Handler actionHandler;
	private int code;

	public InfoThread(String username, String password, String deviceName, Handler actionHandler, int code) {
		this.username = username;
		this.password = password;
		this.deviceName = deviceName;
		this.actionHandler = actionHandler;

	    this.code = code;
	}

	@Override
	public void run() {
		Face face = new Face();
		try {

			byte[] key = Util.hash256(password);
			HmacHelper hmacHelper = new HmacHelper(key);
			MyTimer timer = new MyTimer(actionHandler, hmacHelper, INFO_MSG, 0);

			// interest format:
			// /home/controller/<deviceName>/user/<username>/accessTokenList
			String interestName = "/home/controller" + deviceName;
			Name name = new Name(interestName);
			name.append("user");
			name.append(username).append("accessTokenList");

			Name keyName = new Name("user_key");
			Interest interest = new Interest(name);

			hmacHelper.signInterest(interest, keyName);

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

			msg.what = 400;
			msg.obj = INFO_MSG + " execption occurs.";
			actionHandler.sendMessage(msg);
			e.printStackTrace();
		}
	}
}
