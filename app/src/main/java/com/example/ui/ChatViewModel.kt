package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoEngine
import com.example.data.database.ChatDatabase
import com.example.data.model.Channel
import com.example.data.model.Member
import com.example.data.model.Message
import com.example.data.repository.ChatRepository
import com.example.data.repository.GeminiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    
    val channels: StateFlow<List<Channel>>
    val members: StateFlow<List<Member>>
    
    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    private val _isTyping = MutableStateFlow<String?>(null) // Name of member typing
    val isTyping: StateFlow<String?> = _isTyping.asStateFlow()

    private val _globalCipherMode = MutableStateFlow(false)
    val globalCipherMode: StateFlow<Boolean> = _globalCipherMode.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<String>>(emptyList())
    val auditLogs: StateFlow<List<String>> = _auditLogs.asStateFlow()

    init {
        val database = ChatDatabase.getDatabase(application)
        repository = ChatRepository(database.chatDao())
        
        channels = repository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        members = repository.allMembers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed data and set up user keys
        viewModelScope.launch {
            repository.initializeDbIfEmpty()
            refreshAuditLogs()
            // Set initial selected channel if channels is populated
            channels.collect { list ->
                if (_selectedChannelId.value == null && list.isNotEmpty()) {
                    _selectedChannelId.value = list.first().id
                }
            }
        }
    }

    // Active message stream based on selected channel
    val activeMessages: StateFlow<List<Message>> = _selectedChannelId
        .flatMapLatest { channelId ->
            if (channelId == null) flowOf(emptyList())
            else repository.getMessagesForChannel(channelId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectChannel(channelId: String) {
        _selectedChannelId.value = channelId
        CryptoEngine.logAudit("Switched secure context to channel: $channelId")
        refreshAuditLogs()
    }

    fun toggleGlobalCipherMode() {
        _globalCipherMode.value = !_globalCipherMode.value
        CryptoEngine.logAudit("Global Cryptographic Overlay mode toggled: ${_globalCipherMode.value}")
        refreshAuditLogs()
    }

    fun refreshAuditLogs() {
        _auditLogs.value = CryptoEngine.auditLogs.reversed()
    }

    fun sendMessage(text: String) {
        val currentChannelId = _selectedChannelId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            repository.sendMessage(currentChannelId, text)
            refreshAuditLogs()

            // Trigger secure teammate response simulation
            simulateTeammateReply(currentChannelId, text)
        }
    }

    private fun simulateTeammateReply(channelId: String, userMsg: String) {
        viewModelScope.launch {
            val channel = repository.getChannelById(channelId) ?: return@launch
            
            // Determine who should reply
            val replier = selectReplier(channel) ?: return@launch
            
            // Show typing indicator
            delay(1000)
            _isTyping.value = replier.name
            CryptoEngine.logAudit("Teammate ${replier.name} is typing... negotiating ephemeral key handshake.")
            refreshAuditLogs()
            
            // Wait a bit more for realistic typing duration
            delay(1500)
            
            // Generate response via Gemini (or local fallback)
            val contextInfo = if (channel.isGroup) {
                "Group Channel: ${channel.name} - ${channel.description}"
            } else {
                "Direct encrypted peer-to-peer tunnel."
            }

            val replyText = GeminiService.generateTeammateResponse(
                teammateName = replier.name,
                teammateRole = replier.role,
                contextInfo = contextInfo,
                userMessage = userMsg
            )

            // Inject reply into DB (this will encrypt it with our public key)
            repository.receiveSimulatedTeammateMessage(channelId, replier.id, replyText)
            
            _isTyping.value = null
            CryptoEngine.logAudit("Received E2E response from ${replier.name}. Decryption succeeded locally.")
            refreshAuditLogs()
        }
    }

    private suspend fun selectReplier(channel: Channel): Member? {
        val allTeammates = members.value.filter { !it.isCurrentUser }
        if (allTeammates.isEmpty()) return null

        return if (!channel.isGroup) {
            // Direct chat: replier is the teammate matching this channel
            val teammateId = when (channel.id) {
                "direct-alice" -> "alice"
                "direct-bob" -> "bob"
                "direct-carol" -> "carol"
                else -> null
            }
            allTeammates.find { it.id == teammateId }
        } else {
            // Group chat: randomly pick one of the active channel participants
            allTeammates.random()
        }
    }

    fun rotateCredentials() {
        viewModelScope.launch {
            repository.rotateUserKeys()
            refreshAuditLogs()
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            CryptoEngine.logAudit("Cryptographic database scrubbed. All session messages deleted.")
            refreshAuditLogs()
        }
    }
}
