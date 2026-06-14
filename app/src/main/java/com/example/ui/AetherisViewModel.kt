package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.AetherisCore
import com.example.data.*
import com.example.protocol.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

enum class AetherisScreen {
    SETUP,
    DASHBOARD,
    NODE,
    EXPLORER,
    CONTACTS,
    VAULT
}

class AetherisViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AetherisRepository(db)
    val nodeEngine = AetherisNodeEngine(application, repository)

    // --- Navigation & App Status ---
    private val _currentScreen = MutableStateFlow(AetherisScreen.SETUP)
    val currentScreen: StateFlow<AetherisScreen> = _currentScreen.asStateFlow()

    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // --- Active Wallet context ---
    private val _activeAccount = MutableStateFlow<WalletAccount?>(null)
    val activeAccount: StateFlow<WalletAccount?> = _activeAccount.asStateFlow()

    val allAccounts: StateFlow<List<WalletAccount>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI-triggered mnemonic storage for verification step during creation
    private val _pendingMnemonic = MutableStateFlow("")
    val pendingMnemonic: StateFlow<String> = _pendingMnemonic.asStateFlow()

    // --- Balances & History ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeBalance: StateFlow<Long> = _activeAccount
        .flatMapLatest { account ->
            if (account == null) flowOf(0L)
            else repository.observeBalanceForAddress(account.address)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeTransactions: StateFlow<List<TransactionRecord>> = _activeAccount
        .flatMapLatest { account ->
            if (account == null) flowOf(emptyList())
            else repository.getTransactionsForAddress(account.address)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Global Ledger Data ---
    val allBlocks: StateFlow<List<BlockRecord>> = repository.allBlocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val peers: StateFlow<List<PeerRecord>> = repository.activePeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Node Telemetries
    val isSyncing = nodeEngine.isSyncing
    val currentHeight = nodeEngine.currentHeight
    val activePeersCount = nodeEngine.activePeersCount
    val nodeLatencyMs = nodeEngine.nodeLatencyMs
    val networkHealth = nodeEngine.networkHealth
    val activeNetwork = nodeEngine.activeNetwork

    init {
        // Auto-select first account if exists
        viewModelScope.launch {
            allAccounts.collect { accounts ->
                if (accounts.isNotEmpty() && _activeAccount.value == null) {
                    _activeAccount.value = accounts.first { it.isFavorite } ?: accounts.first()
                    _currentScreen.value = AetherisScreen.DASHBOARD
                }
            }
        }
        // Start blockchain light node headers task sync
        nodeEngine.startSync()
    }

    fun navigateTo(screen: AetherisScreen) {
        _currentScreen.value = screen
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun selectAccount(account: WalletAccount) {
        _activeAccount.value = account
        navigateTo(AetherisScreen.DASHBOARD)
    }

    // --- Cryptographic Wallet Actions ---

    /**
     * Phase 1.2: Generate a fresh, cryptographically secure mnemonic phrase
     */
    fun draftMnemonic(wordCount: Int) {
        val phrase = AetherisCore.generateMnemonic(wordCount)
        _pendingMnemonic.value = phrase
    }

    /**
     * Clear the generated phrase cache
     */
    fun clearPendingMnemonic() {
        _pendingMnemonic.value = ""
    }

    /**
     * Confirm seed phrases backup matches and commit creation to encrypted database
     */
    fun commitGeneratedWallet(label: String) {
        val phrase = _pendingMnemonic.value
        if (phrase.isEmpty()) return
        
        viewModelScope.launch {
            try {
                val derivedAddress = AetherisCore.deriveAddress(phrase)
                // Phase 2: Encrypt mnemonic at rest using Android Keystore
                val encryptedEntropy = AetherisCore.encryptWithKeystore(phrase)
                
                val newAccount = WalletAccount(
                    label = label.ifBlank { "Aetheris Account" },
                    derivationPath = "m/44'/2026'/0'/0/0",
                    address = derivedAddress,
                    encryptedEntropyHex = encryptedEntropy,
                    isWatchOnly = false,
                    isFavorite = allAccounts.value.isEmpty()
                )

                repository.insertAccount(newAccount)
                _activeAccount.value = newAccount
                _pendingMnemonic.value = ""
                _isAppLocked.value = false
                navigateTo(AetherisScreen.DASHBOARD)
            } catch (e: Exception) {
                Log.e("AET_ViewModel", "Wallet creation failed: ${e.message}")
            }
        }
    }

    /**
     * Import an existing seed phrase
     */
    fun importWallet(label: String, mnemonic: String): Pair<Boolean, String> {
        val cleanPhrase = mnemonic.trim().lowercase().replace(",", " ")
        if (!AetherisCore.validateMnemonic(cleanPhrase)) {
            return Pair(false, "Mnemonic validation failed. Ensure wordlist words are correct.")
        }

        viewModelScope.launch {
            try {
                val derivedAddress = AetherisCore.deriveAddress(cleanPhrase)
                val encrypted = AetherisCore.encryptWithKeystore(cleanPhrase)

                val newAccount = WalletAccount(
                    label = label.ifBlank { "Imported Aetheris" },
                    derivationPath = "m/44'/2026'/0'/0/0",
                    address = derivedAddress,
                    encryptedEntropyHex = encrypted,
                    isWatchOnly = false,
                    isFavorite = allAccounts.value.isEmpty()
                )

                repository.insertAccount(newAccount)
                _activeAccount.value = newAccount
                _isAppLocked.value = false
                navigateTo(AetherisScreen.DASHBOARD)
            } catch (e: Exception) {
                Log.e("AET_ViewModel", "Wallet import failure: ${e.message}")
            }
        }
        return Pair(true, "Wallet successfully imported")
    }

    /**
     * Configure watch-only trackers with no private keys (Phase 11.7)
     */
    fun importWatchOnly(label: String, address: String): Pair<Boolean, String> {
        val decoded = AetherisCore.decodeBech32(address)
        if (decoded == null || decoded.first != AetherisProtocol.ADDR_PREFIX) {
            return Pair(false, "Invalid Bech32 checksum or invalid address format prefix.")
        }

        viewModelScope.launch {
            val newAccount = WalletAccount(
                label = label.ifBlank { "Watch Wallet" },
                derivationPath = "Watch Only",
                address = address.trim(),
                encryptedEntropyHex = "", // No private key or entropy material
                isWatchOnly = true,
                isFavorite = allAccounts.value.isEmpty()
            )
            repository.insertAccount(newAccount)
            _activeAccount.value = newAccount
            _isAppLocked.value = false
            navigateTo(AetherisScreen.DASHBOARD)
        }
        return Pair(true, "Watch-Only address imported successfully.")
    }

    /**
     * Decrypts the active account's mnemonic phrase using Android Keystore
     */
    fun revealActiveSeed(): String {
        val active = _activeAccount.value ?: return ""
        if (active.isWatchOnly) return "Watch Only (No seed phrase)"
        return AetherisCore.decryptWithKeystore(active.encryptedEntropyHex)
    }

    /**
     * Phase 0.52: Execute deterministic BIP-44 transaction signing & validation broadcast
     */
    fun executeSend(recipient: String, amountAetons: Long, memo: String): Pair<Boolean, String> {
        val active = _activeAccount.value ?: return Pair(false, "No active wallet selected.")
        
        if (active.isWatchOnly) {
            return Pair(false, "Signing failed: Privileged actions are disabled for Watch-Only nodes.")
        }

        val decodedRecipient = AetherisCore.decodeBech32(recipient)
        if (decodedRecipient == null || decodedRecipient.first != AetherisProtocol.ADDR_PREFIX) {
            return Pair(false, "Invalid recipient: Mismatched or corrupted Bech32 checksum address.")
        }

        val txFee = AetherisProtocol.MIN_FEE_AETONS
        val balance = activeBalance.value

        // Construct Transaction Block object
        val timestamp = System.currentTimeMillis() / 1000
        val tempTxId = UUID.randomUUID().toString()

        val seedPhrase = AetherisCore.decryptWithKeystore(active.encryptedEntropyHex)
        val txHash = AetherisProtocol.calculateTxHash(
            sender = active.address,
            receiver = recipient.trim(),
            amount = amountAetons,
            fee = txFee,
            nonce = timestamp,
            timestamp = timestamp,
            memo = memo
        )

        // Generate actual cryptographic signature over tx hash
        val signatureBytes = AetherisCore.signTx(seedPhrase, txHash.toByteArray())

        val txRecord = TransactionRecord(
            txId = txHash,
            sender = active.address,
            receiver = recipient.trim(),
            amountAetons = amountAetons,
            feeAetons = txFee,
            nonce = timestamp,
            timestamp = timestamp,
            signature = signatureBytes,
            memo = memo,
            height = -1L, // Mark as MEMPOOL PENDING
            status = "PENDING"
        )

        // Consensus verification checks
        val validation = AetherisProtocol.validateTransaction(txRecord, balance)
        if (!validation.first) {
            return Pair(false, "Validation Error: ${validation.second}")
        }

        // Add to Database (Pending State)
        viewModelScope.launch {
            repository.insertTransaction(txRecord)
            Log.d("AET_Network", "Broadcasted transaction to local Mempool: ${txRecord.txId}")
        }

        return Pair(true, "Transaction successfully validated, signed, and broadcasted to peer network.")
    }

    // --- Contacts Directory ---

    fun addContact(name: String, address: String, notes: String): Pair<Boolean, String> {
        val decoded = AetherisCore.decodeBech32(address)
        if (decoded == null || decoded.first != AetherisProtocol.ADDR_PREFIX) {
            return Pair(false, "Validation Failure: Invalid Bech32 routing address.")
        }
        viewModelScope.launch {
            repository.insertContact(Contact(label = name, address = address.trim(), notes = notes))
        }
        return Pair(true, "Saved.")
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    // --- Admin Commands ---

    fun switchActiveNetwork(network: AetherisNetwork) {
        nodeEngine.setNetwork(network)
    }

    fun wipeDatabaseAndReset() {
        viewModelScope.launch {
            nodeEngine.stopSync()
            repository.clearBlockchain()
            repository.clearTransactions()
            repository.clearPeers()
            _activeAccount.value = null
            _currentScreen.value = AetherisScreen.SETUP
            nodeEngine.initializeGenesisIfEmpty()
            nodeEngine.startSync()
        }
    }
}
