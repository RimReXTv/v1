# Aetheris (AET) Wallet Security Audit & Threat Model

This document outlines the security architecture, Android secure storage integration, cryptographic inventory, and threat mitigations configured in Aetheris to prepare for third-party security review.

---

## 1. Threat Mitigation Matrix

Mitigations for common mobile cryptocurrency hazards are implemented directly in source code:

| Threat Endpoint | Target Vector | Code Mitigation |
| :--- | :--- | :--- |
| **Malware Clipboard Hijack** | Trojan intercepts clipboard addressing. | **Wipe Warning Display:** Shows a prominent alert instructing immediate clearing of clipboard data. Clipboard copying includes a secure banner advising manual cleanups. |
| **Encrypted Database Leak** | Physical device theft or raw SQLite copy. | **Encrypted Local Storage:** Wallet profiles, records, contacts, and transactions are stored using standard encrypted files / isolated databases. |
| **Keystore/PIN Sniffing** | Compromised operating system or memory dump. | **Android KeyStore Isolation:** Wallet seed phrases are encrypted using Android KeyStore master keys. Keys are backed by hardware-backed secure enclaves (StrongBox) where available, preventing keys from being extracted from memory. |
| **Phishing QR / Memo Spikes** | Malicious URIs or oversized payloads. | **Bech32 Decoding Assertion:** All QR inputs and paste fields pass strict Bech32-character validation and CRC checksum tests prior to signing, rejecting overflow strings. |
| **Malicious Block Injection** | Rogue P2P peer broadcasting fork claims. | **P2P Reputation Throttler:** Invalid blocks trigger immediate score penalization. Senders broadcasting mismatched hashes or unauthorized signatures are banned permanently. |
| **Double Spend Replays** | Replaying transactions across environments. | **Chain ID & Nonce Bindings:** Signatures bind the specific Chain ID (`aet-mainnet-1`, `aet-testnet-1`) and monotonic nonces, making cross-network replay impossible. |

---

## 2. Cryptographic Inventory & Operations

The codebase relies exclusively on audited, production-grade cryptographic implementations:

1. **Hash Algorithms (SHA-256 / SHA-512):** Used for transaction hazard binding, block sequence linking, and BIP-39 checksum hashing.
2. **Deterministic Signatures (ECDSA / SECP256K1):** Standard cryptosystem for blockchain signing.
3. **AES-256-GCM (No Padding):** Standard symmetric encryption protocol for securing private seeds at rest.
4. **Argon2 / SHA-256 Key Derivation:** Secures symmetric keys from user passwords.

---

## 3. Secure Memory Handling (Rust Core)

The Rust core (`/rust_core/src/lib.rs`) enforces clean memory bounds to avoid standard memory hazards:

- **Secrets Sanitization:** Uses memory zeroization (`Zeroize` trait) to clear sensitive key materials and raw converted byte arrays from heap memories immediately after signing completes.
- **FFI Panics Prevention:** Every FFI export handles standard Rust exceptions gracefully. Functions return safe error codes rather than propagating Rust panics across FFI boundaries into the Android Dalvik machine.
- **Strict Buffer Bounds:** JNI buffer reading copies data deterministically within precise size limitations, protecting against buffer overflows.

---

## 4. Security Audit Checklist

Developers can verify security integrity through local Robolectric and JVM unit tests:

- [x] Correct bip39 mnemonic generation & words count checks.
- [x] Bech32 HRP decoding & CRC checksum verify accuracy.
- [x] Android KeyStore keys isolation and mock recovery fallback.
- [x] Invariant supply guard (Total supply limit checks).
- [x] P2P malicious block rejecting and peer banning routines.
- [x] Watch-Only mode privilege restriction tests.
