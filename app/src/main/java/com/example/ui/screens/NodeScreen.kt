package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PeerRecord
import com.example.ui.AetherisViewModel
import com.example.ui.theme.*

@Composable
fun NodeScreen(viewModel: AetherisViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val height by viewModel.currentHeight.collectAsState()
    val peerCount by viewModel.activePeersCount.collectAsState()
    val latency by viewModel.nodeLatencyMs.collectAsState()
    val health by viewModel.networkHealth.collectAsState()
    val peersList by viewModel.peers.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .padding(16.dp)
    ) {
        Text("Light Node Participation", style = MaterialTheme.typography.headlineMedium, color = WarmWhite)
        Text("Your node acts as an availability snapshot witness.", style = MaterialTheme.typography.bodyLarge, color = SoftMutedGray)

        Spacer(modifier = Modifier.height(20.dp))

        // Large Telemetry Console Block (Aesthetic, clean, functional)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TelemetryRow("Sync Status", if (isSyncing) "ACTIVE witnesses headers" else "IDLE offline", if (isSyncing) TerminalGreen else PhantomRed)
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                TelemetryRow("Consensus Height", "#$height blocks", NeonTeal)
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                TelemetryRow("Network Latency", "$latency ms", if (latency > 250) CyberAmber else TerminalGreen)
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                TelemetryRow("Active Seed Peers", "$peerCount connected", NeonTeal)
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                TelemetryRow("Status Line", health, CyberAmber)
                Divider(color = BorderDark, modifier = Modifier.padding(vertical = 8.dp))
                TelemetryRow("Witness Role", "Proof of Availability SNAPSHOT witnessed", TerminalGreen)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Direct Control Buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            if (isSyncing) {
                Button(
                    onClick = { viewModel.nodeEngine.stopSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = PhantomRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("stop_sync_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Disconnect Node", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.nodeEngine.startSync() },
                    colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("start_sync_btn")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Synchronize Client Now", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // P2P Trust/Scores peer lists
        Text("P2P Peer Reputation Directory", style = MaterialTheme.typography.titleLarge, color = WarmWhite)
        Spacer(modifier = Modifier.height(12.dp))

        if (peersList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DeepSlate, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No peers loaded.", color = SoftMutedGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("peer_telemetry_list")
            ) {
                items(peersList) { peer ->
                    PeerReputationRow(peer)
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = SoftMutedGray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PeerReputationRow(peer: PeerRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DeepSlate, RoundedCornerShape(10.dp))
            .border(1.dp, if (peer.isBanned) PhantomRed.copy(alpha = 0.3f) else BorderDark, RoundedCornerShape(10.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = peer.ipAddress,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontSize = 13.sp,
                color = WarmWhite
            )
            val updatedLabel = if (peer.isBanned) "Banned: Spam block broadcast" else "Active tracker seed"
            Text(
                text = updatedLabel,
                fontSize = 11.sp,
                color = if (peer.isBanned) PhantomRed else SoftMutedGray
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Trust: ${peer.score}%",
                    fontWeight = FontWeight.Bold,
                    color = if (peer.isBanned) PhantomRed else if (peer.score > 80) TerminalGreen else CyberAmber,
                    fontSize = 13.sp
                )
                Text(
                    text = "Ping: ${peer.latencyMs}ms",
                    fontSize = 11.sp,
                    color = SoftMutedGray
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (peer.isBanned) PhantomRed else if (peer.score > 80) TerminalGreen else CyberAmber)
            )
        }
    }
}
