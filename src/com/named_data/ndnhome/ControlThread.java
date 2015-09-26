package com.named_data.ndnhome;

import java.util.HashMap;

import com.named_data.ndnhome.util.HmacHelper;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

public class ControlThread extends Thread {
	private static final String TAG = "NDNHome";
	private String devicePrefix;
	private AccessTokenInfo accessTokenInfo;
	private HashMap<String, String> commandMsgPair;
	private int code;
	private Handler actionHandler;
	private String msgStr = "";

	public ControlThread(String devicePrefix, AccessTokenInfo accessTokenInfo, Handler actionHandler,
			HashMap<String, String> commandMsgPair, int code) {
		this.devicePrefix = devicePrefix;
		this.accessTokenInfo = accessTokenInfo;
		this.commandMsgPair = commandMsgPair;
		this.code = code;
		this.actionHandler = actionHandler;
	}

	@Override
	public void run() {
		Face face = new Face();
		try {
			String commandName = accessTokenInfo.getCommandName();
			int commandTokenSeq = accessTokenInfo.getCommandTokenSequence();
			int seedSeq = accessTokenInfo.getSeedSequence();
			byte[] accessToken = accessTokenInfo.getAccessToken();
			String accessTokenName = accessTokenInfo.getAccessTokenName();

			/*
			 * String commandTokenName = "/home/sensor/LED/1/" + commandName +
			 * "/token/0"; //byte[] seedKey = Util.hash256("seed"); byte[]
			 * commandKey = Util.hmacEncode("seed".getBytes(),
			 * commandTokenName); byte[] accessToken2 =
			 * Util.hmacEncode(commandKey, accessTokenName);
			 */

			Log.d(TAG, "commandName:" + commandName);
			Log.d(TAG, "accessTokenName: " + accessTokenName);
			Log.d(TAG, "accessToken:" + Base64.encodeToString(accessToken, Base64.DEFAULT));

			for (HashMap.Entry<String, String> entry : commandMsgPair.entrySet()) {
				if (commandName.equals(entry.getKey())) {
					msgStr = entry.getValue();
				}
			}

			if (msgStr.isEmpty()) {
				Log.i(TAG, "command not support");
				Message msg = new Message();

				msg.what = 400 + code;
				msg.obj = msgStr + " command not support.";
				actionHandler.sendMessage(msg);
			} else {
				HmacHelper hmacHelper = new HmacHelper(accessToken);

				MyTimer timer = new MyTimer(actionHandler, hmacHelper, msgStr, code);

				// interest format: devicePrefix/commandName/seedSeq/commandSeq
				String interestName = devicePrefix;
				Name name = new Name(interestName);
				name.append(commandName);
				name.append(String.valueOf(seedSeq)).append(String.valueOf(commandTokenSeq));

				Name keyName = new Name(accessTokenName);
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
		}

		catch (Exception e) {
			face.shutdown();
			Log.i(TAG, "exception: " + e.getMessage());
			Message msg = new Message();

			msg.what = 400 + code;
			msg.obj = msgStr + " execption occurs.";
			actionHandler.sendMessage(msg);
			e.printStackTrace();
		}
	}
}
