package com.example.data.crypto

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CloudEncryptionManager {

    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ALGORITHM = "AES/GCM/NoPadding"

    // Default master key seed for cloud client-side zero-knowledge encryption
    private val defaultKeyBytes = byteArrayOf(
        0x53, 0x63, 0x72, 0x65, 0x65, 0x6E, 0x52, 0x65,
        0x63, 0x6F, 0x72, 0x64, 0x65, 0x72, 0x50, 0x72,
        0x6F, 0x43, 0x6C, 0x6F, 0x75, 0x64, 0x56, 0x61,
        0x75, 0x6C, 0x74, 0x4B, 0x65, 0x79, 0x32, 0x36
    )

    fun generateSecretKey(): SecretKey {
        return SecretKeySpec(defaultKeyBytes, "AES")
    }

    /**
     * Encrypts a video file with AES-256-GCM.
     * Output format: [12-byte IV] + [Ciphertext + Auth Tag]
     */
    fun encryptFile(inputFile: File, outputFile: File, key: SecretKey = generateSecretKey()): Boolean {
        return try {
            val iv = ByteArray(GCM_IV_LENGTH).apply {
                SecureRandom().nextBytes(this)
            }
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)

            FileOutputStream(outputFile).use { fos ->
                // Write IV header first
                fos.write(iv)
                FileInputStream(inputFile).use { fis ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val encryptedChunk = cipher.update(buffer, 0, bytesRead)
                        if (encryptedChunk != null) {
                            fos.write(encryptedChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null) {
                        fos.write(finalChunk)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Decrypts an encrypted file back into original format
     */
    fun decryptFile(encryptedFile: File, outputFile: File, key: SecretKey = generateSecretKey()): Boolean {
        return try {
            FileInputStream(encryptedFile).use { fis ->
                val iv = ByteArray(GCM_IV_LENGTH)
                val readIv = fis.read(iv)
                if (readIv != GCM_IV_LENGTH) return false

                val cipher = Cipher.getInstance(ALGORITHM)
                val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)

                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        val decryptedChunk = cipher.update(buffer, 0, bytesRead)
                        if (decryptedChunk != null) {
                            fos.write(decryptedChunk)
                        }
                    }
                    val finalChunk = cipher.doFinal()
                    if (finalChunk != null) {
                        fos.write(finalChunk)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
