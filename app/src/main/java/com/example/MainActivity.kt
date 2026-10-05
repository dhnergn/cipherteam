package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatViewModel
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.LightSlate
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SecureYellow
import com.example.ui.theme.SlateGrey
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

sealed class Screen {
    object TunnelsList : Screen()
    data class ActiveChat(val channelId: String) : Screen()
    object Vault : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val viewModel: ChatViewModel = viewModel()
    
    // UI state observers
    val channels by viewModel.channels.collectAsState()
    val members by viewModel.members.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val selectedChannelId by viewModel.selectedChannelId.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val globalCipherMode by viewModel.globalCipherMode.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    // Navigation and layout tracking
    var currentScreen by remember { mutableStateOf<Screen>(Screen.TunnelsList) }
    var bottomTabSelected by remember { mutableStateOf(0) } // 0 = Chats, 1 = Vault

    // Responsive design thresholds: Large screen/tablets are >= 600.dp
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        // If user is on a tablet (wide screen), we can show the dual-pane layout for Chats
        val showDualPane = isWideScreen && bottomTabSelected == 0

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopSecureBar(
                    currentScreen = currentScreen,
                    globalCipherMode = globalCipherMode,
                    showDualPane = showDualPane,
                    onBackClick = {
                        currentScreen = Screen.TunnelsList
                    },
                    onToggleCipherMode = {
                        viewModel.toggleGlobalCipherMode()
                    }
                )
            },
            bottomBar = {
                SecureBottomNavigation(
                    selectedTab = bottomTabSelected,
                    onTabSelected = { tab ->
                        bottomTabSelected = tab
                        currentScreen = if (tab == 0) Screen.TunnelsList else Screen.Vault
                    }
                )
            }
        ) { innerPadding ->
            val paddingModifier = Modifier.padding(innerPadding)

            // Handle hardware back button in single-pane mode
            if (!isWideScreen && currentScreen is Screen.ActiveChat) {
                BackHandler {
                    currentScreen = Screen.TunnelsList
                }
            }

            Box(modifier = paddingModifier.fillMaxSize()) {
                if (bottomTabSelected == 1) {
                    // Vault tab is active
                    VaultScreen(
                        members = members,
                        auditLogs = auditLogs,
                        onRotateKeys = { viewModel.rotateCredentials() },
                        onClearChatHistory = { viewModel.clearChatHistory() },
                        onRefreshLogs = { viewModel.refreshAuditLogs() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Chats tab is active
                    if (showDualPane) {
                        // Wide dual-pane view: Master (List) | Detail (Chat Thread)
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Channel List (Master)
                            ChatListScreen(
                                channels = channels,
                                members = members,
                                selectedChannelId = selectedChannelId,
                                globalCipherMode = globalCipherMode,
                                onChannelSelected = { channelId ->
                                    viewModel.selectChannel(channelId)
                                },
                                modifier = Modifier
                                    .width(300.dp)
                                    .fillMaxHeight()
                            )

                            // Divider line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(LightSlate)
                            )

                            // Right Pane: Active Chat Thread (Detail)
                            val activeChannel = channels.find { it.id == selectedChannelId }
                            if (activeChannel != null) {
                                ChatScreen(
                                    channel = activeChannel,
                                    messages = activeMessages,
                                    members = members,
                                    isTyping = isTyping,
                                    globalCipherMode = globalCipherMode,
                                    onSendMessage = { text ->
                                        viewModel.sendMessage(text)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            } else {
                                // Empty state when no channel selected
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .background(SpaceDark),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Shield,
                                            contentDescription = "Shield logo",
                                            tint = LightSlate,
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "SECURE E2E WORKSPACE",
                                            fontWeight = FontWeight.Bold,
                                            color = TextLight,
                                            letterSpacing = 1.2.sp,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Select an encrypted tunnel on the left to begin discussing confidential projects.",
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Narrow single-pane view: Toggle between List and Detail
                        when (val screen = currentScreen) {
                            Screen.TunnelsList -> {
                                ChatListScreen(
                                    channels = channels,
                                    members = members,
                                    selectedChannelId = selectedChannelId,
                                    globalCipherMode = globalCipherMode,
                                    onChannelSelected = { channelId ->
                                        viewModel.selectChannel(channelId)
                                        currentScreen = Screen.ActiveChat(channelId)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            is Screen.ActiveChat -> {
                                val activeChannel = channels.find { it.id == screen.channelId }
                                if (activeChannel != null) {
                                    ChatScreen(
                                        channel = activeChannel,
                                        messages = activeMessages,
                                        members = members,
                                        isTyping = isTyping,
                                        globalCipherMode = globalCipherMode,
                                        onSendMessage = { text ->
                                            viewModel.sendMessage(text)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Fallback to list if error
                                    currentScreen = Screen.TunnelsList
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopSecureBar(
    currentScreen: Screen,
    globalCipherMode: Boolean,
    showDualPane: Boolean,
    onBackClick: () -> Unit,
    onToggleCipherMode: () -> Unit
) {
    Surface(
        color = SlateGrey,
        modifier = Modifier.statusBarsPadding(),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // If on single-pane and inside active chat, show Back arrow
                if (!showDualPane && currentScreen is Screen.ActiveChat) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Logo",
                        tint = CyberGreen,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("app_shield_logo")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = "CipherTeam",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                // Secure server status capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "ONLINE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberGreen
                    )
                }
            }

            // Database Cipher Overlay mode toggle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleCipherMode,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("global_cipher_toggle"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (globalCipherMode) SecureYellow.copy(alpha = 0.15f) else Color.Transparent,
                        contentColor = if (globalCipherMode) SecureYellow else TextMuted
                    )
                ) {
                    Icon(
                        imageVector = if (globalCipherMode) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Database View Mode",
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (globalCipherMode) "CIPHER" else "PLAIN",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (globalCipherMode) SecureYellow else TextMuted
                )
            }
        }
    }
}

@Composable
fun SecureBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = SlateGrey,
        modifier = Modifier.navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            label = { Text("Secure Tunnels", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Chats",
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpaceDark,
                selectedTextColor = CyberGreen,
                indicatorColor = CyberGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("tunnels_tab")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            label = { Text("Security Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Vault",
                    modifier = Modifier.size(20.dp)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpaceDark,
                selectedTextColor = CyberPurple,
                indicatorColor = CyberPurple,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("vault_tab")
        )
    }
}
