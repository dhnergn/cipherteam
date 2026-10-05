package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.Member
import com.example.ui.theme.LightSlate
import com.example.ui.theme.TextMuted

@Composable
fun ChatListScreen(
    channels: List<Channel>,
    members: List<Member>,
    selectedChannelId: String?,
    globalCipherMode: Boolean,
    onChannelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredChannels = channels.filter {
        it.name.contains(searchQuery, ignoreCase = true) || 
        it.description.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search secure tunnels...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_tunnels_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = LightSlate,
                    focusedContainerColor = SlateGreyBackground(),
                    unfocusedContainerColor = SlateGreyBackground()
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chats list header
        Text(
            text = "ACTIVE SECURE CHANNELS",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = LightSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No channels match search",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("channels_list")
            ) {
                items(filteredChannels, key = { it.id }) { channel ->
                    val isSelected = channel.id == selectedChannelId
                    val isGroup = channel.isGroup
                    
                    // In DMs, get the corresponding member's detail
                    val dmMember = if (!isGroup) {
                        val memberId = when (channel.id) {
                            "direct-alice" -> "alice"
                            "direct-bob" -> "bob"
                            "direct-carol" -> "carol"
                            else -> ""
                        }
                        members.find { it.id == memberId }
                    } else null

                    ChannelItem(
                        channel = channel,
                        dmMember = dmMember,
                        isSelected = isSelected,
                        globalCipherMode = globalCipherMode,
                        onClick = { onChannelSelected(channel.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    channel: Channel,
    dmMember: Member?,
    isSelected: Boolean,
    globalCipherMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("channel_item_${channel.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) LightSlate else Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Indicator
            if (channel.isGroup) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LightSlate, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Group",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else if (dmMember != null) {
                val avatarColor = try {
                    Color(android.graphics.Color.parseColor(dmMember.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.secondary
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(avatarColor, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dmMember.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LightSlate, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AlternateEmail,
                        contentDescription = "Direct",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "E2E Encrypted",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Last Message
                val displayMsg = if (channel.lastMessage != null) {
                    if (globalCipherMode) {
                        // Simulate viewing raw Base64 CipherText
                        "scrypt_aes256: " + android.util.Base64.encodeToString(
                            channel.lastMessage.take(15).toByteArray(),
                            android.util.Base64.NO_WRAP
                        ).take(22) + "..."
                    } else {
                        channel.lastMessage
                    }
                } else {
                    channel.description
                }

                Text(
                    text = displayMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (channel.lastMessage != null) TextMuted else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Timestamp or status
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val timeString = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(channel.lastMessageTime))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Beautiful secure active tunnel indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun SlateGreyBackground(): Color {
    return Color(0xFF131922)
}
