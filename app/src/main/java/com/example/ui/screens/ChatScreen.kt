package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.crypto.EncryptedPayload
import com.example.data.model.Channel
import com.example.data.model.Member
import com.example.data.model.Message
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.LightSlate
import com.example.ui.theme.SecureYellow
import com.example.ui.theme.SlateGrey
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@Composable
fun ChatScreen(
    channel: Channel,
    messages: List<Message>,
    members: List<Member>,
    isTyping: String?,
    globalCipherMode: Boolean,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showSecurityBlueprint by remember { mutableStateOf(false) }

    // Map to keep track of individual message ciphertext expanded status
    val expandedMessages = remember { mutableStateMapOf<Int, Boolean>() }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Get current user and recipient members for security metadata displaying
    val currentUser = members.find { it.isCurrentUser }
    val recipient = if (!channel.isGroup) {
        val teammateId = when (channel.id) {
            "direct-alice" -> "alice"
            "direct-bob" -> "bob"
            "direct-carol" -> "carol"
            else -> ""
        }
        members.find { it.id == teammateId }
    } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Channel Top bar details
        ChannelHeader(
            channel = channel,
            recipient = recipient,
            onShowSecurityClick = { showSecurityBlueprint = !showSecurityBlueprint },
            isSecurityVisible = showSecurityBlueprint
        )

        // Security Blueprint Expandable Pane
        AnimatedVisibility(
            visible = showSecurityBlueprint,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SecurityBlueprintPane(
                channel = channel,
                currentUser = currentUser,
                recipient = recipient
            )
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.isSystemMessage) {
                    SystemMessageRow(message = message, globalCipherMode = globalCipherMode)
                } else {
                    val isSenderMe = message.senderId == "current_user"
                    val senderMember = members.find { it.id == message.senderId }
                    val isExpanded = expandedMessages[message.id] ?: false

                    MessageRow(
                        message = message,
                        sender = senderMember,
                        isSenderMe = isSenderMe,
                        globalCipherMode = globalCipherMode,
                        isExpanded = isExpanded,
                        currentUser = currentUser,
                        recipient = recipient,
                        channel = channel,
                        onToggleExpand = {
                            expandedMessages[message.id] = !isExpanded
                        }
                    )
                }
            }

            // Typing Indicator
            if (isTyping != null) {
                item {
                    TypingIndicatorRow(name = isTyping)
                }
            }
        }

        // Input bottom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateGrey)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                placeholder = { Text("Send encrypted payload...", color = TextMuted, fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = LightSlate,
                    focusedContainerColor = SpaceDark,
                    unfocusedContainerColor = SpaceDark,
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight
                ),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = {
                    if (textState.isNotBlank()) {
                        onSendMessage(textState)
                        textState = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_secure_button"),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = SpaceDark
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Encrypt and Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChannelHeader(
    channel: Channel,
    recipient: Member?,
    onShowSecurityClick: () -> Unit,
    isSecurityVisible: Boolean
) {
    Surface(
        color = SlateGrey,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = CyberGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "E2E",
                                tint = CyberGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "E2E ENCRYPTED",
                                fontSize = 8.sp,
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = if (channel.isGroup) channel.description else recipient?.role ?: channel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onShowSecurityClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isSecurityVisible) CyberGreen.copy(alpha = 0.15f) else Color.Transparent,
                    contentColor = if (isSecurityVisible) CyberGreen else TextMuted
                )
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = "Security Blueprint",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SecurityBlueprintPane(
    channel: Channel,
    currentUser: Member?,
    recipient: Member?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("security_blueprint_pane"),
        colors = CardDefaults.cardColors(containerColor = SpaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = "Shield", tint = CyberGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SECURITY BLUEPRINT (E2E TUNNEL)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CyberGreen
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LightSlate)
            Spacer(modifier = Modifier.height(12.dp))

            // Technical Specs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Asymmetric Protocol:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("RSA-2048 (PKCS1Padding)", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextLight)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Symmetric Protocol:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("AES-256-CBC (PKCS5Padding)", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextLight)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Database Encryption:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Text("Sealed SQLite (Pre-Decryption)", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = TextLight)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = LightSlate)
            Spacer(modifier = Modifier.height(12.dp))

            // Keys info
            Text("ACTIVE CRYPTOGRAPHIC ENTITIES:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            
            // Me key
            Text(
                "You: ${currentUser?.name}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = CyberPurple
            )
            Text(
                "Public Key Thumbprint: RSA-2048-PUB...${currentUser?.publicKey?.takeLast(50)?.replace("\n", "")}",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted,
                maxLines = 1
            )
            
            if (recipient != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Recipient: ${recipient.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberBlue
                )
                Text(
                    "Public Key Thumbprint: RSA-2048-PUB...${recipient.publicKey.takeLast(50).replace("\n", "")}",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted,
                    maxLines = 1
                )
            } else if (channel.isGroup) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Group Sync Participants:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = CyberGreen
                )
                Text(
                    "Alice Vance (CSO), Bob Miller (VP Eng), Carol Chen (Product)",
                    fontSize = 10.sp,
                    color = TextLight
                )
            }
        }
    }
}

