package com.bitmechanic.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;

/**
 * Created by James Cooper <james@bitmechanic.com>
 * Date: Jan 9, 2010
 */
public class DesCipher {

    Cipher ecipher;

    Cipher dcipher;

    // 8-byte Salt
    private byte[] salt = {(byte) 0xA3, (byte) 0x9B, (byte) 0xC8, (byte) 0x22,
            (byte) 0x56, (byte) 0x75, (byte) 0xE3, (byte) 0x03};

    private static final int iterationCount = 20;

    public DesCipher(String passPhrase) {
        try {
            // Create the key
            KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt,
                    iterationCount);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
                    .generateSecret(keySpec);
            ecipher = Cipher.getInstance(key.getAlgorithm());
            dcipher = Cipher.getInstance(key.getAlgorithm());

            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt,
                    iterationCount);

            // Create the ciphers
            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized String encryptBase64(String str) {
        byte enc[] = encrypt(str);

        // Encode bytes to base64 to get a string
        return Base64.encodeBase64String(enc);
    }

    public synchronized String encryptHex(String str) {
        byte enc[] = encrypt(str);

        // Encode bytes to base64 to get a string
        return new String(Hex.encodeHex(enc));
    }

    private synchronized byte[] encrypt(String str) {
        try {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");

            // Encrypt
            byte[] enc = ecipher.doFinal(utf8);

            return enc;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized String decryptBase64(String str) {
        return decrypt(Base64.decodeBase64(str));
    }

    public synchronized String decryptHex(String str) throws DecoderException {
        return decrypt(Hex.decodeHex(str.toCharArray()));
    }

    private synchronized String decrypt(byte dec[]) {
        try {
            // Decrypt
            byte[] utf8 = dcipher.doFinal(dec);

            // Decode using utf-8
            return new String(utf8, "UTF8");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized byte[] encryptBytes(byte[] input) {
        try {
            return ecipher.doFinal(input);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized byte[] decryptBytes(byte[] input) {
        try {
            return dcipher.doFinal(input);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
