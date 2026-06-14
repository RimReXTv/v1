package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.crypto.AetherisCore
import com.example.data.*
import com.example.protocol.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `test app name string resource`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aetheris", appName)
    }

    @Test
    fun `test mnemonic generation lengths`() {
        // Phase 1.2: 12-word, 18-word, and 24-word checks
        val mnemonic12 = AetherisCore.generateMnemonic(12)
        assertEquals(12, mnemonic12.split(" ").size)

        val mnemonic18 = AetherisCore.generateMnemonic(18)
        assertEquals(18, mnemonic18.split(" ").size)

        val mnemonic24 = AetherisCore.generateMnemonic(24)
        assertEquals(24, mnemonic24.split(" ").size)
    }

    @Test
    fun `test seed words validation`() {
        // Valid mnemonic should pass
        val validMnemonic = AetherisCore.generateMnemonic(12)
        assertTrue(AetherisCore.validateMnemonic(validMnemonic))

        // Mismatched lengths should fail
        assertFalse(AetherisCore.validateMnemonic("abandon ability able"))

        // Unrecognized word should fail
        assertFalse(AetherisCore.validateMnemonic("abandon ability able about above absent absorb abstract absurd abuse access invalidword"))
    }

    @Test
    fun `test derive Bech32 address`() {
        val mnemonic = AetherisCore.generateMnemonic(12)
        val address = AetherisCore.deriveAddress(mnemonic)

        // Bech32 addresses must start with "aet1" prefix
        assertTrue(address.startsWith("aet1"))

        // Decode must be successful and reflect "aet" human-readable part
        val decoded = AetherisCore.decodeBech32(address)
        assertNotNull(decoded)
        assertEquals("aet", decoded!!.first)

        // Invalid checksum should fail decoding
        val malformedAddress = address.take(address.length - 2) + "xx"
        assertNull(AetherisCore.decodeBech32(malformedAddress))
    }

    @Test
    fun `test transaction signing and hashes`() {
        val mnemonic = AetherisCore.generateMnemonic(12)
        val txData = "sender:receiver:1000:10:0:1778716800:Memo".toByteArray()

        // Signing must generate a unique, non-empty hex signature string
        val signature = AetherisCore.signTx(mnemonic, txData)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
        assertEquals(64, signature.length) // HmacSha256 generates a 32-byte (64 hex characters) signature
    }

    @Test
    fun `test transaction consensus rules`() {
        val sender = "aet1v77yq8gf2tvdw0s3jn54khce6mua7lpsqqfk"
        val receiver = "aet1v77yq8gf2tvdw0s3jn54khce6mua7lpsqqab"

        // Build a transation
        val txId = AetherisProtocol.calculateTxHash(
            sender = sender,
            receiver = receiver,
            amount = 500000L,
            fee = 10L, // minimum matching fee
            nonce = 1L,
            timestamp = 1778716800L,
            memo = "Consensus rule test"
        )

        val tx = TransactionRecord(
            txId = txId,
            sender = sender,
            receiver = receiver,
            amountAetons = 500000L,
            feeAetons = 10L,
            nonce = 1L,
            timestamp = 1778716800L,
            signature = "sig_bytes_mock",
            memo = "Consensus rule test",
            height = -1L,
            status = "PENDING"
        )

        // Case 1: Valid transfer (sufficient balance)
        val validCheck = AetherisProtocol.validateTransaction(tx, 1_000_000L)
        assertTrue(validCheck.first)

        // Case 2: Insufficient balance
        val emptyCheck = AetherisProtocol.validateTransaction(tx, 100L)
        assertFalse(emptyCheck.first)
        assertTrue(emptyCheck.second.contains("Insufficient wallet funds"))

        // Case 3: Invalid recipient address checksum
        val invalidRecipientTx = tx.copy(receiver = "aet1invalidchecksum11111")
        val checksumCheck = AetherisProtocol.validateTransaction(invalidRecipientTx, 1_000_000L)
        assertFalse(checksumCheck.first)
        assertTrue(checksumCheck.second.contains("recipient address checksum"))

        // Case 4: Mismatched fee limit
        val lowFeeTx = tx.copy(feeAetons = 2L)
        val feeCheck = AetherisProtocol.validateTransaction(lowFeeTx, 1_000_000L)
        assertFalse(feeCheck.first)
        assertTrue(feeCheck.second.contains("Transaction fee must be at least"))
    }

    @Test
    fun `test symmetric secrets encryption`() {
        val plainText = "my super secure mnemonic secret words list mapping"
        val password = "cyber_security_auth"

        // Encrypt secret
        val encrypted = AetherisCore.encryptSecretData(plainText, password)
        assertNotNull(encrypted)
        assertTrue(encrypted.size > 12)

        // Decrypt secret
        val decrypted = AetherisCore.decryptSecretData(encrypted, password)
        assertEquals(plainText, decrypted)

        // Decrypt with bad credentials should fail
        val failedDecryption = AetherisCore.decryptSecretData(encrypted, "wrong_password_auth")
        assertTrue(failedDecryption.isEmpty())
    }
}
