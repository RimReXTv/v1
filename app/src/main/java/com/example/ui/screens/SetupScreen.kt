package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*

@Composable
fun SetupScreen(viewModel: AetherisViewModel) {
    val scrollState = rememberScrollState()
    var currentTab by remember { mutableStateOf(0) } // 0: Create, 1: Import, 2: Watch-Only

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Aetheris Branded Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(NeonTeal, ElectricCyan)),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AÆ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AETHERIS",
                style = MaterialTheme.typography.displayMedium,
                color = NeonTeal,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Decentralized Single-Coin Light Node Wallet",
                style = MaterialTheme.typography.labelLarge,
                color = SoftMutedGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Custom Glassmorphic tab headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSlate, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("CREATE", "IMPORT", "WATCH").forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (currentTab == index) NeonTeal else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { currentTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (currentTab == index) Color.White else SoftMutedGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Active Onboarding Context Display
            when (currentTab) {
                0 -> CreateWalletLayout(viewModel)
                1 -> ImportWalletLayout(viewModel)
                2 -> WatchOnlyLayout(viewModel)
            }
        }
    }
}

@Composable
fun CreateWalletLayout(viewModel: AetherisViewModel) {
    var walletName by remember { mutableStateOf("") }
    var selectWordsCount by remember { mutableStateOf(12) }
    val pendingMnemonic by viewModel.pendingMnemonic.collectAsState()
    
    // Recovery validation variables
    var showBackupValidationStep by remember { mutableStateOf(false) }
    var selectedWordIndexes = remember { mutableStateListOf<String>() } // Track selected validation words
    var validationErrorText by remember { mutableStateOf("") }

    if (!showBackupValidationStep) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Secure local BIP-39 Identity",
                style = MaterialTheme.typography.headlineMedium,
                color = CyberAmber
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your identity and private key is generated client-side from the secure random mnemonic seed. All data is kept permanently offline.",
                style = MaterialTheme.typography.bodyLarge,
                color = SoftMutedGray
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = walletName,
                onValueChange = { walletName = it },
                label = { Text("Wallet Node Name (e.g. Node-1)", color = SoftMutedGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderDark,
                    focusedBorderColor = NeonTeal,
                    unfocusedTextColor = WarmWhite,
                    focusedTextColor = WarmWhite
                ),
                modifier = Modifier.fillMaxWidth().testTag("create_wallet_label_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Mnemonic Word Count", color = WarmWhite, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(12, 18, 24).forEach { count ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .border(
                                1.dp,
                                if (selectWordsCount == count) NeonTeal else BorderDark,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectWordsCount = count }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count Words",
                            color = if (selectWordsCount == count) NeonTeal else SoftMutedGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.draftMnemonic(selectWordsCount)
                    showBackupValidationStep = true
                    selectedWordIndexes.clear()
                    validationErrorText = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("draft_mnemonic_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Backup Seed Phrase", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // Recovery Verification Flow (Phase 1.2 Mnemonic back-up check)
        val wordList = pendingMnemonic.split(" ")
        
        // Random validation targets chosen once
        val validationIndices = remember(pendingMnemonic) {
            listOf(0, wordList.size / 2, wordList.size - 1).shuffled()
        }
        val targetWordsStr = validationIndices.map { wordList[it] }.joinToString(", ")

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Validate Backup Security",
                style = MaterialTheme.typography.headlineMedium,
                color = CyberAmber
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Write down your secure seed phrase in the exact index sequence shown below. Never share this with anyone.",
                style = MaterialTheme.typography.bodyLarge,
                color = SoftMutedGray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glowing seed phrase grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepSlate, RoundedCornerShape(12.dp))
                    .border(1.dp, NeonTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    wordList.chunked(3).forEachIndexed { rowIndex, chunk ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            chunk.forEachIndexed { colIndex, word ->
                                val actualIdx = rowIndex * 3 + colIndex + 1
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .background(RichOnyx, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "$actualIdx. $word",
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = NeonTeal,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Validation: select the matching keywords in order:\n[$targetWordsStr]",
                style = MaterialTheme.typography.labelLarge,
                color = CyberAmber,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Input check selections
            val shuffledWords = remember(pendingMnemonic) { wordList.shuffled() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                shuffledWords.take(4).forEach { word ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                if (selectedWordIndexes.contains(word)) NeonTeal else DeepSlate,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (selectedWordIndexes.contains(word)) {
                                    selectedWordIndexes.remove(word)
                                } else {
                                    selectedWordIndexes.add(word)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = word,
                            color = if (selectedWordIndexes.contains(word)) Color.Black else WarmWhite,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (validationErrorText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(validationErrorText, color = PhantomRed, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearPendingMnemonic()
                        showBackupValidationStep = false
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftMutedGray),
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val expected = validationIndices.map { wordList[it] }
                        if (selectedWordIndexes.toList() == expected) {
                            viewModel.commitGeneratedWallet(walletName)
                        } else {
                            validationErrorText = "Sequence verification failed. Ensure elements are in the exact order."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                    modifier = Modifier.weight(1.5f).padding(start = 6.dp).testTag("commit_wallet_btn")
                ) {
                    Text("Verify & Create", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ImportWalletLayout(viewModel: AetherisViewModel) {
    var label by remember { mutableStateOf("") }
    var mnemonicInput by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var resultSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Restore Wallet Node",
            style = MaterialTheme.typography.headlineMedium,
            color = CyberAmber
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your offline physical 12, 18, or 24 mnemonic recovery words in order. Entries must be separated by spaces.",
            style = MaterialTheme.typography.bodyLarge,
            color = SoftMutedGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Account Label (optional)", color = SoftMutedGray) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderDark,
                focusedBorderColor = NeonTeal,
                unfocusedTextColor = WarmWhite,
                focusedTextColor = WarmWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = mnemonicInput,
            onValueChange = { mnemonicInput = it },
            label = { Text("Key phrase (words separated by space)", color = SoftMutedGray) },
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderDark,
                focusedBorderColor = CyberAmber,
                unfocusedTextColor = WarmWhite,
                focusedTextColor = WarmWhite
            ),
            modifier = Modifier.fillMaxWidth().testTag("mnemonic_input")
        )

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = resultText,
                color = if (resultSuccess) TerminalGreen else PhantomRed,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val res = viewModel.importWallet(label, mnemonicInput)
                resultSuccess = res.first
                resultText = res.second
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("import_wallet_btn")
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Mnemonic Seed", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WatchOnlyLayout(viewModel: AetherisViewModel) {
    var label by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var resultSuccess by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Watch-Only Ledger Node",
            style = MaterialTheme.typography.headlineMedium,
            color = CyberAmber
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Monitor any address on the Aetheris blockchain without importing seed material. Watch-only nodes can view ledger receipts but cannot submit or sign operations.",
            style = MaterialTheme.typography.bodyLarge,
            color = SoftMutedGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("Account Identifier Name", color = SoftMutedGray) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderDark,
                focusedBorderColor = NeonTeal,
                unfocusedTextColor = WarmWhite,
                focusedTextColor = WarmWhite
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = addressInput,
            onValueChange = { addressInput = it },
            label = { Text("Aetheris address (aet1...)", color = SoftMutedGray) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderDark,
                focusedBorderColor = NeonTeal,
                unfocusedTextColor = WarmWhite,
                focusedTextColor = WarmWhite
            ),
            modifier = Modifier.fillMaxWidth().testTag("watch_address_input")
        )

        if (resultText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = resultText,
                color = if (resultSuccess) TerminalGreen else PhantomRed,
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val res = viewModel.importWatchOnly(label, addressInput)
                resultSuccess = res.first
                resultText = res.second
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("watch_only_btn")
        ) {
            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Watch-Only Address", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
