package com.named_data.ndnhome.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import android.util.Base64;
import android.util.Log;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.KeyLocatorType;
import net.named_data.jndn.Name;
import net.named_data.jndn.Sha256WithRsaSignature;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SignedBlob;

public class HmacHelper {
	private static final String TAG = "NDNHome";
	
	private byte[] key;
	private WireFormat wireFormat;

	public HmacHelper(byte[] key, WireFormat wireFormat) {
		this.key = key;
		this.wireFormat = wireFormat;
	}

	public HmacHelper(byte[] key) {
		this.key = key;
		wireFormat = WireFormat.getDefaultWireFormat();
	}

	public void signInterest(Interest interest, Name keyName) throws Exception {
		Name interestName = interest.getName();

		// generate value for nonce
		byte[] nonceValue = new byte[8];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(nonceValue);

		// generate value for timestamp
		Calendar calendar = Calendar.getInstance();
		Date now = calendar.getTime();
		Timestamp ts = new Timestamp(now.getTime());
		String timeStampValue = ts.toString();

		// append name
		interestName.append(nonceValue).append(timeStampValue);

		// use the key to sign interest
		Sha256WithRsaSignature s = new Sha256WithRsaSignature();
		s.getKeyLocator().setType(KeyLocatorType.KEYNAME);
		s.getKeyLocator().setKeyName(keyName);

		interestName.append(wireFormat.encodeSignatureInfo(s));
		interestName.append(new Name.Component());
		SignedBlob encoding = interest.wireEncode(wireFormat);
		Log.d(TAG, "encoded intrerest(before set name):" + Base64.encodeToString(encoding.getImmutableArray(), Base64.DEFAULT));
		Log.d(TAG, "interest name:" + interest.getName().toUri());
		byte[] signer = Util.hmacEncode(key, encoding.signedBuf());
		s.setSignature(new Blob(signer));
		interest.setName(interestName.getPrefix(-1).append(wireFormat.encodeSignatureValue(s)));

		SignedBlob encoding2 = interest.wireEncode(wireFormat);
		Log.d(TAG, "encoded intrerest(after set name) :" + Base64.encodeToString(encoding2.getImmutableArray(), Base64.DEFAULT));
		
		//return interest;
	}

	public void signInterest(Interest interest, Name keyName, WireFormat wf) throws Exception {
		Name interestName = interest.getName();

		// generate value for nonce
		byte[] nonceValue = new byte[8];
		SecureRandom rand = new SecureRandom();
		rand.nextBytes(nonceValue);

		// generate value for timestamp
		Calendar calendar = Calendar.getInstance();
		Date now = calendar.getTime();
		Timestamp ts = new Timestamp(now.getTime());
		String timeStampValue = ts.toString();

		// append name
		interestName.append(nonceValue).append(timeStampValue);

		// use the key to sign interest
		Sha256WithRsaSignature s = new Sha256WithRsaSignature();
		s.getKeyLocator().setType(KeyLocatorType.KEYNAME);
		s.getKeyLocator().setKeyName(keyName);

		interestName.append(wf.encodeSignatureInfo(s));
		interestName.append(new Name.Component());
		SignedBlob encoding = interest.wireEncode(wf);
		byte[] signer = Util.hmacEncode(key, encoding.signedBuf());
		s.setSignature(new Blob(signer));
		interest.setName(interestName.getPrefix(-1).append(wf.encodeSignatureValue(s)));		

		//return interest;
	}
	
	
	public boolean verifyData(Data data) throws Exception{
		//needs to debug
		
		SignedBlob encoded = data.wireEncode(wireFormat);	
	    byte[] hash = Util.hmacEncode(key, encoded.signedBuf());
	    Blob sigBytes = data.getSignature().getSignature();
	    Log.d(TAG, "hash computed:" + hash);
	   /* Log.d(TAG, "hash in data packet" + sigBytes);
	    Log.d(TAG, "hash computed to string :" + hash.toString());
	    Log.d(TAG, "hash in data packet to string " + sigBytes.toString());
	    Log.d(TAG, "hash in data packet to string " + sigBytes.toHex());*/
	    ByteBuffer byteBuffer  =sigBytes.buf();
        byte[] originalHash = new byte[byteBuffer.remaining()];
        byteBuffer.get(originalHash);
        Log.d(TAG, "hash in data packet:" + originalHash);
	    return originalHash.equals(hash);
		
	}

}
