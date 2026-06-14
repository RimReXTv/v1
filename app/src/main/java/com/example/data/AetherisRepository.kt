package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AetherisRepository(private val db: AppDatabase) {

    // --- Wallet Accounts ---
    val allAccounts: Flow<List<WalletAccount>> = db.walletAccountDao().getAllAccounts()

    suspend fun insertAccount(account: WalletAccount) {
        db.walletAccountDao().insertAccount(account)
    }

    suspend fun updateAccount(account: WalletAccount) {
        db.walletAccountDao().updateAccount(account)
    }

    suspend fun deleteAccount(account: WalletAccount) {
        db.walletAccountDao().deleteAccount(account)
    }

    suspend fun getAccountByAddress(address: String): WalletAccount? {
        return db.walletAccountDao().getAccountByAddress(address)
    }

    // --- Address Book Contacts ---
    val allContacts: Flow<List<Contact>> = db.contactDao().getAllContacts()

    suspend fun insertContact(contact: Contact) {
        db.contactDao().insertContact(contact)
    }

    suspend fun deleteContact(contact: Contact) {
        db.contactDao().deleteContact(contact)
    }

    // --- Local Blockchain Ledger State ---
    val allBlocks: Flow<List<BlockRecord>> = db.blockRecordDao().getAllBlocks()

    suspend fun getBlockByHeight(height: Long): BlockRecord? {
        return db.blockRecordDao().getBlockByHeight(height)
    }

    suspend fun getLatestBlock(): BlockRecord? {
        return db.blockRecordDao().getLatestBlock()
    }

    suspend fun insertBlock(block: BlockRecord) {
        db.blockRecordDao().insertBlock(block)
    }

    suspend fun clearBlockchain() {
        db.blockRecordDao().clearBlockchain()
    }

    // --- Local Transaction History & Mempool ---
    val allTransactions: Flow<List<TransactionRecord>> = db.transactionRecordDao().getAllTransactions()

    fun getTransactionsForAddress(address: String): Flow<List<TransactionRecord>> {
        return db.transactionRecordDao().getTransactionsForAddress(address)
    }

    val mempoolTransactions: Flow<List<TransactionRecord>> = db.transactionRecordDao().getMempoolTransactions()

    suspend fun getTxById(txId: String): TransactionRecord? {
        return db.transactionRecordDao().getTxById(txId)
    }

    suspend fun insertTransaction(tx: TransactionRecord) {
        db.transactionRecordDao().insertTransaction(tx)
    }

    suspend fun clearTransactions() {
        db.transactionRecordDao().clearTransactions()
    }

    /**
     * Reactively computes the balance (in aetons) for an address
     * by scanning confirming/validated ledger transactions.
     * Balance = Sum(Inbound) - Sum(Outbound) - Sum(Outbound Fees)
     */
    fun observeBalanceForAddress(address: String): Flow<Long> {
        return getTransactionsForAddress(address).map { transactions ->
            var inbound = 0L
            var outbound = 0L
            for (tx in transactions) {
                // Ignore orphaned or completely invalid transactions
                if (tx.status == "ORPHANED") continue

                if (tx.receiver.trim().lowercase() == address.trim().lowercase()) {
                    inbound += tx.amountAetons
                }
                if (tx.sender.trim().lowercase() == address.trim().lowercase()) {
                    outbound += tx.amountAetons + tx.feeAetons
                }
            }
            inbound - outbound
        }
    }

    // --- Peer Sync Nodes ---
    val activePeers: Flow<List<PeerRecord>> = db.peerRecordDao().getActivePeers()

    suspend fun insertPeer(peer: PeerRecord) {
        db.peerRecordDao().insertPeer(peer)
    }

    suspend fun updatePeer(peer: PeerRecord) {
        db.peerRecordDao().updatePeer(peer)
    }

    suspend fun clearPeers() {
        db.peerRecordDao().clearPeers()
    }
}
