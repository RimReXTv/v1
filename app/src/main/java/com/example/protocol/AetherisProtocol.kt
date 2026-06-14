package com.example.protocol

import com.example.crypto.AetherisCore
import com.example.data.BlockRecord
import com.example.data.TransactionRecord
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Enumeration of Aetheris Network environments.
 */
enum class AetherisNetwork(val chainId: String, val genesisHash: String) {
    MAINNET("aet-mainnet-1", "00000000aet_genesis_mainnet_hash_971e42b"),
    TESTNET("aet-testnet-1", "00000000aet_genesis_testnet_hash_617c59a"),
    DEVNET("aet-devnet-1", "00000000aet_genesis_devnet_hash_215f93b")
}

/**
 * Aetheris Immutable Protocol Specifications and Rules.
 */
object AetherisProtocol {
    const val PROTOCOL_VERSION = 1
    const val ADDR_PREFIX = "aet"
    
    // Limits
    const val MAX_BLOCK_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB
    const val MAX_TX_SIZE_BYTES = 100 * 1024 // 100 KB
    const val MIN_FEE_AETONS = 10L // Minimum spam fee limit
    const val MAX_SUPPLY_AET = 100_000_000L
    const val AETONS_PER_AET = 1_000_000L // 1 AET = 10^6 Aetons list
    const val BLOCK_REWARD_AETONS = 5 * AETONS_PER_AET // 5 AET per snapshot validation

    // Immutable Seed Bootstrap peers
    val SEED_PEERS = listOf(
        "157.90.122.10:2026",
        "95.217.44.201:2026",
        "167.235.15.42:2026",
        "194.5.178.69:2026"
    )

    // Genesis Validators
    val GENESIS_VALIDATORS = listOf(
        "aet1validator1x8gf2tvdw0s3jn54khce6mua7lq59a7",
        "aet1validator2qpzry9x8gf2tvdw0s3jn54khce6mu98e",
        "aet1validator3mvx9gf2tvdw0s3jn54khce6mua3x21a"
    )

    /**
     * Obtains the hard-coded Genesis Block Record for a chosen active network.
     */
    fun getGenesisBlock(network: AetherisNetwork): BlockRecord {
        return BlockRecord(
            height = 0L,
            hash = network.genesisHash,
            parentHash = "0000000000000000000000000000000000000000000000000000000000000000",
            timestamp = 1778716800L, // 2026-06-14T00:00:00Z
            merkleRoot = "genesis_merkle_root_9918abf8cf202aeb7d002a2fc849ca0e",
            validatorPublicKey = GENESIS_VALIDATORS[0],
            blockSignature = "genesis_sig_75e2faff8b21",
            transactionCount = 1
        )
    }

    /**
     * Determines the initial genesis transaction giving distribution of initial supply.
     */
    fun getGenesisTransaction(network: AetherisNetwork): TransactionRecord {
        return TransactionRecord(
            txId = "genesis_allocation_tx_" + network.chainId,
            sender = "aet1systemgenesis0000000000000",
            receiver = "aet1v77yq8gf2tvdw0s3jn54khce6mua7lpsqqfk", // Initial Foundation Recovery Vault
            amountAetons = 50_000_000L * AETONS_PER_AET, // 50% distributed initial allocation
            feeAetons = 0L,
            nonce = 0L,
            timestamp = 1778716800L,
            signature = "system_genesis_signature",
            memo = "Aetheris (AET) Genesis Distribution Package",
            height = 0L,
            status = "FINALIZED"
        )
    }

    /**
     * Calculates the deterministic hash of a transaction to guard against tampering.
     */
    fun calculateTxHash(sender: String, receiver: String, amount: Long, fee: Long, nonce: Long, timestamp: Long, memo: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val raw = "$sender:$receiver:$amount:$fee:$nonce:$timestamp:$memo"
            val hashBytes = digest.digest(raw.toByteArray(StandardCharsets.UTF_8))
            return bytesToHex(hashBytes)
        } catch (e: Exception) {
            return "tx_hash_error_" + System.currentTimeMillis()
        }
    }

    /**
     * Verifies that the transaction matches protocol transition rules (non-negative, fee check, address match).
     */
    fun validateTransaction(tx: TransactionRecord, senderBalance: Long): Pair<Boolean, String> {
        if (tx.amountAetons <= 0) return Pair(false, "Inbound transfer amount must be greater than zero.")
        if (tx.feeAetons < MIN_FEE_AETONS) return Pair(false, "Transaction fee must be at least $MIN_FEE_AETONS aetons.")
        if (tx.sender == tx.receiver) return Pair(false, "Self-transfer transactions are prohibited.")
        if (tx.sender != "aet1systemgenesis0000000000000" && senderBalance < tx.amountAetons + tx.feeAetons) {
            return Pair(false, "Insufficient wallet funds. Balance: ${senderBalance / AETONS_PER_AET.toDouble()} AET, Required: ${(tx.amountAetons + tx.feeAetons) / AETONS_PER_AET.toDouble()} AET.")
        }
        
        // Address check verify
        if (AetherisCore.decodeBech32(tx.sender) == null && tx.sender != "aet1systemgenesis0000000000000") {
            return Pair(false, "Invalid sender address checksum.")
        }
        if (AetherisCore.decodeBech32(tx.receiver) == null) {
            return Pair(false, "Invalid recipient address checksum.")
        }

        return Pair(true, "Validated")
    }

    /**
     * Hex bytes converter
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        val lookup = "0123456789abcdef"
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = lookup[v ushr 4]
            hexChars[i * 2 + 1] = lookup[v and 0x0F]
        }
        return String(hexChars)
    }
}
