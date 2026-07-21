
package com.meshstream.crypto

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SessionKeyManager {
    fun createSessionKey(seed: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = digest.digest(seed.toByteArray(StandardCharsets.UTF_8)).copyOf(16)

        val spec = PBEKeySpec(seed.toCharArray(), salt, 200_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = factory.generateSecret(spec).encoded
        return "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(key)}"
    }
}
