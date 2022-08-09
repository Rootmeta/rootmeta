package com.meta.community.util;

import java.math.BigInteger;
import java.util.Arrays;

import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

public class Web3Util {
	public static boolean verifySignature(String address, String message, String signature) {
		boolean success = false;
		
		String msg = "\u0019Ethereum Signed Message:\n" + message.length() + message;
		byte[] msgHash = Hash.sha3(msg.getBytes());
		
		byte[] signBytes = Numeric.hexStringToByteArray(signature);
		byte v = signBytes[64];
		if (v < 27) {
			v += 27;
		}
		Sign.SignatureData signData = new Sign.SignatureData(v, Arrays.copyOfRange(signBytes, 0, 32), Arrays.copyOfRange(signBytes, 32, 64));
		
		for (int i = 0; i < 4; i++) {
			BigInteger publicKey = Sign.recoverFromSignature((byte) i, new ECDSASignature(new BigInteger(1, signData.getR()), new BigInteger(1, signData.getS())), msgHash);
			if (publicKey != null) {
				String addressRecovered = "0x" + Keys.getAddress(publicKey);
				if (addressRecovered.equals(address)) {
					success = true;
					break;
				}
			}
		}
		return success;
	}
}
