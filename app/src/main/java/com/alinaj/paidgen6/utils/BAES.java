package com.alinaj.paidgen6.utils;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2021/06/08
 */

public final class BAES {

    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final String CHARSET = "UTF-8";
    public static boolean DEBUG_LOG_ENABLED = false;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String TAG = "AESCrypt";
    private static final byte[] ivBytes = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static final String vars = "4paZ4paa4pab4pac4pad4pae4paf4paD4paE4paF4paG4paH4paI4paJ4paK4paQ";

    private static SecretKeySpec generateKey(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest instance = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] bytes = str.getBytes(CHARSET);
        instance.update(bytes, 0, bytes.length);
        byte[] digest = instance.digest();
        log("SHA-256 key ", digest);
        return new SecretKeySpec(digest, "AES");
    }

    public static String encrypt(String str2, String str3) throws GeneralSecurityException {
        String genUser = genUser();
        try {
            SecretKeySpec generateKey = generateKey(genUser + genKey(str2));
            log("message", str3);
            genUser = Base64.encodeToString(encrypt(generateKey, ivBytes, str3.getBytes(CHARSET)), 2);
            log("Base64.NO_WRAP", genUser);
            return genCode(genUser);
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED) {
                Log.e(TAG, "UnsupportedEncodingException ", e);
            }
            throw new GeneralSecurityException(e);
        }
    }

    public static byte[] encrypt(SecretKeySpec secretKeySpec, byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        Cipher instance = Cipher.getInstance(AES_MODE);
        instance.init(1, secretKeySpec, new IvParameterSpec(bArr));
        byte[] doFinal = instance.doFinal(bArr2);
        log("cipherText", doFinal);
        return doFinal;
    }

    public static String decrypt(String str2, String str3) throws GeneralSecurityException {
        String genString = genString(str3);
        String genUser = genUser();
        try {
            SecretKeySpec generateKey = generateKey(genUser + genKey(str2));
            log("base64EncodedCipherText", genString);
            byte[] decode = Base64.decode(genString, 2);
            log("decodedCipherText", decode);
            decode = decrypt(generateKey, ivBytes, decode);
            log("decryptedBytes", decode);
            genUser = new String(decode, CHARSET);
            log("message", genUser);
            return genUser;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED) {
                Log.e(TAG, "UnsupportedEncodingException ", e);
            }
            throw new GeneralSecurityException(e);
        }
    }

    public static byte[] decrypt(SecretKeySpec secretKeySpec, byte[] bArr, byte[] bArr2) throws GeneralSecurityException {
        Cipher instance = Cipher.getInstance(AES_MODE);
        instance.init(2, secretKeySpec, new IvParameterSpec(bArr));
        byte[] doFinal = instance.doFinal(bArr2);
        log("decryptedBytes", doFinal);
        return doFinal;
    }

    private static void log(String str, byte[] bArr) {
        if (DEBUG_LOG_ENABLED) {
            Log.d(TAG, str + "[" + bArr.length + "] [" + bytesToHex(bArr) + "]");
        }
    }

    private static void log(String str, String str2) {
        if (DEBUG_LOG_ENABLED) {
            Log.d(TAG, str + "[" + str2.length() + "] [" + str2 + "]");
        }
    }

    private static String bytesToHex(byte[] bArr) {
        char[] cArr = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] cArr2 = new char[(bArr.length * 2)];
        for (int i = 0; i < bArr.length; i++) {
            int i2 = bArr[i] & 255;
            cArr2[i * 2] = cArr[i2 >>> 4];
            cArr2[(i * 2) + 1] = cArr[i2 & 15];
        }
        return new String(cArr2);
    }

    BAES() {
    }

    public static String genUser() {
        return Base64.encodeToString((new Object() {
            int usr;
            public String toString() {
                byte[] buf = new byte[11];
                usr = -498989214;
                buf[0] = (byte) (usr >>> 11);
                usr = 1134795263;
                buf[1] = (byte) (usr >>> 19);
                usr = -1985105111;
                buf[2] = (byte) (usr >>> 3);
                usr = 1672562058;
                buf[3] = (byte) (usr >>> 24);
                usr = 1969199312;
                buf[4] = (byte) (usr >>> 1);
                usr = 216671972;
                buf[5] = (byte) (usr >>> 4);
                usr = -1830935578;
                buf[6] = (byte) (usr >>> 17);
                usr = 1640685350;
                buf[7] = (byte) (usr >>> 18);
                usr = 841697994;
                buf[8] = (byte) (usr >>> 1);
                usr = -528276816;
                buf[9] = (byte) (usr >>> 12);
                usr = -901558376;
                buf[10] = (byte) (usr >>> 12);
                return new String(buf);
            }
        }.toString()).getBytes(), 0);
    }

    public static String genKey(String str) {
        if (str != null) {
            return asHex(str.getBytes());
        }
        throw new NullPointerException();
    }

    private static String asHex(byte[] bArr) {
        char[] toCharArray = "0123456789ABCDEF".toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bArr) {
            stringBuilder.append(toCharArray[(b & 240) >> 4]);
            stringBuilder.append(toCharArray[b & 15]);
        }
        return stringBuilder.toString();
    }

    public static String genCode(String str) {
        if (str != null) {
            return gen(str.getBytes());
        }
        throw new NullPointerException();
    }

    private static String gen(byte[] bArr) {
        char[] toCharArray = var(vars).toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bArr) {
            stringBuilder.append(toCharArray[(b & 240) >> 4]);
            stringBuilder.append(toCharArray[b & 15]);
        }
        return stringBuilder.toString();
    }

    public static String var(String str) {
        return new String(Base64.decode(str, 0));
    }

    public static String genString(String str) {
        String var = var(vars);
        char[] toCharArray = str.toCharArray();
        byte[] bArr = new byte[(str.length() / 2)];
        for (int i = 0; i < bArr.length; i++) {
            bArr[i] = (byte) (((var.indexOf(toCharArray[i * 2]) * 16) + var.indexOf(toCharArray[(i * 2) + 1])) & 255);
        }
        return new String(bArr);
    }
}


