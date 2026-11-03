package com.pe.pivot.account.infrastructure.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic component responsible for SHA-256 blind indexing and AES-256 encryption
 * using the secret key injected from HashiCorp Vault (SRP).
 * It is responsable for:
 *   - Hashing sensitive data (like account numbers) using SHA-256 for blind indexing.
 *   - Encrypting and decrypting sensitive data at rest using AES-256.
 *   - Ensuring that the AES secret key is securely injected from Vault and not hardcoded
 *   - Providing utility methods for cryptographic operations in a secure manner.
 */
@Component
public final class CryptoUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    // 32-byte key seed for AES-256 encryption at rest
    private final SecretKeySpec secretKeySpec;

    /**
     * Injects the AES-256 secret key directly from HashiCorp Vault (secret/pivot-bank).
     *
     * @param aesSecretKey secret key stored in Vault
     */
    public CryptoUtil(@Value("${aes-secret-key}") String aesSecretKey) {
        if (aesSecretKey == null || aesSecretKey.isBlank()) {
            throw new IllegalStateException(
                    "The AES secret key is missing. Configure aes-secret-key in HashiCorp Vault.");
        }
        this.secretKeySpec = buildAes256Key(aesSecretKey);
    }

    /**
     * Builds a SecretKeySpec for AES-256 encryption from the provided key string.
     *
     * @param input - the raw key string
     * @return - SecretKeySpec for AES-256
     */
    public String hashSha256(String input) {
        // Hashes the input using SHA-256 and returns the hex representation
        if (input == null) {
            return null;
        }

        // Hash the input using SHA-256 and return the hex representation
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] encodedHash = digest.digest(input.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedHash);
        } catch (Exception ex) {
            throw new IllegalStateException("Error hashing data with SHA-256", ex);
        }
    }

    /**
     * Encrypts sensitive plain text using AES-256 before storing in MongoDB.
     *
     * @param plainText - the sensitive data to encrypt
     * @return - the AES-256 encrypted string
     */
    public String encryptAes256(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, this.secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, payload, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Error encrypting data with AES-256", ex);
        }
    }

    /**
     * Decrypts a string encrypted with AES-256.
     *
     * @param cipherTest the encrypted string
     * @return the decrypted string
     */
    public String decryptAes256(String cipherTest) {
        // Verify if the input is null or empty
        if (cipherTest == null) {
            return null;
        }

        // Decrypt the input using AES-256 and return the plaintext
        try {
            byte[] payload = Base64.getDecoder().decode(cipherTest);
            if (payload.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted payload");
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, GCM_IV_LENGTH);
            byte[] encryptedBytes = Arrays.copyOfRange(payload, GCM_IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, this.secretKeySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Error decrypting data with AES-256", ex);
        }
    }

    /**
     * Builds a SecretKeySpec for AES-256 encryption from the provided raw key string.
     *
     * @param rawKey - the raw key string
     * @return - SecretKeySpec for AES-256
     * @throws IllegalStateException if the SHA-256 algorithm is not available
     */
    private SecretKeySpec buildAes256Key(String rawKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance(SHA_256_ALGORITHM);
            byte[] keyBytes = sha.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            keyBytes = Arrays.copyOf(keyBytes, 32);
            return new SecretKeySpec(keyBytes, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Error initializing AES-256 key", ex);
        }
    }
}
