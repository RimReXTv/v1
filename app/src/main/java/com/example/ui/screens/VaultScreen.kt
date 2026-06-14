package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.protocol.AetherisNetwork
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*

@Composable
fun VaultScreen(viewModel: AetherisViewModel) {
    val scrollState = rememberScrollState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val activeNet by viewModel.activeNetwork.collectAsState()

    var showMnemonicText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text("Vault Security Controls", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
        Text("Manage private key parameters and environment parameters.", style = MaterialTheme.typography.bodyLarge, color = SoftMutedGray)

        Spacer(modifier = Modifier.height(20.dp))

        if (activeAccount == null) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No active wallet. Access restricted.", color = SoftMutedGray)
            }
            return
        }

        val acc = activeAccount!!

        // Section 1: Active Wallet metadata
        Text("Active Wallet Profile", style = MaterialTheme.typography.titleLarge, color = NeonTeal)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsRow("Derivation Path", acc.derivationPath)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow("Watch-Only Mode", if (acc.isWatchOnly) "Enabled (No private keys)" else "Disabled (Full Signing)")
                Spacer(modifier = Modifier.height(8.dp))
                SettingsRow("Address Checksum", "Bech32 Verified")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Environment settings
        Text("Select Active Blockchain Network", style = MaterialTheme.typography.titleLarge, color = NeonTeal)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Switching networks clears the ledger history local tables and downloads isolated, environment-specific transaction files.",
                    fontSize = 12.sp,
                    color = SoftMutedGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                AetherisNetwork.values().forEach { net ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.switchActiveNetwork(net) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(net.name, fontWeight = FontWeight.Bold, color = if (activeNet == net) NeonTeal else WarmWhite)
                            Text("Chain ID: ${net.chainId}", fontSize = 11.sp, color = SoftMutedGray)
                        }
                        RadioButton(
                            selected = activeNet == net,
                            onClick = { viewModel.switchActiveNetwork(net) },
                            colors = RadioButtonDefaults.colors(selectedColor = NeonTeal, unselectedColor = BorderDark)
                        )
                    }
                    Divider(color = BorderDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Seed Reveal (Biometric authentication gate simulated inline via direct security tap)
        Text("Physical Seed Phrase reveal", style = MaterialTheme.typography.titleLarge, color = CyberAmber)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, PhantomRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Exporting secrets poses severe security hazards. Uncover seed characters only when in a private space.",
                    fontSize = 12.sp,
                    color = SoftMutedGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (showMnemonicText.isEmpty()) {
                    Button(
                        onClick = {
                            // Biometric gate simulated securely
                            showMnemonicText = viewModel.revealActiveSeed()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                        modifier = Modifier.fillMaxWidth().testTag("reveal_seed_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF001D36))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Biometrics & Reveal Seed", color = Color(0xFF001D36), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RichOnyx, RoundedCornerShape(8.dp))
                            .border(1.dp, NeonTeal, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = showMnemonicText,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = NeonTeal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showMnemonicText = "" }) {
                        Text("Hide secret letters", color = CyberAmber)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section 4: Emergency hard wipes (AET reset locks)
        Text("Node Emergency Management", style = MaterialTheme.typography.titleLarge, color = PhantomRed)
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, PhantomRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Completely wipes local databases, purging stored contacts, validator latency records, block archives, and private keys. All parameters will revert to onboarding setup state.",
                    fontSize = 12.sp,
                    color = SoftMutedGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PhantomRed),
                    modifier = Modifier.fillMaxWidth().testTag("emergency_wipe_btn")
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purge Local Databases State", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = RichOnyx,
            title = { Text("Are you absolutely sure?", color = PhantomRed, style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "This action is irreversible. If your physical seed phrase has not been written down on physical media, you will permanently lose access to all Aetheris funds.",
                    color = WarmWhite
                )
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Abort", color = SoftMutedGray)
                    }

                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.wipeDatabaseAndReset()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PhantomRed),
                        modifier = Modifier.testTag("wipe_confirm_btn")
                    ) {
                        Text("Confirm Ledger Wipe", color = Color.White)
                    }
                }
            }
        )
    }
}

@Composable
fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = SoftMutedGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = WarmWhite, fontWeight = FontWeight.Bold)
    }
}
