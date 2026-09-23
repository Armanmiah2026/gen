package com.alinaj.paidgen6.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @developer_ Name: Rubel Brand Name: rksoft, https://t.me/rksoft_update
 * @date 2021/06/22
 */

public final class TeaBase64 {
    public static String TAG = "OVPN3Gen";
    public static final int DELTA = 0x9E3779B9;

    private static int MX(int sum, int y, int z, int p, int e, int[] k) {
        return (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
    }

    private TeaBase64() {}

    public static byte[] encrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        return toByteArray(encrypt(toIntArray(data, true), toIntArray(fixKey(key), false)), false);
    }

    public static byte[] encrypt(String data, byte[] key) {
        return encrypt(data.getBytes(StandardCharsets.UTF_8), key);
    }

    public static byte[] encrypt(byte[] data, String key) {
        return encrypt(data, key.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] encrypt(String data, String key) {
        return encrypt(data.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
    }

    public static String encryptToBase64String(byte[] data, byte[] key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return encode(bytes);
    }

    public static String encryptToBase64String(String data, byte[] key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return encode(bytes);
    }

    public static String encryptToBase64String(byte[] data, String key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return encode(bytes);
    }

    public static String encryptToBase64String(String data, String key) {
        byte[] bytes = encrypt(data, key);
        if (bytes == null) return null;
        return encode(bytes);
    }

    public static byte[] decrypt(byte[] data, byte[] key) {
        if (data.length == 0) {
            return data;
        }
        return toByteArray(
            decrypt(toIntArray(data, false), toIntArray(fixKey(key), false)), true);
    }

    public static byte[] decrypt(byte[] data, String key) {
        return decrypt(data, key.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] decryptBase64String(String data, byte[] key) {
        return decrypt(decode(data), key);
    }

    public static byte[] decryptBase64String(String data, String key) {
        return decrypt(decode(data), key);
    }

    public static String decryptToString(byte[] data, byte[] key) {
        byte[] bytes = decrypt(data, key);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String decryptToString(byte[] data, String key) {
        byte[] bytes = decrypt(data, key);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String decryptBase64StringToString(String data, byte[] key) {
        byte[] bytes = decrypt(decode(data), key);
        if (bytes == null) return null;
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String decryptBase64StringToString(String data, String key) {
        byte[] bytes = decrypt(decode(data), key);
        if (bytes == null) return "";
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static int[] encrypt(int[] v, int[] k) {
        int n = v.length - 1;


        if (n < 1) {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z = v[n], y, sum = 0, e;


        while (q-- > 0) {
            sum = sum + DELTA;
            e = sum >>> 2 & 3;
            for (p = 0; p < n; p++) {
                y = v[p + 1];
                z = v[p] += MX(sum, y, z, p, e, k);
            }
            y = v[0];
            z = v[n] += MX(sum, y, z, p, e, k);
        }
        return v;
    }


    private static int[] decrypt(int[] v, int[] k) {
        int n = v.length - 1;


        if (n < 1) {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z, y = v[0], sum = q * DELTA, e;


        while (sum != 0) {
            e = sum >>> 2 & 3;
            for (p = n; p > 0; p--) {
                z = v[p - 1];
                y = v[p] -= MX(sum, y, z, p, e, k);
            }
            z = v[n];
            y = v[0] -= MX(sum, y, z, p, e, k);
            sum = sum - DELTA;
        }
        return v;
    }


    private static byte[] fixKey(byte[] key) {
        if (key.length == 16) return key;
        byte[] fixedkey = new byte[16];
        if (key.length < 16) {
            System.arraycopy(key, 0, fixedkey, 0, key.length);
        }
        else {
            System.arraycopy(key, 0, fixedkey, 0, 16);
        }
        return fixedkey;
    }


    private static int[] toIntArray(byte[] data, boolean includeLength) {
        int n = (((data.length & 3) == 0)
            ? (data.length >>> 2)
            : ((data.length >>> 2) + 1));
        int[] result;


        if (includeLength) {
            result = new int[n + 1];
            result[n] = data.length;
        }
        else {
            result = new int[n];
        }
        n = data.length;
        for (int i = 0; i < n; ++i) {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }


    private static byte[] toByteArray(int[] data, boolean includeLength) {
        int n = data.length << 2;


        if (includeLength) {
            int m = data[data.length - 1];
            n -= 4;
            if ((m < n - 3) || (m > n)) {
                return null;
            }
            n = m;
        }
        byte[] result = new byte[n];


        for (int i = 0; i < n; ++i) {
            result[i] = (byte) (data[i >>> 2] >>> ((i & 3) << 3));
        }
        return result;
    }


    public static String encode(byte[] data) {
        final char[] base64EncodeChars = new char[] {
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
                'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
                'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
                'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
                'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
                'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
                'w', 'x', 'y', 'z', '0', '1', '2', '3',
                '4', '5', '6', '7', '8', '9', '+', '/' };
        StringBuilder sb = new StringBuilder();
        int r = data.length % 3;
        int len = data.length - r;
        int i = 0;
        int c;
        while (i < len) {
            c = (0x000000ff & data[i++]) << 16 |
                    (0x000000ff & data[i++]) << 8  |
                    (0x000000ff & data[i++]);
            sb.append(base64EncodeChars[c >> 18]);
            sb.append(base64EncodeChars[c >> 12 & 0x3f]);
            sb.append(base64EncodeChars[c >> 6  & 0x3f]);
            sb.append(base64EncodeChars[c & 0x3f]);
        }
        if (r == 1) {
            c = 0x000000ff & data[i++];
            sb.append(base64EncodeChars[c >> 2]);
            sb.append(base64EncodeChars[(c & 0x03) << 4]);
            sb.append("==");
        }
        else if (r == 2) {
            c = (0x000000ff & data[i++]) << 8 |
                    (0x000000ff & data[i++]);
            sb.append(base64EncodeChars[c >> 10]);
            sb.append(base64EncodeChars[c >> 4 & 0x3f]);
            sb.append(base64EncodeChars[(c & 0x0f) << 2]);
            sb.append("=");
        }
        return sb.toString();
    }

    public static byte[] decode(String str) {
        final byte[] base64DecodeChars = new byte[] {
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
                52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
                -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
                15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
                -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
                41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };
        byte[] data = str.getBytes();
        int len = data.length;
        ByteArrayOutputStream buf = new ByteArrayOutputStream(len);
        int i = 0;
        int b1, b2, b3, b4;
        while (i < len) {
            do {
                b1 = base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1) {
                break;
            }
            do {
                b2 = base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1) {
                break;
            }
            buf.write(((b1 << 2) | ((b2 & 0x30) >>> 4)));
            do {
                b3 = data[i++];
                if (b3 == 61) {
                    return buf.toByteArray();
                }
                b3 = base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1) {
                break;
            }
            buf.write((((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));
            do {
                b4 = data[i++];
                if (b4 == 61) {
                    return buf.toByteArray();
                }
                b4 = base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1) {
                break;
            }
            buf.write((((b3 & 0x03) << 6) | b4));
        }
        return buf.toByteArray();
    }
}



