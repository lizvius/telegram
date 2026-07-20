package com.azurlize.team.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.azurlize.team.R
import com.azurlize.team.databinding.ItemMessageReceivedBinding
import com.azurlize.team.databinding.ItemMessageSentBinding
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(
    private val myUserId: Long,
    private val getUser: (Long) -> TdApi.User?,
    private val getFile: (Int) -> TdApi.File?,
    private val downloadFile: (Int) -> Unit,
    private val onMediaClick: (TdApi.File) -> Unit
) : ListAdapter<TdApi.Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId is TdApi.MessageSenderUser && (message.senderId as TdApi.MessageSenderUser).userId == myUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: TdApi.Message) {
            bindMessageContent(message, binding.tvMessageText, binding.cardMedia, binding.ivMedia, binding.pbMedia, binding.layoutFile, binding.tvFileName, binding.tvFileInfo)
            binding.tvTime.text = formatTime(message.date)
            
            // Status
            val statusIcon = when {
                message.sendingState is TdApi.MessageSendingStatePending -> R.drawable.ic_schedule
                message.sendingState is TdApi.MessageSendingStateFailed -> R.drawable.ic_error
                else -> R.drawable.ic_done_all 
            }
            binding.ivStatus.setImageResource(statusIcon)
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: TdApi.Message) {
            bindMessageContent(message, binding.tvMessageText, binding.cardMedia, binding.ivMedia, binding.pbMedia, binding.layoutFile, binding.tvFileName, binding.tvFileInfo)
            binding.tvTime.text = formatTime(message.date)

            val senderId = message.senderId
            if (senderId is TdApi.MessageSenderUser) {
                val user = getUser(senderId.userId)
                if (user != null) {
                    binding.tvSenderName.text = "${user.firstName} ${user.lastName}".trim()
                    binding.tvAvatarMonogram.text = if (user.firstName.isNotEmpty()) user.firstName[0].toString() else "?"
                    
                    val photo = user.profilePhoto?.small
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
                }
            }
        }
    }

    private var highlightQuery: String = ""

    fun setHighlightQuery(query: String) {
        highlightQuery = query
        notifyDataSetChanged()
    }

    private fun bindMessageContent(
        message: TdApi.Message,
        tvText: android.widget.TextView,
        cardMedia: View,
        ivMedia: android.widget.ImageView,
        pbMedia: android.widget.ProgressBar,
        layoutFile: View,
        tvFileName: android.widget.TextView,
        tvFileInfo: android.widget.TextView
    ) {
        tvText.visibility = View.GONE
        cardMedia.visibility = View.GONE
        layoutFile.visibility = View.GONE
        pbMedia.visibility = View.GONE

        when (val content = message.content) {
            is TdApi.MessageText -> {
                tvText.visibility = View.VISIBLE
                tvText.text = highlightText(content.text.text, highlightQuery)
            }
            is TdApi.MessagePhoto -> {
                cardMedia.visibility = View.VISIBLE
                val photo = content.photo.sizes.lastOrNull()
                photo?.let {
                    val file = getFile(it.photo.id) ?: it.photo
                    if (file.local.isDownloadingCompleted) {
                        ivMedia.load(File(file.local.path))
                        pbMedia.visibility = View.GONE
                        cardMedia.setOnClickListener { onMediaClick(file) }
                    } else {
                        ivMedia.setImageResource(R.drawable.ic_image) // Placeholder
                        if (file.local.isDownloadingActive) {
                            pbMedia.visibility = View.VISIBLE
                            val progress = if (file.size > 0) (file.local.downloadedSize.toDouble() / file.size * 100).toInt() else 0
                            pbMedia.progress = progress
                        } else {
                            pbMedia.visibility = View.GONE
                            downloadFile(file.id)
                        }
                        cardMedia.setOnClickListener(null)
                    }
                }
                if (content.caption.text.isNotEmpty()) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = highlightText(content.caption.text, highlightQuery)
                }
            }
            is TdApi.MessageVideo -> {
                cardMedia.visibility = View.VISIBLE
                val thumbnail = content.video.thumbnail?.file
                thumbnail?.let {
                    val file = getFile(it.id) ?: it
                    if (file.local.isDownloadingCompleted) {
                        ivMedia.load(File(file.local.path))
                    } else {
                        ivMedia.setImageResource(R.drawable.ic_video) // Placeholder
                        downloadFile(file.id)
                    }
                }
                
                val videoFile = content.video.video
                if (videoFile.local.isDownloadingActive) {
                    pbMedia.visibility = View.VISIBLE
                    val progress = if (videoFile.size > 0) (videoFile.local.downloadedSize.toDouble() / videoFile.size * 100).toInt() else 0
                    pbMedia.progress = progress
                } else if (videoFile.local.isDownloadingCompleted) {
                    pbMedia.visibility = View.GONE
                    cardMedia.setOnClickListener { onMediaClick(videoFile) }
                } else {
                    pbMedia.visibility = View.GONE
                    cardMedia.setOnClickListener { downloadFile(videoFile.id) }
                }

                if (content.caption.text.isNotEmpty()) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = highlightText(content.caption.text, highlightQuery)
                }
            }
            is TdApi.MessageDocument -> {
                layoutFile.visibility = View.VISIBLE
                val doc = content.document
                tvFileName.text = highlightText(doc.fileName, highlightQuery)
                tvFileInfo.text = formatFileSize(doc.document.size)
                
                val file = getFile(doc.document.id) ?: doc.document
                if (file.local.isDownloadingCompleted) {
                    layoutFile.setOnClickListener { onMediaClick(file) }
                } else {
                    layoutFile.setOnClickListener { downloadFile(file.id) }
                }

                if (content.caption.text.isNotEmpty()) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = highlightText(content.caption.text, highlightQuery)
                }
            }
            is TdApi.MessageAudio -> {
                layoutFile.visibility = View.VISIBLE
                val audio = content.audio
                tvFileName.text = highlightText(if (audio.title.isNotEmpty()) audio.title else "Audio", highlightQuery)
                tvFileInfo.text = formatFileSize(audio.audio.size)
                
                val file = getFile(audio.audio.id) ?: audio.audio
                if (file.local.isDownloadingCompleted) {
                    layoutFile.setOnClickListener { onMediaClick(file) }
                } else {
                    layoutFile.setOnClickListener { downloadFile(file.id) }
                }

                if (content.caption.text.isNotEmpty()) {
                    tvText.visibility = View.VISIBLE
                    tvText.text = highlightText(content.caption.text, highlightQuery)
                }
            }
            else -> {
                tvText.visibility = View.VISIBLE
                tvText.text = getMessageText(content)
            }
        }
    }

    private fun highlightText(text: String, query: String): CharSequence {
        if (query.isEmpty() || !text.contains(query, ignoreCase = true)) return text
        val spannable = android.text.SpannableString(text)
        val lowerText = text.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())
        var start = lowerText.indexOf(lowerQuery)
        while (start >= 0) {
            spannable.setSpan(
                android.text.style.BackgroundColorSpan(android.graphics.Color.YELLOW),
                start,
                start + query.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            start = lowerText.indexOf(lowerQuery, start + query.length)
        }
        return spannable
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024
        if (kb < 1024) return "$kb KB"
        val mb = kb / 1024
        return "$mb MB"
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
            else -> "Message"
        }
    }

    private fun formatTime(dateSeconds: Int): String {
        val date = Date(dateSeconds.toLong() * 1000)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(date)
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<TdApi.Message>() {
        override fun areItemsTheSame(oldItem: TdApi.Message, newItem: TdApi.Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TdApi.Message, newItem: TdApi.Message): Boolean {
            return oldItem.content.javaClass == newItem.content.javaClass &&
                    oldItem.sendingState?.javaClass == newItem.sendingState?.javaClass &&
                    (oldItem.content as? TdApi.MessageText)?.text?.text == (newItem.content as? TdApi.MessageText)?.text?.text
        }
    }
}
