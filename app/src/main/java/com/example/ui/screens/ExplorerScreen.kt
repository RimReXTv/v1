package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.AetherisCore
import com.example.data.*
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ExplorerScreen(viewModel: AetherisViewModel) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    
    // Result states (use robust, direct State holding to resolve delegate casting limits)
    val searchPerformed = remember { mutableStateOf(false) }
    val foundBlock = remember { mutableStateOf<BlockRecord?>(null) }
    val foundAddressResult = remember { mutableStateOf<AddressExplorerResult?>(null) }
    val errorText = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .padding(16.dp)
    ) {
        Text("Integrated Explorer", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
        Text("Search heights, block hashes, or Bech32 addresses.", style = MaterialTheme.typography.bodyLarge, color = SoftMutedGray)

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search height (e.g. 0) or address (aet1...)", color = SoftMutedGray) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        val query = searchQuery.trim()
                        if (query.isEmpty()) return@IconButton
                        
                        searchPerformed.value = true
                        foundBlock.value = null
                        foundAddressResult.value = null
                        errorText.value = ""

                        scope.launch {
                            // 1. Try to search for block height
                            val heightQuery = query.toLongOrNull()
                            if (heightQuery != null) {
                                val block = viewModel.repository.getBlockByHeight(heightQuery)
                                if (block != null) {
                                    foundBlock.value = block
                                } else {
                                    errorText.value = "Block Height #$heightQuery not found in current validated chain state."
                                }
                            } else {
                                // 2. Try to decode as Bech32 address
                                val decoded = AetherisCore.decodeBech32(query)
                                if (decoded != null) {
                                    // Calculate transactions and local contact info
                                    val txFlow = viewModel.repository.getTransactionsForAddress(query)
                                    val txs = txFlow.first()
                                    
                                    val contactsList = viewModel.repository.allContacts.first()
                                    val matchedContact = contactsList.find { it.address.lowercase() == query.lowercase() }

                                    var totalReceived = 0L
                                    var totalSent = 0L
                                    var inboundCount = 0
                                    var outboundCount = 0

                                    for (tx in txs) {
                                        if (tx.status == "ORPHANED") continue
                                        if (tx.receiver.lowercase() == query.lowercase()) {
                                            totalReceived += tx.amountAetons
                                            inboundCount++
                                        }
                                        if (tx.sender.lowercase() == query.lowercase()) {
                                            totalSent += tx.amountAetons + tx.feeAetons
                                            outboundCount++
                                        }
                                    }

                                    foundAddressResult.value = AddressExplorerResult(
                                        address = query,
                                        balanceAetons = totalReceived - totalSent,
                                        inboundCount = inboundCount,
                                        outboundCount = outboundCount,
                                        contactLabel = matchedContact?.label
                                    )
                                } else {
                                    errorText.value = "Invalid input query. Must be a valid long height or a valid Bech32 address."
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("run_search_btn")
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = NeonTeal)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderDark,
                focusedBorderColor = NeonTeal,
                unfocusedTextColor = WarmWhite,
                focusedTextColor = WarmWhite
            ),
            modifier = Modifier.fillMaxWidth().testTag("explorer_input")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Display results or empty states
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                errorText.value.isNotEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepSlate, RoundedCornerShape(12.dp))
                            .padding(24.dp)
                    ) {
                        Text(errorText.value, color = PhantomRed, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                foundBlock.value != null -> {
                    BlockResultCard(foundBlock.value!!)
                }

                foundAddressResult.value != null -> {
                    AddressResultCard(foundAddressResult.value!!)
                }

                !searchPerformed.value -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DeepSlate, RoundedCornerShape(12.dp)),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("Search a validated target to display ledger facts.", color = SoftMutedGray)
                    }
                }
            }
        }
    }
}

data class AddressExplorerResult(
    val address: String,
    val balanceAetons: Long,
    val inboundCount: Int,
    val outboundCount: Int,
    val contactLabel: String?
)

@Composable
fun BlockResultCard(block: BlockRecord) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate, RoundedCornerShape(12.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Block Heights Record #${block.height}", style = MaterialTheme.typography.headlineMedium, color = NeonTeal)
        Spacer(modifier = Modifier.height(16.dp))

        FactItem("Block Hash Identity", block.hash)
        FactItem("Previous Hash Link", block.parentHash)
        FactItem("Merkle Tree Root Hash", block.merkleRoot)
        FactItem("Validators Public Signer", block.validatorPublicKey)
        FactItem("Validator Block Signature", block.blockSignature)
        FactItem("Consensus Timestamp", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(block.timestamp * 1000)))
        FactItem("Transactions Count", "${block.transactionCount} transactions included")
    }
}

@Composable
fun AddressResultCard(res: AddressExplorerResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate, RoundedCornerShape(12.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Bech32 Account Audit", style = MaterialTheme.typography.headlineMedium, color = CyberAmber)
        Spacer(modifier = Modifier.height(16.dp))

        FactItem("Target address", res.address)
        if (res.contactLabel != null) {
            FactItem("Saved contact reference", res.contactLabel)
        }
        FactItem("Local ledger balance", "${res.balanceAetons / 1_000_000.0} AET")
        FactItem("Inbound transaction entries", "${res.inboundCount} received")
        FactItem("Outbound transaction entries", "${res.outboundCount} sent")
    }
}

@Composable
fun FactItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label.uppercase(), color = SoftMutedGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontSize = 12.sp,
            color = WarmWhite,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = BorderDark)
    }
}
