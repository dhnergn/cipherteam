package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.LightSlate
import com.example.ui.theme.SecureRed
import com.example.ui.theme.SecureYellow
import com.example.ui.theme.SlateGrey
import com.example.ui.theme.SpaceDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.TextMuted

@Composable
fun VaultScreen(
    members: List<Member>,
    auditLogs: List<String>,
    onRotateKeys: () -> Unit,
    onClearChatHistory: () -> Unit,
    onRefreshLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = members.find { it.isCurrentUser }
    val teammates = members.filter { !it.isCurrentUser }

    var showPrivateKey by remember { mutableStateOf(false) }
    var showPublicKey by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Security Vault",
                    tint = CyberGreen,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "SECURITY VAULT & KEYRING",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        "Manage on-device cryptographic keyrings & system audit logs",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }

        // --- Current User Cryptographic Identity Card ---
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("user_identity_card"),
                    colors = CardDefaults.cardColors(containerColor = SlateGrey)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(CyberPurple, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Key, contentDescription = "User Identity", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    currentUser.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    currentUser.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyberPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = LightSlate)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Public Key Expandable
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPublicKey = !showPublicKey }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Public Key (RSA-2048)", style = MaterialTheme.typography.bodyMedium, color = TextLight)
                            Icon(
                                if (showPublicKey) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextMuted
                            )
                        }

                        AnimatedVisibility(
                            visible = showPublicKey,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SpaceDark)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = currentUser.publicKey,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = CyberGreen,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { copyToClipboard(currentUser.publicKey, "RSA Public Key") },
                                            modifier = Modifier.fillMaxWidth().testTag("copy_public_key_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Copy Public Key PEM", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Private Key Expandable (Sealed warning)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPrivateKey = !showPrivateKey }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "Sealed", tint = SecureRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Your Private Key (Sealed in DB)", style = MaterialTheme.typography.bodyMedium, color = TextLight)
                            }
                            Icon(
                                if (showPrivateKey) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle",
                                tint = TextMuted
                            )
                        }

                        AnimatedVisibility(
                            visible = showPrivateKey,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SpaceDark)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(SecureRed.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                "WARNING: Your RSA private key is stored securely in an encrypted local SQL sandbox. It is never transmitted. Exporting or leaking private keys breaks the E2E security model.",
                                                fontSize = 11.sp,
                                                color = SecureRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentUser.privateKey ?: "Unallocated",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = SecureRed,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { copyToClipboard(currentUser.privateKey ?: "", "RSA Private Key") },
                                            modifier = Modifier.fillMaxWidth().testTag("copy_private_key_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecureRed),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Copy Private Key PEM", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Credentials rotation
                        Button(
                            onClick = {
                                onRotateKeys()
                                Toast.makeText(context, "New RSA Credentials Generated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("rotate_credentials_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Rotate", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ROTATE RSA KEYPAIR (2048-BIT)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- Teammate Public Keys Section ---
        item {
            Text(
                "SYNCHRONIZED TEAM KEYRING",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(teammates, key = { it.id }) { teammate ->
            var expandedPubKey by remember { mutableStateOf(false) }
            val teammateColor = try { Color(android.graphics.Color.parseColor(teammate.colorHex)) } catch(e:Exception) { MaterialTheme.colorScheme.secondary }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { expandedPubKey = !expandedPubKey },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).background(teammateColor, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(teammate.initials, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(teammate.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text(teammate.role, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            }
                        }
                        Icon(
                            if (expandedPubKey) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand Key",
                            tint = TextMuted
                        )
                    }

                    AnimatedVisibility(
                        visible = expandedPubKey,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Card(colors = CardDefaults.cardColors(containerColor = SpaceDark)) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = teammate.publicKey,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = CyberBlue,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedButton(
                                        onClick = { copyToClipboard(teammate.publicKey, "${teammate.name} Public Key") },
                                        modifier = Modifier.fillMaxWidth().size(36.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberBlue),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Copy Public Key", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Security Audit Log Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "CRYPTOGRAPHIC AUDIT LOGS",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRefreshLogs,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Logs", tint = CyberGreen, modifier = Modifier.size(18.dp))
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("audit_logs_terminal"),
                colors = CardDefaults.cardColors(containerColor = SpaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = CyberGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "cipherteam_kernel_logs.sh",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = LightSlate)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (auditLogs.isEmpty()) {
                            item {
                                Text(
                                    "No cryptographic events recorded yet.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            items(auditLogs) { log ->
                                Text(
                                    text = log,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (log.contains("ERROR")) SecureRed else if (log.contains("Generating")) SecureYellow else CyberGreen,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Danger Zone Admin Actions ---
        item {
            Text(
                "SYSTEM CONTROLS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
                color = SecureRed
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("system_controls_card"),
                colors = CardDefaults.cardColors(containerColor = SlateGrey)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "DATABASE SANITIZATION",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        "Scrub the local database and remove all encrypted conversation history across all channels. This action is irreversible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            onClearChatHistory()
                            Toast.makeText(context, "All session histories scrubbed completely!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecureRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("purge_history_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Scrub DB")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PURGE ALL ENCRYPTED HISTORY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
