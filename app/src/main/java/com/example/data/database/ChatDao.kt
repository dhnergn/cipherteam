package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Channel
import com.example.data.model.Member
import com.example.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // --- Members Queries ---
    @Query("SELECT * FROM members")
    fun getAllMembersFlow(): Flow<List<Member>>

    @Query("SELECT * FROM members")
    suspend fun getAllMembers(): List<Member>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: String): Member?

    @Query("SELECT * FROM members WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    // --- Channels Queries ---
    @Query("SELECT * FROM channels ORDER BY lastMessageTime DESC")
    fun getAllChannelsFlow(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    // --- Messages Queries ---
    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannelFlow(channelId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    suspend fun getMessagesForChannel(channelId: String): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
