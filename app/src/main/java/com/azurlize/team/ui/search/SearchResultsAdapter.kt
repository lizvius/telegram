package com.azurlize.team.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.azurlize.team.databinding.ItemChatBinding
import com.azurlize.team.databinding.ItemContactBinding
import org.drinkless.tdlib.TdApi
import java.io.File

class SearchResultsAdapter(
    private val onChatClick: (TdApi.Chat) -> Unit,
    private val onUserClick: (TdApi.User) -> Unit,
    private val getFile: (Int) -> TdApi.File?
) : ListAdapter<SearchResult, RecyclerView.ViewHolder>(SearchDiffCallback()) {

    private val VIEW_TYPE_CHAT = 1
    private val VIEW_TYPE_USER = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SearchResult.ChatResult -> VIEW_TYPE_CHAT
            is SearchResult.UserResult -> VIEW_TYPE_USER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CHAT -> {
                val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ChatViewHolder(binding)
            }
            else -> {
                val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                UserViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is ChatViewHolder && item is SearchResult.ChatResult) {
            holder.bind(item.chat)
        } else if (holder is UserViewHolder && item is SearchResult.UserResult) {
            holder.bind(item.user)
        }
    }

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: TdApi.Chat) {
            binding.tvChatName.text = chat.title
            binding.tvLastMessage.text = chat.lastMessage?.content?.let { "Message" } ?: ""
            binding.root.setOnClickListener { onChatClick(chat) }
            
            // Avatar logic
            binding.tvAvatarMonogram.text = if (chat.title.isNotEmpty()) chat.title[0].uppercase() else ""
            chat.photo?.small?.let { photo ->
                val file = getFile(photo.id) ?: photo
                if (file.local.isDownloadingCompleted) {
                    binding.ivAvatar.load(File(file.local.path))
                    binding.ivAvatar.visibility = View.VISIBLE
                    binding.tvAvatarMonogram.visibility = View.GONE
                }
            }
        }
    }

    inner class UserViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: TdApi.User) {
            binding.tvName.text = "${user.firstName} ${user.lastName}".trim()
            val username = user.usernames?.activeUsernames?.firstOrNull()
            binding.tvStatus.text = if (username != null) "@$username" else "User"
            binding.root.setOnClickListener { onUserClick(user) }

            // Avatar logic
            binding.tvAvatarMonogram.text = if (user.firstName.isNotEmpty()) user.firstName[0].uppercase() else ""
            user.profilePhoto?.small?.let { photo ->
                val file = getFile(photo.id) ?: photo
                if (file.local.isDownloadingCompleted) {
                    binding.ivAvatar.load(File(file.local.path))
                    binding.ivAvatar.visibility = View.VISIBLE
                    binding.tvAvatarMonogram.visibility = View.GONE
                }
            }
        }
    }

    class SearchDiffCallback : DiffUtil.ItemCallback<SearchResult>() {
        override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult) = oldItem == newItem
    }
}
