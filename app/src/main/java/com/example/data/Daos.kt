package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletAccountDao {
    @Query("SELECT * FROM wallet_accounts ORDER BY accountId ASC")
    fun getAllAccounts(): Flow<List<WalletAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: WalletAccount)

    @Update
    suspend fun updateAccount(account: WalletAccount)

    @Delete
    suspend fun deleteAccount(account: WalletAccount)

    @Query("SELECT * FROM wallet_accounts WHERE address = :address LIMIT 1")
    suspend fun getAccountByAddress(address: String): WalletAccount?
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY label ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)
}

@Dao
interface BlockRecordDao {
    @Query("SELECT * FROM block_records ORDER BY height DESC")
    fun getAllBlocks(): Flow<List<BlockRecord>>

    @Query("SELECT * FROM block_records WHERE height = :height LIMIT 1")
    suspend fun getBlockByHeight(height: Long): BlockRecord?

    @Query("SELECT * FROM block_records ORDER BY height DESC LIMIT 1")
    suspend fun getLatestBlock(): BlockRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockRecord)

    @Query("DELETE FROM block_records")
    suspend fun clearBlockchain()
}

@Dao
interface TransactionRecordDao {
    @Query("SELECT * FROM transaction_records ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE sender = :address OR receiver = :address ORDER BY timestamp DESC")
    fun getTransactionsForAddress(address: String): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE status = 'PENDING' ORDER BY feeAetons DESC")
    fun getMempoolTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transaction_records WHERE txId = :txId LIMIT 1")
    suspend fun getTxById(txId: String): TransactionRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: TransactionRecord)

    @Query("DELETE FROM transaction_records")
    suspend fun clearTransactions()
}

@Dao
interface PeerRecordDao {
    @Query("SELECT * FROM peer_records ORDER BY score DESC, latencyMs ASC")
    fun getActivePeers(): Flow<List<PeerRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerRecord)

    @Update
    suspend fun updatePeer(peer: PeerRecord)

    @Query("DELETE FROM peer_records")
    suspend fun clearPeers()
}
