package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    val publicKey: String,          // PEM format
    val privateKey: String?,         // PEM format (null for teammates)
    val isCurrentUser: Boolean,
    val initials: String,
    val colorHex: String
)

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val isGroup: Boolean,
    val lastMessage: String? = null,
    val lastMessageTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val ciphertext: String,         // The encrypted message content (Base64)
    val encryptedAesKey: String,     // The AES key encrypted with recipient's RSA public key (Base64)
    val timestamp: Long = System.currentTimeMillis(),
    val isSystemMessage: Boolean = false
)
