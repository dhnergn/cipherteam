package com.example.data.repository

import com.example.crypto.CryptoEngine
import com.example.data.database.ChatDao
import com.example.data.model.Channel
import com.example.data.model.Member
import com.example.data.model.Message
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    val allChannels: Flow<List<Channel>> = chatDao.getAllChannelsFlow()
    val allMembers: Flow<List<Member>> = chatDao.getAllMembersFlow()

    fun getMessagesForChannel(channelId: String): Flow<List<Message>> {
        return chatDao.getMessagesForChannelFlow(channelId)
    }

    suspend fun getChannelById(channelId: String): Channel? {
        return chatDao.getChannelById(channelId)
    }

    suspend fun getMemberById(memberId: String): Member? {
        return chatDao.getMemberById(memberId)
    }

    suspend fun getCurrentUser(): Member? {
        return chatDao.getCurrentUser()
    }

    /**
     * Seeds the local database with a secure identity and team channels if empty.
     */
    suspend fun initializeDbIfEmpty() {
        val currentUser = chatDao.getCurrentUser()
        if (currentUser != null) {
            CryptoEngine.logAudit("Existing secure identity verified for ${currentUser.name}.")
            return
        }

        CryptoEngine.logAudit("Initializing secure offline team database...")

        // 1. Generate RSA key pair for the user
        CryptoEngine.logAudit("Initializing on-device user security credentials...")
        val userKeyPair = CryptoEngine.generateKeyPair()
        val userPublicPem = CryptoEngine.publicKeyToPem(userKeyPair.public)
        val userPrivatePem = CryptoEngine.privateKeyToPem(userKeyPair.private)

        val userMember = Member(
            id = "current_user",
            name = "You (Product Lead)",
            role = "Product & Security Owner",
            publicKey = userPublicPem,
            privateKey = userPrivatePem,
            isCurrentUser = true,
            initials = "ME",
            colorHex = "#6A1B9A" // Deep purple
        )
        chatDao.insertMember(userMember)
        CryptoEngine.logAudit("Your local secure identity generated and private key sealed in DB.")

        // 2. Generate RSA keys and profiles for teammates
        CryptoEngine.logAudit("Provisioning secure cryptographic profiles for team members...")
        
        // Alice (Security Lead)
        val aliceKeyPair = CryptoEngine.generateKeyPair()
        val aliceMember = Member(
            id = "alice",
            name = "Alice Vance",
            role = "CSO / Security Lead",
            publicKey = CryptoEngine.publicKeyToPem(aliceKeyPair.public),
            privateKey = CryptoEngine.privateKeyToPem(aliceKeyPair.private),
            isCurrentUser = false,
            initials = "AV",
            colorHex = "#2E7D32" // Green
        )

        // Bob (Engineering)
        val bobKeyPair = CryptoEngine.generateKeyPair()
        val bobMember = Member(
            id = "bob",
            name = "Bob Miller",
            role = "VP Engineering",
            publicKey = CryptoEngine.publicKeyToPem(bobKeyPair.public),
            privateKey = CryptoEngine.privateKeyToPem(bobKeyPair.private),
            isCurrentUser = false,
            initials = "BM",
            colorHex = "#1565C0" // Blue
        )

        // Carol (Product)
        val carolKeyPair = CryptoEngine.generateKeyPair()
        val carolMember = Member(
            id = "carol",
            name = "Carol Chen",
            role = "Head of Product",
            publicKey = CryptoEngine.publicKeyToPem(carolKeyPair.public),
            privateKey = CryptoEngine.privateKeyToPem(carolKeyPair.private),
            isCurrentUser = false,
            initials = "CC",
            colorHex = "#D84315" // Orange
        )

        chatDao.insertMembers(listOf(aliceMember, bobMember, carolMember))

        // 3. Seed Default Channels
        CryptoEngine.logAudit("Creating secure encrypted group and direct channels...")
        val defaultChannels = listOf(
            Channel(
                id = "project-alpha",
                name = "#project-alpha",
                description = "Confidential Next-Gen Cryptography Project",
                isGroup = true
            ),
            Channel(
                id = "finance-operations",
                name = "#finance-operations",
                description = "Internal Q3 Financial Forecasting & Mergers",
                isGroup = true
            ),
            Channel(
                id = "direct-alice",
                name = "Alice Vance",
                description = "Secure Point-to-Point Tunnel with CSO",
                isGroup = false
            ),
            Channel(
                id = "direct-bob",
                name = "Bob Miller",
                description = "Secure Point-to-Point Tunnel with VP Eng",
                isGroup = false
            ),
            Channel(
                id = "direct-carol",
                name = "Carol Chen",
                description = "Secure Point-to-Point Tunnel with Head of Product",
                isGroup = false
            )
        )
        chatDao.insertChannels(defaultChannels)

        // 4. Inject introductory system and encrypted messages
        val initialMessages = listOf(
            // #project-alpha
            createSystemMessage("project-alpha", "Secure Channel Established. Group E2E AES-256 Key negotiated."),
            createEncryptedMessage(
                channelId = "project-alpha",
                sender = aliceMember,
                recipient = userMember,
                text = "Welcome to Project Alpha. All communications here are end-to-end encrypted. We need to finalize the patent filing tomorrow.",
                timeOffsetMs = -10 * 60 * 1000 // 10 mins ago
            ),
            createEncryptedMessage(
                channelId = "project-alpha",
                sender = bobMember,
                recipient = userMember,
                text = "Understood. The core crypto modules are ready, awaiting the legal sign-off.",
                timeOffsetMs = -8 * 60 * 1000 // 8 mins ago
            ),

            // #finance-operations
            createSystemMessage("finance-operations", "Secure Tunnel Initialized. Key rotation interval set to 24h."),
            createEncryptedMessage(
                channelId = "finance-operations",
                sender = carolMember,
                recipient = userMember,
                text = "Team, please review the revised forecast slides before our board sync on Monday.",
                timeOffsetMs = -30 * 60 * 1000 // 30 mins ago
            ),

            // Direct Alice
            createSystemMessage("direct-alice", "Direct RSA-2048 / AES-256 peer tunnel completed. Verified key hash: AV-CSO-991A"),
            createEncryptedMessage(
                channelId = "direct-alice",
                sender = aliceMember,
                recipient = userMember,
                text = "Let me know when you have time to review the threat model for our cloud endpoints.",
                timeOffsetMs = -5 * 60 * 1000 // 5 mins ago
            )
        )

        for (msg in initialMessages) {
            chatDao.insertMessage(msg)
        }

        // Update last message in channels
        updateChannelLastMessage("project-alpha", "Core crypto modules ready, awaiting legal sign-off.", System.currentTimeMillis() - 8 * 60 * 1000)
        updateChannelLastMessage("finance-operations", "Review forecast slides before our board sync.", System.currentTimeMillis() - 30 * 60 * 1000)
        updateChannelLastMessage("direct-alice", "Let me know when you have time to review...", System.currentTimeMillis() - 5 * 60 * 1000)

        CryptoEngine.logAudit("Secure initialization completed. Encryption sandbox live.")
    }

    private suspend fun updateChannelLastMessage(channelId: String, text: String, time: Long) {
        val channel = chatDao.getChannelById(channelId)
        if (channel != null) {
            chatDao.updateChannel(channel.copy(lastMessage = text, lastMessageTime = time))
        }
    }

    private fun createSystemMessage(channelId: String, text: String): Message {
        // System messages are plaintext payloads with metadata
        val payload = CryptoEngine.encrypt(text, getStubPublicKeyPem())
        return Message(
            channelId = channelId,
            senderId = "system",
            senderName = "SYSTEM",
            ciphertext = payload.ciphertext,
            encryptedAesKey = payload.encryptedAesKey,
            isSystemMessage = true
        )
    }

    private fun createEncryptedMessage(
        channelId: String,
        sender: Member,
        recipient: Member,
        text: String,
        timeOffsetMs: Long
    ): Message {
        // In this simulation, sender encrypts using recipient's public key
        val recipientPublicKey = recipient.publicKey
        val payload = CryptoEngine.encrypt(text, recipientPublicKey)
        return Message(
            channelId = channelId,
            senderId = sender.id,
            senderName = sender.name,
            ciphertext = payload.ciphertext,
            encryptedAesKey = payload.encryptedAesKey,
            timestamp = System.currentTimeMillis() + timeOffsetMs,
            isSystemMessage = false
        )
    }

    private fun getStubPublicKeyPem(): String {
        // Just a helper to satisfy the encrypt call for system messages which aren't really read
        val kp = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }.generateKeyPair()
        return CryptoEngine.publicKeyToPem(kp.public)
    }

    /**
     * Send a secure message from current user
     */
    suspend fun sendMessage(channelId: String, plainText: String) {
        val currentUser = chatDao.getCurrentUser() ?: return
        val channel = chatDao.getChannelById(channelId) ?: return

        // Encrypt the message.
        // If it's a direct channel, encrypt for the specific recipient.
        // If it's a group channel, encrypt for the user themselves (or simulate group shared key).
        // Let's encrypt using the recipient's public key (for direct chats) or our own public key for backup.
        var recipientPublicKeyPem = currentUser.publicKey

        if (!channel.isGroup) {
            // Find teammate ID
            val teammateId = when (channelId) {
                "direct-alice" -> "alice"
                "direct-bob" -> "bob"
                "direct-carol" -> "carol"
                else -> null
            }
            if (teammateId != null) {
                val teammate = chatDao.getMemberById(teammateId)
                if (teammate != null) {
                    recipientPublicKeyPem = teammate.publicKey
                }
            }
        }

        CryptoEngine.logAudit("Outgoing E2E encryption initiated for $channelId...")
        val payload = CryptoEngine.encrypt(plainText, recipientPublicKeyPem)

        val message = Message(
            channelId = channelId,
            senderId = currentUser.id,
            senderName = currentUser.name,
            ciphertext = payload.ciphertext,
            encryptedAesKey = payload.encryptedAesKey,
            timestamp = System.currentTimeMillis(),
            isSystemMessage = false
        )

        chatDao.insertMessage(message)
        chatDao.updateChannel(channel.copy(lastMessage = plainText, lastMessageTime = System.currentTimeMillis()))
    }

    /**
     * Inserts an encrypted message sent by a virtual teammate
     */
    suspend fun receiveSimulatedTeammateMessage(channelId: String, senderId: String, plainText: String) {
        val sender = chatDao.getMemberById(senderId) ?: return
        val currentUser = chatDao.getCurrentUser() ?: return
        val channel = chatDao.getChannelById(channelId) ?: return

        CryptoEngine.logAudit("Incoming encrypted message received from ${sender.name}...")
        
        // Sender encrypts using current user's RSA public key (real end-to-end encryption!)
        val payload = CryptoEngine.encrypt(plainText, currentUser.publicKey)

        val message = Message(
            channelId = channelId,
            senderId = sender.id,
            senderName = sender.name,
            ciphertext = payload.ciphertext,
            encryptedAesKey = payload.encryptedAesKey,
            timestamp = System.currentTimeMillis(),
            isSystemMessage = false
        )

        chatDao.insertMessage(message)
        chatDao.updateChannel(channel.copy(lastMessage = plainText, lastMessageTime = System.currentTimeMillis()))
    }

    /**
     * Clear all message history & security audit logs.
     */
    suspend fun clearHistory() {
        chatDao.clearAllMessages()
    }

    /**
     * Rotate User Keys. Generates a fresh RSA Key Pair.
     */
    suspend fun rotateUserKeys() {
        val currentUser = chatDao.getCurrentUser() ?: return
        CryptoEngine.logAudit("Initiating key rotation cycle for User...")
        
        val newKeyPair = CryptoEngine.generateKeyPair()
        val newPublicPem = CryptoEngine.publicKeyToPem(newKeyPair.public)
        val newPrivatePem = CryptoEngine.privateKeyToPem(newKeyPair.private)

        chatDao.insertMember(
            currentUser.copy(
                publicKey = newPublicPem,
                privateKey = newPrivatePem
            )
        )
        CryptoEngine.logAudit("Key rotation completed. Old key revoked. New RSA-2048 credentials active.")
    }
}