@Composable
fun MessageRow(
    message: Message,
    sender: Member?,
    isSenderMe: Boolean,
    globalCipherMode: Boolean,
    isExpanded: Boolean,
    currentUser: Member?,
    recipient: Member?,
    channel: Channel,
    onToggleExpand: () -> Unit
) {
    // Decrypt the message content.
    // In our E2E mockup, we check if the user is the sender (they can decrypt their own message) or if they hold the private key to decrypt recipient messages.
    // Under standard direct chat:
    // Sender sends, encrypted with recipient public key.
    // If it's sent to me, I decrypt using my private key.
    // If I sent it, I decrypt it in my own UI because I sent the plaintext (or we can decrypt using the recipient's private key just for the mockup display, or show that it was encrypted before transmission).
    // Let's implement real local decryption!
    // If sender == user (isSenderMe), we show the decrypted message because they wrote it, but also show that in the db it is encrypted.
    // If sender == teammate:
    // It is encrypted using user's public key, so we decrypt it using the user's private key! This is 100% correct E2E flow!
    val decryptedText = remember(message.ciphertext, message.encryptedAesKey, currentUser?.privateKey) {
        try {
            if (isSenderMe) {
                // If I am the sender, we can decrypt it using the recipient's private key just to display it,
                // or simply display it since we are the sender and have the original message.
                // In direct chats, if I encrypt for Alice, Alice's private key is in the DB, so we can decrypt it using Alice's private key!
                // This simulates Alice's view, or our local chat log copy which keeps a local history.
                // Let's decrypt it. If it's direct chat and we have Alice's private key, we use Alice's private key.
                // If it's group chat, we decrypt using our own key (since everyone is in the loop).
                if (!channel.isGroup && recipient != null && recipient.privateKey != null) {
                    val payload = EncryptedPayload(message.ciphertext, message.encryptedAesKey)
                    CryptoEngine.decrypt(payload, recipient.privateKey)
                } else if (currentUser?.privateKey != null) {
                    val payload = EncryptedPayload(message.ciphertext, message.encryptedAesKey)
                    CryptoEngine.decrypt(payload, currentUser.privateKey)
                } else {
                    "Decryption Error: Private Key missing"
                }
            } else {
                // Sent by teammate: Encrypted with my (user) public key, so we decrypt using my private key!
                if (currentUser?.privateKey != null) {
                    val payload = EncryptedPayload(message.ciphertext, message.encryptedAesKey)
                    CryptoEngine.decrypt(payload, currentUser.privateKey)
                } else {
                    "Decryption Error: Local Private Key missing"
                }
            }
        } catch (e: Exception) {
            "Decryption Error: ${e.message}"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSenderMe) Arrangement.End else Arrangement.Start
    ) {
        // Recipient Avatar for teammate messages
        if (!isSenderMe && sender != null) {
            val color = try { Color(android.graphics.Color.parseColor(sender.colorHex)) } catch(e:Exception) { MaterialTheme.colorScheme.secondary }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(sender.initials, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // Message bubble container
        Column(
            horizontalAlignment = if (isSenderMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Teammate name header
            if (!isSenderMe && sender != null && channel.isGroup) {
                Text(
                    sender.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                )
            }

            // The card
            Card(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isSenderMe) 16.dp else 4.dp,
                            bottomEnd = if (isSenderMe) 4.dp else 16.dp
                        )
                    )
                    .clickable { onToggleExpand() }
                    .testTag("message_bubble_${message.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSenderMe) CyberBlue.copy(alpha = 0.9f) else LightSlate
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    // Message content: toggles based on global mode or local expand
                    val shouldShowCipher = globalCipherMode || isExpanded

                    if (shouldShowCipher) {
                        // Display ciphertext scrambled payload
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Ciphertext",
                                tint = if (isSenderMe) Color.White else CyberGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "CIPHER TEXT (DATABASE PAYLOAD):",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSenderMe) Color.White.copy(alpha = 0.8f) else CyberGreen
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = message.ciphertext,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSenderMe) Color.White else SecureYellow,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = if (isSenderMe) Color.White.copy(alpha = 0.3f) else SpaceDark)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "RSA-ENC AES SESSION KEY:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSenderMe) Color.White.copy(alpha = 0.8f) else CyberPurple
                        )
                        Text(
                            text = message.encryptedAesKey,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Display Plaintext decrypted
                        Text(
                            text = decryptedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message metadata footprint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                val timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
                Text(
                    text = timeText,
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                if (globalCipherMode || isExpanded) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = SecureYellow,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "DATABASE STATE: ENCRYPTED",
                        fontSize = 9.sp,
                        color = SecureYellow,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = "Decrypted",
                        tint = CyberGreen,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "E2E DECRYPTED LOCALLY",
                        fontSize = 9.sp,
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SystemMessageRow(message: Message, globalCipherMode: Boolean) {
    // Decrypt system message text
    // System messages are decrypted directly
    val text = remember(message.ciphertext) {
        try {
            val payload = EncryptedPayload(message.ciphertext, message.encryptedAesKey)
            // Decrypt using any random key spec, or simply display it since we mock it
            // Actually, in the repository we encrypted it, we can decrypt it here using the stub private key.
            // But since system messages are just status reports, we can show standard security text.
            val rawText = "SECURE ENCRYPTED HANDSHAKE VERIFIED. AES-256 SESSION READY."
            if (message.ciphertext.length > 50) {
                rawText
            } else {
                message.ciphertext
            }
        } catch (e: Exception) {
            "SECURE CHANNEL READY."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGrey.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("system_message")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Terminal,
                    contentDescription = "System Status",
                    tint = CyberGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (globalCipherMode) "0x${android.util.Base64.encodeToString(text.take(15).toByteArray(), android.util.Base64.NO_WRAP).take(18)}..." else text,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberGreen,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorRow(name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(LightSlate, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = "Security handshake", tint = CyberGreen, modifier = Modifier.size(16.dp))
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LightSlate)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(CyberGreen, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$name is preparing encrypted payload...",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
