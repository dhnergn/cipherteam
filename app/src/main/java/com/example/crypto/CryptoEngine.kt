package com.example.crypto

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertext: String,      // Base64 of (IV + AES-encrypted message)
    val encryptedAesKey: String  // Base64 of RSA-encrypted AES key
)

object CryptoEngine {

    private const val RSA_ALGORITHM = "RSA"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_ALGORITHM = "AES"
    private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 16

    // Audit logs that the UI can subscribe to
    private val _auditLogs = mutableListOf<String>()
    val auditLogs: List<String> get() = _auditLogs

    fun logAudit(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        _auditLogs.add("[$timestamp] $message")
        if (_auditLogs.size > 200) {
            _auditLogs.removeAt(0)
        }
    }

    /**
     * Generate a new RSA 2048-bit Key Pair.
     */
    fun generateKeyPair(): KeyPair {
        logAudit("Generating RSA-2048 key pair...")
        val kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()
        logAudit("RSA-2048 key pair generated successfully.")
        return keyPair
    }

    /**
     * Convert PublicKey to PEM string.
     */
    fun publicKeyToPem(publicKey: PublicKey): String {
        val encoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PUBLIC KEY-----\n" +
                encoded.chunked(64).joinToString("\n") +
                "\n-----END PUBLIC KEY-----"
    }

    /**
     * Convert PrivateKey to PEM string.
     */
    fun privateKeyToPem(privateKey: PrivateKey): String {
        val encoded = Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
        return "-----BEGIN PRIVATE KEY-----\n" +
                encoded.chunked(64).joinToString("\n") +
                "\n-----END PRIVATE KEY-----"
    }

    /**
     * Parse Public Key from PEM string.
     */
    fun pemToPublicKey(pem: String): PublicKey {
        val cleanPem = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance(RSA_ALGORITHM)
        return kf.generatePublic(spec)
    }

    /**
     * Parse Private Key from PEM string.
     */
    fun pemToPrivateKey(pem: String): PrivateKey {
        val cleanPem = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val decoded = Base64.decode(cleanPem, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(decoded)
        val kf = KeyFactory.getInstance(RSA_ALGORITHM)
        return kf.generatePrivate(spec)
    }

    /**
     * Hybrid Encryption: Encrypts message with AES-256 and encrypts the AES key with RSA Public Key.
     */
    fun encrypt(message: String, recipientPublicKeyPem: String): EncryptedPayload {
        try {
            logAudit("Starting Hybrid Encryption for message: \"${message.take(20)}${if (message.length > 20) "..." else ""}\"")
            
            // 1. Generate ephemeral AES-256 key
            logAudit("Generating ephemeral AES-256 symmetric key...")
            val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
            keyGen.init(256)
            val aesKey = keyGen.generateKey()
            logAudit("Symmetric AES key generated: 0x${Base64.encodeToString(aesKey.encoded.take(6).toByteArray(), Base64.NO_WRAP).take(12)}... (Truncated)")

            // 2. Generate random 16-byte IV
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            // 3. Encrypt message with AES-256-CBC
            logAudit("Encrypting message bytes with AES-256-CBC and dynamic IV...")
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec)
            val encryptedBytes = aesCipher.doFinal(message.toByteArray(Charsets.UTF_8))
            
            // Prepend IV to encrypted bytes: [IV (16 bytes)][Encrypted Bytes]
            val ivAndCiphertext = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, ivAndCiphertext, iv.size, encryptedBytes.size)
            
            val base64Ciphertext = Base64.encodeToString(ivAndCiphertext, Base64.NO_WRAP)
            logAudit("Message encrypted. Total Ciphertext size (Base64): ${base64Ciphertext.length} chars.")

            // 4. Encrypt AES key with Recipient's RSA Public Key
            logAudit("Parsing recipient RSA public key from PEM...")
            val publicKey = pemToPublicKey(recipientPublicKeyPem)
            
            logAudit("Encrypting symmetric AES key using RSA-2048 (PKCS1Padding)...")
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey)
            val encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.encoded)
            val base64EncryptedAesKey = Base64.encodeToString(encryptedAesKeyBytes, Base64.NO_WRAP)
            logAudit("AES session key encapsulated successfully.")

            return EncryptedPayload(
                ciphertext = base64Ciphertext,
                encryptedAesKey = base64EncryptedAesKey
            )
        } catch (e: Exception) {
            logAudit("CRITICAL ENCRYPTION ERROR: ${e.message}")
            throw e
        }
    }

    /**
     * Hybrid Decryption: Decrypts the AES key using recipient's RSA Private Key, then decrypts message.
     */
    fun decrypt(payload: EncryptedPayload, recipientPrivateKeyPem: String): String {
        try {
            logAudit("Starting Hybrid Decryption. Ciphertext length: ${payload.ciphertext.length}")

            // 1. Decrypt AES key using Private RSA Key
            logAudit("Parsing local RSA private key from PEM...")
            val privateKey = pemToPrivateKey(recipientPrivateKeyPem)

            logAudit("Decrypting encapsulated AES key using RSA-2048...")
            val rsaCipher = Cipher.getInstance(RSA_TRANSFORMATION)
            rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
            
            val encryptedAesKeyBytes = Base64.decode(payload.encryptedAesKey, Base64.NO_WRAP)
            val aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes)
            val aesKey = SecretKeySpec(aesKeyBytes, AES_ALGORITHM)
            logAudit("Symmetric AES key recovered: 0x${Base64.encodeToString(aesKeyBytes.take(6).toByteArray(), Base64.NO_WRAP).take(12)}... (Truncated)")

            // 2. Extract IV and encrypted bytes from payload
            logAudit("Extracting initialization vector (IV) and encrypted message payload...")
            val ivAndCiphertext = Base64.decode(payload.ciphertext, Base64.NO_WRAP)
            if (ivAndCiphertext.size < IV_SIZE) {
                throw IllegalArgumentException("Ciphertext is too short to contain IV")
            }
            
            val iv = ByteArray(IV_SIZE)
            val encryptedBytes = ByteArray(ivAndCiphertext.size - IV_SIZE)
            
            System.arraycopy(ivAndCiphertext, 0, iv, 0, IV_SIZE)
            System.arraycopy(ivAndCiphertext, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
            val ivSpec = IvParameterSpec(iv)

            // 3. Decrypt message with AES-256-CBC
            logAudit("Decrypting payload bytes with AES-256-CBC...")
            val aesCipher = Cipher.getInstance(AES_TRANSFORMATION)
            aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec)
            val decryptedBytes = aesCipher.doFinal(encryptedBytes)
            val plainText = String(decryptedBytes, Charsets.UTF_8)
            logAudit("Payload successfully decrypted. Plaintext length: ${plainText.length}")

            return plainText
        } catch (e: Exception) {
            logAudit("CRITICAL DECRYPTION ERROR: ${e.message}")
            throw e
        }
    }
}
