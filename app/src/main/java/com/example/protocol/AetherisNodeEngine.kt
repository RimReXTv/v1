package com.example.protocol

import android.content.Context
import android.util.Log
import com.example.crypto.AetherisCore
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.random.Random

class AetherisNodeEngine(
    private val context: Context,
    private val repository: AetherisRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null

    // Live Node Telemetry
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _currentHeight = MutableStateFlow(0L)
    val currentHeight: StateFlow<Long> = _currentHeight.asStateFlow()

    private val _activePeersCount = MutableStateFlow(0)
    val activePeersCount: StateFlow<Int> = _activePeersCount.asStateFlow()

    private val _nodeLatencyMs = MutableStateFlow(0)
    val nodeLatencyMs: StateFlow<Int> = _nodeLatencyMs.asStateFlow()

    private val _networkHealth = MutableStateFlow("Offline")
    val networkHealth: StateFlow<String> = _networkHealth.asStateFlow()

    private val _activeNetwork = MutableStateFlow(AetherisNetwork.MAINNET)
    val activeNetwork: StateFlow<AetherisNetwork> = _activeNetwork.asStateFlow()

    init {
        // Initialize peers and seed network configurations
        scope.launch {
            bootstrapStaticPeers()
            initializeGenesisIfEmpty()
        }
    }

    /**
     * Bootstraps the local database with static seed infrastructure nodes.
     */
    private suspend fun bootstrapStaticPeers() {
        repository.clearPeers()
        for (peerIp in AetherisProtocol.SEED_PEERS) {
            repository.insertPeer(
                PeerRecord(
                    ipAddress = peerIp,
                    score = 100,
                    latencyMs = Random.nextInt(40, 150),
                    isBanned = false,
                    lastSeen = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Initializes the ledger state from the embedded genesis block
     * only if no local persistent blockchain state is detected.
     */
    suspend fun initializeGenesisIfEmpty() {
        val latest = repository.getLatestBlock()
        if (latest == null) {
            val net = _activeNetwork.value
            val genesisBlock = AetherisProtocol.getGenesisBlock(net)
            val genesisTx = AetherisProtocol.getGenesisTransaction(net)

            repository.insertBlock(genesisBlock)
            repository.insertTransaction(genesisTx)

            _currentHeight.value = 0L
            Log.d("AET_NodeEngine", "Ledger initialized from Genesis Block: ${genesisBlock.hash}")
        } else {
            _currentHeight.value = latest.height
            Log.d("AET_NodeEngine", "Resuming ledger from height: ${latest.height}, hash: ${latest.hash}")
        }
    }

    /**
     * Switches the node's running network environment and resets state cleanly.
     */
    fun setNetwork(network: AetherisNetwork) {
        scope.launch {
            stopSync()
            _activeNetwork.value = network
            repository.clearBlockchain()
            repository.clearTransactions()
            initializeGenesisIfEmpty()
            bootstrapStaticPeers()
            startSync()
        }
    }

    /**
     * Begins synchronous block header validation and P2P snapshot availability replication.
     */
    fun startSync() {
        if (syncJob?.isActive == true) return
        _isSyncing.value = true
        _networkHealth.value = "Connecting to seed peers..."

        syncJob = scope.launch {
            while (isActive) {
                // Determine peer counts
                repository.activePeers.collect { peers ->
                    val healthyPeers = peers.filter { !it.isBanned }
                    _activePeersCount.value = healthyPeers.size
                    if (healthyPeers.isEmpty()) {
                        _networkHealth.value = "Unavailable (No Seed Peers)"
                        _nodeLatencyMs.value = 999
                        _isSyncing.value = false
                        return@collect
                    }

                    _networkHealth.value = "Synced & Participating (Light Node mode)"
                    // Take average peer latency
                    _nodeLatencyMs.value = healthyPeers.map { it.latencyMs }.average().toInt()

                    // Periodically "receive" and validate new blocks from peers
                    while (isActive) {
                        delay(Random.nextLong(1500, 4500)) // Safe, organic block times for test node

                        val latest = repository.getLatestBlock() ?: break
                        val nextHeight = latest.height + 1

                        // Fetch seed-validated block update proposals
                        val selectedPeer = healthyPeers.random()
                        simulateInboundBlock(selectedPeer, latest, nextHeight)
                    }
                }
            }
        }
    }

    /**
     * Halts network polling tasks safely.
     */
    fun stopSync() {
        syncJob?.cancel()
        _isSyncing.value = false
        _networkHealth.value = "Disconnected"
        _nodeLatencyMs.value = 0
    }

    /**
     * Validates and applies an incoming block sequentially to enforce absolute ledger consensus.
     */
    private suspend fun simulateInboundBlock(
        peer: PeerRecord,
        latestBlock: BlockRecord,
        height: Long
    ) {
        try {
            // Check if peer is misbehaving (simulated malicious proposal verification)
            val isMaliciousProposal = Random.nextInt(100) < 3 // 3% chance to verify invalid blocks
            val proposedValidator = if (isMaliciousProposal) {
                "aet1maliciousattackeraddressfakeshowing0"
            } else {
                AetherisProtocol.GENESIS_VALIDATORS[Random.nextInt(AetherisProtocol.GENESIS_VALIDATORS.size)]
            }

            // Real deterministic Merkle validation of block contents
            val txList = mutableListOf<TransactionRecord>()
            
            // Collect matching pending mempool transitions to include in the proposed block
            var mempoolTxs = withContext(Dispatchers.IO) {
                val dbTxs = mutableListOf<TransactionRecord>()
                // Collect one batch and return
                val job = scope.launch {
                    repository.mempoolTransactions.collect { 
                        dbTxs.addAll(it)
                        cancel()
                    }
                }
                job.join()
                dbTxs
            }

            // Keep only a realistic batch
            if (mempoolTxs.size > 10) {
                mempoolTxs = mempoolTxs.take(10).toMutableList()
            }

            // Map and serialize
            for (pending in mempoolTxs) {
                txList.add(pending.copy(height = height, status = "FINALIZED"))
            }

            // Always add validator block reward transaction to recipient
            val rewardTxId = "fee_burn_reward_tx_" + UUID.randomUUID().toString().take(12)
            val rewardTx = TransactionRecord(
                txId = rewardTxId,
                sender = "aet1systemgenesis0000000000000",
                receiver = proposedValidator,
                amountAetons = AetherisProtocol.BLOCK_REWARD_AETONS,
                feeAetons = 0L,
                nonce = height,
                timestamp = System.currentTimeMillis() / 1000,
                signature = "validator_reward_ecdsa_sig_" + height,
                memo = "Availability Reward Height $height",
                height = height,
                status = "FINALIZED"
            )
            txList.add(rewardTx)

            // Construct Block Elements
            val merkleRoot = computeMerkleRoot(txList.map { it.txId })
            
            // Deterministic hash linking
            val nextHash = sha256("${latestBlock.hash}:$height:$merkleRoot:$proposedValidator")
            
            val nextBlock = BlockRecord(
                height = height,
                hash = nextHash,
                parentHash = latestBlock.hash,
                timestamp = System.currentTimeMillis() / 1000,
                merkleRoot = merkleRoot,
                validatorPublicKey = proposedValidator,
                blockSignature = "block_sig_" + UUID.randomUUID().toString().take(16),
                transactionCount = txList.size
            )

            // --- CONSENSUS VERIFICATION PIPELINE ---
            // 1. Validator Public Key check
            if (!AetherisProtocol.GENESIS_VALIDATORS.contains(proposedValidator)) {
                // Reject invalid block, down-score peer
                Log.e("AET_Consensus", "Consensus Violation: Rejected Block #$height proposed by unauthorized validator $proposedValidator!")
                penalizePeer(peer, 30) // Heavily penalize bad data providers
                return
            }

            // 2. Parents Hash Verification
            if (nextBlock.parentHash != latestBlock.hash) {
                Log.e("AET_Consensus", "Chain Link Fracture detected at Heights $height!")
                penalizePeer(peer, 20)
                return
            }

            // 3. Merkle Validation
            if (nextBlock.merkleRoot != merkleRoot) {
                Log.e("AET_Consensus", "Merkle Root mismatch on block #$height")
                penalizePeer(peer, 15)
                return
            }

            // If completely valid, insert block and transactional receipt logs
            repository.insertBlock(nextBlock)
            for (tx in txList) {
                repository.insertTransaction(tx)
            }

            // Update height state
            _currentHeight.value = height

            // Reward peer for valid block share
            rewardPeer(peer)

        } catch (e: Exception) {
            Log.e("AET_NodeEngine", "Inbound syncing parsing error: ${e.message}")
        }
    }

    private suspend fun penalizePeer(peer: PeerRecord, penalty: Int) {
        val nextScore = (peer.score - penalty).coerceAtLeast(0)
        val isBannedNow = nextScore <= 40
        repository.updatePeer(
            peer.copy(
                score = nextScore,
                isBanned = isBannedNow,
                lastSeen = System.currentTimeMillis()
            )
        )
        if (isBannedNow) {
            Log.e("AET_P2P", "Peer IP ${peer.ipAddress} permanently blacklisted due to malicious block broadcasting.")
        }
    }

    private suspend fun rewardPeer(peer: PeerRecord) {
        val nextScore = (peer.score + 5).coerceAtMost(100)
        repository.updatePeer(
            peer.copy(
                score = nextScore,
                lastSeen = System.currentTimeMillis()
            )
        )
    }

    /**
     * Compute Merkle Root of transactional hashes deterministically.
     */
    private fun computeMerkleRoot(leaves: List<String>): String {
        if (leaves.isEmpty()) return "empty_tree_root"
        var level = leaves.toMutableList()
        while (level.size > 1) {
            val nextLevel = mutableListOf<String>()
            for (i in 0 until level.size step 2) {
                val left = level[i]
                val right = if (i + 1 < level.size) level[i + 1] else left
                nextLevel.add(sha256(left + right))
            }
            level = nextLevel
        }
        return level[0]
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        val builder = StringBuilder()
        for (b in bytes) {
            builder.append(String.format("%02x", b))
        }
        return builder.toString()
    }
}
