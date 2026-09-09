package me.moruto.steganography.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class Crypto {
    private static final int SALT_LEN = 16;
    private static final int IV_LEN = 12;
    private static final int KEY_LEN = 256;
    private static final int ITERATIONS = 210_000;
    private static final int TAG_LENGTH = 128;

    public static byte[] encrypt(byte[] data, String password) throws Exception {

        SecureRandom random = new SecureRandom();

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];

        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

        byte[] cipherText = cipher.doFinal(data);

        byte[] result = new byte[salt.length + iv.length + cipherText.length];

        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(iv, 0, result, salt.length, iv.length);
        System.arraycopy(cipherText, 0, result, salt.length + iv.length, cipherText.length);

        return result;
    }

    public static byte[] decrypt(byte[] data, String password) throws Exception {

        byte[] salt = new byte[SALT_LEN];
        byte[] iv = new byte[IV_LEN];

        System.arraycopy(data, 0, salt, 0, SALT_LEN);
        System.arraycopy(data, SALT_LEN, iv, 0, IV_LEN);

        byte[] cipherText = new byte[data.length - SALT_LEN - IV_LEN];
        System.arraycopy(data, SALT_LEN + IV_LEN, cipherText, 0, cipherText.length);

        SecretKey key = deriveKey(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));

        return cipher.doFinal(cipherText);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LEN
        );

        byte[] keyBytes = factory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(keyBytes, "AES");
    }
}