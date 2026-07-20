package com.azurlize.team.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.azurlize.team.databinding.ItemChatBinding
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val onChatClick: (TdApi.Chat) -> Unit,
    private val getUser: (Long) -> TdApi.User?,
    private val getSupergroup: (Long) -> TdApi.Supergroup?,
    private val getBasicGroup: (Long) -> TdApi.BasicGroup?,
    private val getFile: (Int) -> TdApi.File?,
    private val downloadFile: (Int) -> Unit
) : ListAdapter<TdApi.Chat, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: TdApi.Chat) {
            binding.tvChatName.text = chat.title
            
            // Last Message or Draft
            val draftMessage = chat.draftMessage
            if (draftMessage != null) {
                val draftText = when (val content = draftMessage.content) {
                    is TdApi.DraftMessageContentText -> content.text.text
                    else -> "Draft"
                }
                binding.tvLastMessage.text = "Draft: $draftText"
                binding.tvLastMessage.setTextColor(0xFFEB5757.toInt()) // Reddish for Draft
                binding.tvTime.text = formatTime(chat.lastMessage?.date ?: 0)
            } else {
                val lastMessage = chat.lastMessage
                if (lastMessage != null) {
                    binding.tvLastMessage.text = getMessageText(lastMessage.content)
                    binding.tvLastMessage.setTextColor(0xFF666666.toInt()) // Use a resource color in real app
                    binding.tvTime.text = formatTime(lastMessage.date)
                } else {
                    binding.tvLastMessage.text = ""
                    binding.tvTime.text = ""
                }
            }

            // Unread Count
            if (chat.unreadCount > 0) {
                binding.tvUnreadCount.visibility = View.VISIBLE
                binding.tvUnreadCount.text = chat.unreadCount.toString()
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }

            // Pinned
            binding.ivPinned.visibility = if (chat.positions.any { it.isPinned }) View.VISIBLE else View.GONE
            
            // Muted
            binding.ivMuted.visibility = if (chat.notificationSettings.muteFor > 0) View.VISIBLE else View.GONE

            // Verified and Online status
            var isVerified = false
            var isOnline = false
            
            when (val type = chat.type) {
                is TdApi.ChatTypePrivate -> {
                    val user = getUser(type.userId)
                    isVerified = user?.verificationStatus?.isVerified ?: false
                    isOnline = user?.status is TdApi.UserStatusOnline
                }
                is TdApi.ChatTypeSupergroup -> {
                    val supergroup = getSupergroup(type.supergroupId)
                    isVerified = supergroup?.verificationStatus?.isVerified ?: false
                    // Detect if it is a forum
                    val isForum = supergroup?.isForum ?: false
                    binding.ivForum.visibility = if (isForum) View.VISIBLE else View.GONE
                }
            }
            
            binding.ivVerified.visibility = if (isVerified) View.VISIBLE else View.GONE
            binding.viewOnlineIndicator.visibility = if (isOnline) View.VISIBLE else View.GONE

            // Avatar
            binding.tvAvatarMonogram.text = if (chat.title.isNotEmpty()) chat.title[0].toString().uppercase() else "?"
            
            val photo = chat.photo?.small
            if (photo != null) {
                val file = getFile(photo.id) ?: photo
                if (file.local.isDownloadingCompleted) {
                    binding.ivAvatar.visibility = View.VISIBLE
                    binding.ivAvatar.load(File(file.local.path))
                } else {
                    binding.ivAvatar.visibility = View.GONE
                    downloadFile(photo.id)
                }
            } else {
                binding.ivAvatar.visibility = View.GONE
            }

            binding.root.setOnClickListener { onChatClick(chat) }
        }

        private fun getMessageText(content: TdApi.MessageContent): String {
            return when (content) {
                is TdApi.MessageText -> content.text.text
                is TdApi.MessagePhoto -> "📷 Photo"
                is TdApi.MessageVideo -> "🎥 Video"
                is TdApi.MessageAudio -> "🎵 Audio"
                is TdApi.MessageDocument -> "📄 Document"
                is TdApi.MessageSticker -> "🏷️ Sticker"
                is TdApi.MessageAnimation -> "🎬 GIF"
                is TdApi.MessageVoiceNote -> "🎤 Voice Note"
                is TdApi.MessageVideoNote -> "📹 Video Note"
                is TdApi.MessageContact -> "👤 Contact"
                is TdApi.MessageLocation -> "📍 Location"
                is TdApi.MessageVenue -> "🏢 Venue"
                is TdApi.MessagePoll -> "📊 Poll"
                is TdApi.MessageCall -> "📞 Call"
                is TdApi.MessageChatChangeTitle -> "Changed chat title"
                is TdApi.MessageChatChangePhoto -> "Changed chat photo"
                else -> "Message"
            }
        }

        private fun formatTime(dateSeconds: Int): String {
            val date = Date(dateSeconds.toLong() * 1000)
            val now = Date()
            val format = if (isSameDay(date, now)) {
                SimpleDateFormat("HH:mm", Locale.getDefault())
            } else {
                SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            }
            return format.format(date)
        }

        private fun isSameDay(date1: Date, date2: Date): Boolean {
            val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return fmt.format(date1) == fmt.format(date2)
        }
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<TdApi.Chat>() {
        override fun areItemsTheSame(oldItem: TdApi.Chat, newItem: TdApi.Chat): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TdApi.Chat, newItem: TdApi.Chat): Boolean {
            // This is a bit complex for TdApi.Chat, but we'll compare key fields
            return oldItem.title == newItem.title &&
                    oldItem.unreadCount == newItem.unreadCount &&
                    oldItem.lastMessage?.id == newItem.lastMessage?.id &&
                    oldItem.positions.contentEquals(newItem.positions)
        }
    }
}
