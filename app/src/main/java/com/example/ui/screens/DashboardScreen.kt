package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(viewModel: AetherisViewModel) {
    val activeAccount by viewModel.activeAccount.collectAsState()
    val balance by viewModel.activeBalance.collectAsState()
    val transactions by viewModel.activeTransactions.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showReceiveDialog by remember { mutableStateOf(false) }
    var showSendDialog by remember { mutableStateOf(false) }
    var clipboardWipeWarningVisible by remember { mutableStateOf(false) }

    if (activeAccount == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active wallet. Set up or import a seed.", color = SoftMutedGray)
        }
        return
    }

    val acc = activeAccount!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .padding(16.dp)
    ) {
        // Upper Card: High Density Balance Panel
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberAmber),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (acc.isWatchOnly) Color(0xFFC77C00) else Color(0xFF146C2E))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = acc.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF001D36),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (acc.isWatchOnly) {
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF001E2F).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("WATCH Only", color = Color(0xFF001D36), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Real Balance Computed from validated Transactions (AET rules)
                val aetCoins = balance / 1_000_000.0
                Text(
                    text = String.format("%.6f", aetCoins),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF001D36),
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )

                Text(
                    text = "AETERIS (AET) COINS BALANCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF001D36).copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Shortened address Bech32 with click-to-copy (Phase 0.28 address display)
                val displayAddr = if (acc.address.length > 20) {
                    "${acc.address.take(10)}...${acc.address.takeLast(10)}"
                } else acc.address

                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Aetheris Address", acc.address)
                            clipboard.setPrimaryClip(clip)
                            
                            clipboardWipeWarningVisible = true
                            scope.launch {
                                delay(4000)
                                clipboardWipeWarningVisible = false
                            }
                            Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = NeonTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayAddr,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = SoftMutedGray
                    )
                }

                if (clipboardWipeWarningVisible) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .background(CyberAmber.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .border(1.dp, CyberAmber.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SECURITY NOTE: Address copied to clipboard. Clear clipboard immediately after sharing to prevent malware sniffing.",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Access Actions Buttons: Send & Receive Desk
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { showReceiveDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DeepSlate),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .padding(end = 6.dp)
                    .testTag("dashboard_receive_btn")
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = NeonTeal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RECEIVE", color = NeonTeal, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showSendDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp)
                    .padding(start = 6.dp)
                    .testTag("dashboard_send_btn")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEND", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // History Log Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ledger Activity Ledger", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
            Text("${transactions.size} records", style = MaterialTheme.typography.bodyMedium, color = SoftMutedGray)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transactions activity list
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DeepSlate, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = SoftMutedGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transactions detected on this address.", color = SoftMutedGray)
                    Text("Sync nodes or receive AET to update balance.", color = SoftMutedGray, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_list")
            ) {
                items(transactions) { tx ->
                    TransactionItemView(tx, acc.address)
                }
            }
        }
    }

    // Modal Sheet 1: Receive Dialog
    if (showReceiveDialog) {
        AlertDialog(
            onDismissRequest = { showReceiveDialog = false },
            containerColor = RichOnyx,
            title = {
                Text("Scan Bech32 Recipient URI", color = NeonTeal, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Show this QR code to the sender. This represents your secure Bech32 address.",
                        color = SoftMutedGray,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Real Glowing Vector Qr code
                    CyberQrCode(data = acc.address, sizeDp = 200, glowColor = NeonTeal)

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DeepSlate)
                    ) {
                        Text(
                            text = acc.address,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonTeal,
                            modifier = Modifier.padding(12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReceiveDialog = false }) {
                    Text("Done", color = NeonTeal)
                }
            }
        )
    }

    // Modal Sheet 2: Send Form (Phase 0.83 Risk Engine & Phase 0.37 Fee dest allocation)
    if (showSendDialog) {
        var recipientInput by remember { mutableStateOf("") }
        var amountInput by remember { mutableStateOf("") }
        var memoInput by remember { mutableStateOf("") }
        
        var isTransactionBroadcasting by remember { mutableStateOf(false) }
        var transferResultText by remember { mutableStateOf("") }
        var isTransferSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isTransactionBroadcasting) showSendDialog = false },
            containerColor = RichOnyx,
            title = {
                Text("Broadcast Outbound Funds", color = NeonTeal, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Transfer assets permanently from this non-custodial light node.",
                        color = SoftMutedGray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Mnemonic Recipient Address (aet1...)", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("send_recipient_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount AET", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("send_amount_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = memoInput,
                        onValueChange = { memoInput = it },
                        label = { Text("Memo String (optional)", color = SoftMutedGray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = BorderDark,
                            focusedBorderColor = NeonTeal,
                            unfocusedTextColor = WarmWhite,
                            focusedTextColor = WarmWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phase 0.37: Interactive Fee allocation panel details
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DeepSlate, RoundedCornerShape(10.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Consensus Network Fee:", color = SoftMutedGray, fontSize = 11.sp)
                                Text("0.000010 AET (10 aetons)", color = CyberAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Divider(color = BorderDark)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fee Burn Allocation (90%):", color = SoftMutedGray, fontSize = 11.sp)
                                Text("0.000009 AET", color = PhantomRed, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Validator Miner reward (10%):", color = SoftMutedGray, fontSize = 11.sp)
                                Text("0.000001 AET", color = TerminalGreen, fontSize = 11.sp)
                            }
                        }
                    }

                    if (transferResultText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = transferResultText,
                            color = if (isTransferSuccess) TerminalGreen else PhantomRed,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(
                        onClick = { showSendDialog = false },
                        enabled = !isTransactionBroadcasting
                    ) {
                        Text("Cancel", color = SoftMutedGray)
                    }

                    Button(
                        onClick = {
                            isTransactionBroadcasting = true
                            transferResultText = "Verifying cryptographic credentials..."
                            
                            scope.launch {
                                delay(1200) // Aesthetic delay for transaction signing computation
                                
                                val parsedAmt = (amountInput.toDoubleOrNull() ?: 0.0) * 1_000_000
                                if (parsedAmt <= 0) {
                                    isTransferSuccess = false
                                    transferResultText = "Error: Transfer amount must be positive."
                                    isTransactionBroadcasting = false
                                    return@launch
                                }

                                val result = viewModel.executeSend(
                                    recipient = recipientInput,
                                    amountAetons = parsedAmt.toLong(),
                                    memo = memoInput
                                )

                                isTransferSuccess = result.first
                                transferResultText = result.second
                                isTransactionBroadcasting = false

                                if (isTransferSuccess) {
                                    delay(1000)
                                    showSendDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                        enabled = !isTransactionBroadcasting,
                        modifier = Modifier.testTag("send_commit_btn")
                    ) {
                        Text("Sign & Submit", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun TransactionItemView(tx: TransactionRecord, activeAddress: String) {
    var expanded by remember { mutableStateOf(false) }
    val isInbound = tx.receiver.trim().lowercase() == activeAddress.trim().lowercase()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DeepSlate, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (expanded) NeonTeal.copy(alpha = 0.4f) else BorderDark,
                RoundedCornerShape(12.dp)
            )
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isInbound) TerminalGreen.copy(alpha = 0.15f) else PhantomRed.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isInbound) "↓" else "↑",
                        color = if (isInbound) TerminalGreen else PhantomRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (isInbound) "Funds Received (AET)" else "Transfer Sent (AET)",
                        fontWeight = FontWeight.Bold,
                        color = WarmWhite,
                        fontSize = 14.sp
                    )

                    val timeLabel = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(tx.timestamp * 1000))
                    Text(timeLabel, color = SoftMutedGray, fontSize = 11.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val coins = tx.amountAetons / 1_000_000.0
                Text(
                    text = "${if (isInbound) "+" else "-"}${String.format("%.6f", coins)}",
                    fontWeight = FontWeight.Bold,
                    color = if (isInbound) TerminalGreen else PhantomRed,
                    fontSize = 15.sp
                )

                // Render dynamic status confirmations
                val statusColor = when (tx.status) {
                    "PENDING" -> CyberAmber
                    "FINALIZED" -> NeonTeal
                    else -> TerminalGreen
                }
                Text(
                    text = tx.status,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expandable structural ledger details (No pseudo hashes)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Divider(color = BorderDark)
                Spacer(modifier = Modifier.height(12.dp))

                RowDetail("Tx Block ID / Hash", tx.txId)
                Spacer(modifier = Modifier.height(4.dp))
                RowDetail("Origin sender", tx.sender)
                Spacer(modifier = Modifier.height(4.dp))
                RowDetail("Destination receiver", tx.receiver)
                Spacer(modifier = Modifier.height(4.dp))
                RowDetail("BIP44 Signatures Bytes", tx.signature)
                Spacer(modifier = Modifier.height(4.dp))
                RowDetail("Associated Nonce", tx.nonce.toString())
                if (tx.memo.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    RowDetail("Attached memo", tx.memo)
                }
            }
        }
    }
}

@Composable
fun RowDetail(label: String, value: String) {
    Column {
        Text(text = label.uppercase(), color = SoftMutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 11.sp,
            color = WarmWhite,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
