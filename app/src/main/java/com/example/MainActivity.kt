package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val viewModel: AetherisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AetherisTheme {
                val screen by viewModel.currentScreen.collectAsState()
                val isLocked by viewModel.isAppLocked.collectAsState()
                val activeAccount by viewModel.activeAccount.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Render bottom navigation rails only when wallet is loaded and unlocked
                        if (activeAccount != null && !isLocked && screen != AetherisScreen.SETUP) {
                            AetherisBottomBar(
                                currentScreen = screen,
                                onSelect = { viewModel.navigateTo(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(RichOnyx)
                            .padding(innerPadding)
                    ) {
                        when {
                            // 1. App Session Locked Gate (Phase 2 app safety)
                            isLocked && activeAccount != null -> {
                                SessionLockScreen(
                                    onUnlock = { viewModel.unlockApp() }
                                )
                            }

                            // 2. Wallet Onboarding Setup State
                            activeAccount == null || screen == AetherisScreen.SETUP -> {
                                SetupScreen(viewModel)
                            }

                            // 3. Main Console Layouts
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    AetherisHeader(viewModel)
                                    Box(modifier = Modifier.weight(1f)) {
                                        when (screen) {
                                            AetherisScreen.DASHBOARD -> DashboardScreen(viewModel)
                                            AetherisScreen.NODE -> NodeScreen(viewModel)
                                            AetherisScreen.EXPLORER -> ExplorerScreen(viewModel)
                                            AetherisScreen.CONTACTS -> ContactsScreen(viewModel)
                                            AetherisScreen.VAULT -> VaultScreen(viewModel)
                                            else -> DashboardScreen(viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AetherisHeader(viewModel: AetherisViewModel) {
    val activeNet by viewModel.activeNetwork.collectAsState()
    val height by viewModel.currentHeight.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(RichOnyx)
            .border(0.5.dp, BorderDark, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "AÆTHERIS CONSOLE",
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = SoftMutedGray,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(TerminalGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${activeNet.name} : BLOCK #$height",
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = NeonTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        IconButton(
            onClick = { viewModel.lockApp() },
            modifier = Modifier
                .background(DeepSlate, RoundedCornerShape(8.dp))
                .size(36.dp)
                .testTag("app_lock_header_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock App",
                tint = NeonTeal,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun AetherisBottomBar(currentScreen: AetherisScreen, onSelect: (AetherisScreen) -> Unit) {
    NavigationBar(
        containerColor = DeepSlate,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(0.5.dp, BorderDark, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("bottom_nav")
    ) {
        val navItems = listOf(
            NavigationItem(AetherisScreen.DASHBOARD, Icons.Default.Home, "Wallet"),
            NavigationItem(AetherisScreen.NODE, Icons.Default.List, "Node"),
            NavigationItem(AetherisScreen.EXPLORER, Icons.Default.Search, "Explorer"),
            NavigationItem(AetherisScreen.CONTACTS, Icons.Default.Person, "Contacts"),
            NavigationItem(AetherisScreen.VAULT, Icons.Default.Settings, "Vault")
        )

        navItems.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color(0xFF001D36) else SoftMutedGray
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) Color(0xFF001D36) else SoftMutedGray
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = CyberAmber,
                    selectedIconColor = Color(0xFF001D36),
                    unselectedIconColor = SoftMutedGray,
                    selectedTextColor = Color(0xFF001D36),
                    unselectedTextColor = SoftMutedGray
                ),
                modifier = Modifier.testTag("nav_item_${item.screen.name.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val screen: AetherisScreen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun SessionLockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RichOnyx)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(CyberAmber.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, CyberAmber, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CyberAmber,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Wallet Session Encrypted",
            style = MaterialTheme.typography.displayMedium,
            color = WarmWhite,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Biometric or Device Credentials authentication is required to access your cryptographic keys and transaction memos.",
            style = MaterialTheme.typography.bodyLarge,
            color = SoftMutedGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = NeonTeal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("unlock_biometric_btn")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock with Biometrics / PIN", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
