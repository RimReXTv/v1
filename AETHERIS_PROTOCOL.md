# Aetheris (AET) Ledger Protocol Specification & Whitepaper

This document defines the formal protocol specifications, cryptographic foundations, consensus math, fee parameters, and P2P synchronization metrics of the Aetheris (AET) network.

---

## 1. Cryptographic Identity & Addresses

Aetheris utilizes a secure non-custodial single-coin address architecture to avoid double-spend states and ensure offline validation.

### 1.1 Mnemonics & Key Derivation
- **Mnemonic Standard:** BIP-39 (with English-only wordlist supported in v1).
- **HD Wallet Path:** BIP-44 compliant derivation path for the primary account index:
  $$\text{Derivation Path: } m/44'/2026'/0'/0/0$$
- **Seed Cryptography:** Secure randomness maps 128, 192, or 256 bits of entropy to 12, 18, or 24 mnemonic recovery words. Master seed expansion is calculated via 2048 iterations of PBKDF2/HMAC-SHA512.

### 1.2 Address Format (Bech32)
Aetheris address representations are human-readable Bech32-encoded vectors of the RIPEMD160 hash of the derived SECP256K1 public key.
- **Prefix (Human-Readable Part):** `aet`
- **Separator:** `1`
- **Output Alphabet:** `qpzry9x8gf2tvdw0s3jn54khce6mua7l`
- **Example Valid Address:** `aet1v77yq8gf2tvdw0s3jn54khce6mua7lpsqqfk`

---

## 2. Ledger Transaction Schema

Every ledger state transition is represented by a deterministic Transaction format, serialized deterministically prior to signing.

$$\text{Transaction Body: } \{ \text{Sender}, \text{Receiver}, \text{Amount}, \text{Fee}, \text{Nonce}, \text{Timestamp}, \text{Memo} \}$$

$$\text{Sign Hash: } H = \text{SHA256}(\text{Sender} \mathbin{\Vert} \text{Receiver} \mathbin{\Vert} \text{Amount} \mathbin{\Vert} \text{Fee} \mathbin{\Vert} \text{Nonce} \mathbin{\Vert} \text{Timestamp} \mathbin{\Vert} \text{Memo})$$

- **Sender Address:** Bech32 address.
- **Recipient Address:** Bech32 address.
- **Amount:** Denominated in **aetons** ($1 \text{ AET} = 1,000,000 \text{ aetons}$).
- **Fee:** Minimum validation fee of $10 \text{ aetons}$ ($0.000010 \text{ AET}$). No zero-fee transaction bypasses consensus.
- **Nonce:** Strictly monotonic Unix millisecond timestamp or count per addressing index preventing duplicate replay attacks.
- **Signature:** Deterministic compact DER-encoded SECP256K1 signature matching derived public credentials.

---

## 3. Block Structure

Aetheris groups transactions into linked blocks.

### 3.1 Block Header
```rust
struct BlockHeader {
    version: u32,                  // Protocol version (1)
    chain_id: String,              // Environment designation
    height: u64,                   // Sequential block height indexes
    timestamp: u64,                // Unix timestamp record
    previous_hash: String,         // SHA-256 parent hash block connection
    merkle_root: String,           // Deterministic Merkle Root hash of inside transactions
    validator_public_key: String,  // BIP-173 address of consensus block proposer
    block_signature: String,       // Proposer signature verifying block validity
}
```

### 3.2 Block Body
Contains a vector of serialized, signed `TransactionRecord` logs. Each transaction is validated, and matching Mempool entries are evicted on finalized state commit.

---

## 4. Consensus Mechanism: Proof of Availability & Validation (PoAV)

Aetheris implements **Proof of Availability + Validation (PoAV)** to allow mobile light-clients to actively secure the ledger without energy-heavy hash mining.

1. **Block Proposal:** A set of static booster validators (`AetherisProtocol.GENESIS_VALIDATORS`) propose blocks round-robin.
2. **Availability Proof:** Active mobile light nodes download block headers, reconstruct regional Merkle paths, and compare calculated Merkle roots against headers.
3. **Validation Witnessing:** Light-client nodes execute state transitions, validating that sender balances $\ge \text{amount} + \text{fee}$ and that nonces are sequential.
4. **Finality Score:** Blocks receiving signature approval from $\ge 2/3$ validator quorums achieve deterministic finality status (`FINALIZED`), protecting against forks.

---

## 5. Tokenomics, Emissions, & Fee Burn

- **Maximum Cap:** $\mathbf{100,000,000 \text{ AET}}$ (100 Billion aetons). Supply overflow is prevented at runtime by invariant assertion gates.
- **Genesis Distribution:** 50% ($50,000,000 \text{ AET}$) in genesis block allocation `genesis_allocation_tx`.
- **Validation Block Rewards:** $5 \text{ AET}$ ($5,000,000 \text{ aetons}$) emitted per valid block block validation, distributed to consensus block proposers.
- **Fee burning:**
  - **90%** of all transaction fees are eternally **burned** (deleted from supply) to counter-act inflation.
  - **10%** of all transaction fees are transferred as a validation incentive to validator nodes.

---

## 6. P2P Sync & Reputation Metrics

Nodes communicate with seed peers. If a seed peer provides fraudulent block records or failed parent hash bindings, the local node down-scores their reputation:

$$\text{Reputation Metric: } R_{\text{new}} = \max(0, R_{\text{old}} - \Delta_{\text{penalty}})$$

If $R \le 40$, the peer's socket is permanently **blacklisted / banned** from local synchronization routines.
