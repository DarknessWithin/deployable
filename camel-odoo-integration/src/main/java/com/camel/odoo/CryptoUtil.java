package com.camel.odoo;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class CryptoUtil {
    private static final String key = "ThisIsA16ByteKey"; // 16 characters = 128 bits

    public static String encrypt(String plainText) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encrypted) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decoded));
    }

    public static void main(String[] args) throws Exception {
        String apiKey = "ea61d89fc8d22c3";
        String apiSecret = "9d3355b4e12f548";
        String password = "Muqvar-kiqsec-5jogro";

        String encryptedKey = encrypt(apiKey);
        String encryptedSecret = encrypt(apiSecret);
        String encryptedPassword = encrypt(password);

        System.out.println("Encrypted API Key: " + encryptedKey);
        System.out.println("Encrypted API Secret: " + encryptedSecret);
        System.out.println("Encrypted Password: " + encryptedPassword);

        // Now test decryption
        System.out.println("Decrypted API Key: " + decrypt(encryptedKey));
        System.out.println("Decrypted API Secret: " + decrypt(encryptedSecret));
        System.out.println("Decrypted Password: " + decrypt(encryptedPassword));
    }
}
