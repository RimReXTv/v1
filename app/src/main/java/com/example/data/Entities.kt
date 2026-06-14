package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_accounts")
data class WalletAccount(
    @PrimaryKey(autoGenerate = true) val accountId: Int = 0,
    val label: String,
    val derivationPath: String,
    val address: String,
    val encryptedEntropyHex: String,
    val isWatchOnly: Boolean = false,
    val isFavorite: Boolean = false
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val contactId: Int = 0,
    val address: String,
    val label: String,
    val notes: String = "",
    val isFavorite: Boolean = false
)

@Entity(tableName = "block_records")
data class BlockRecord(
    @PrimaryKey val height: Long,
    val hash: String,
    val parentHash: String,
    val timestamp: Long,
    val merkleRoot: String,
    val validatorPublicKey: String,
    val blockSignature: String,
    val transactionCount: Int
)

@Entity(tableName = "transaction_records")
data class TransactionRecord(
    @PrimaryKey val txId: String,
    val sender: String,
    val receiver: String,
    val amountAetons: Long, // 1 AET = 1,000,000 aetons
    val feeAetons: Long,
    val nonce: Long,
    val timestamp: Long,
    val signature: String,
    val memo: String = "",
    val height: Long, // -1 if pending in Mempool
    val status: String // "PENDING", "INCLUDED", "SAFE", "FINALIZED", "ORPHANED"
)

@Entity(tableName = "peer_records")
data class PeerRecord(
    @PrimaryKey val ipAddress: String,
    val score: Int = 100,
    val latencyMs: Int = 100,
    val isBanned: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)
