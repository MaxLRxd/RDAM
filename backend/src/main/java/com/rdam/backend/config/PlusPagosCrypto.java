package com.rdam.backend.config;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encriptación AES-256-CBC compatible con el módulo CryptoJS del mock PlusPagos.
 *
 * Formato del texto encriptado: Base64( IV_16_bytes + AES_Ciphertext )
 *
 * La clave se deriva con SHA-256 del secretKey string (igual que CryptoJS.SHA256).
 * El IV es aleatorio de 16 bytes por cada encriptación.
 * Padding: PKCS5 (equivalente a PKCS7 en bloques de 16 bytes).
 *
 * Compatible con crypto.js del mock:
 *   encryptString(plainText, secretKey) → Base64(IV + Ciphertext)
 *   decryptString(encryptedText, secretKey) → plainText
 */
public class PlusPagosCrypto {

    private static final String ALGORITMO     = "AES/CBC/PKCS5Padding";
    private static final String TIPO_CLAVE    = "AES";
    private static final String HASH_DERIVACION = "SHA-256";

    // Clase utilitaria: no se instancia
    private PlusPagosCrypto() {}

    /**
     * Encripta un texto plano con AES-256-CBC.
     *
     * Equivalente a encryptString(plainText, secretKey) en crypto.js.
     *
     * @param plainText Texto a encriptar.
     * @param secretKey Clave secreta compartida con el mock.
     * @return Base64( IV_16_bytes + AES_Ciphertext )
     */
    public static String encrypt(String plainText, String secretKey) throws Exception {
        // Derivar clave de 256 bits con SHA-256 (igual que CryptoJS.SHA256)
        byte[] keyBytes = deriveKey(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, TIPO_CLAVE);

        // IV aleatorio de 16 bytes
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // Encriptar
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] ciphertext = cipher.doFinal(
            plainText.getBytes(StandardCharsets.UTF_8)
        );

        // Combinar IV + ciphertext y encodear en Base64
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv,         0, combined, 0,          iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length,  ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Desencripta un texto encriptado con encrypt().
     *
     * Equivalente a decryptString(encryptedText, secretKey) en crypto.js.
     *
     * @param encryptedText Base64( IV_16_bytes + AES_Ciphertext )
     * @param secretKey     Clave secreta compartida con el mock.
     * @return Texto plano original.
     */
    public static String decrypt(String encryptedText, String secretKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedText);

        // Extraer IV (primeros 16 bytes)
        byte[] iv         = new byte[16];
        byte[] ciphertext = new byte[combined.length - 16];
        System.arraycopy(combined, 0,  iv,         0, 16);
        System.arraycopy(combined, 16, ciphertext, 0, ciphertext.length);

        byte[] keyBytes = deriveKey(secretKey);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, TIPO_CLAVE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] plainBytes = cipher.doFinal(ciphertext);

        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * Deriva una clave AES de 256 bits aplicando SHA-256 al secretKey.
     * Replica el comportamiento de CryptoJS.SHA256(secretKey) como clave.
     */
    private static byte[] deriveKey(String secretKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_DERIVACION);
        return digest.digest(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}