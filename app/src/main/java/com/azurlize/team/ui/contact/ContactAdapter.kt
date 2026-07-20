package com.azurlize.team.ui.contact

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.azurlize.team.databinding.ItemContactBinding
import org.drinkless.tdlib.TdApi
import java.io.File

class ContactAdapter(
    private val onContactClick: (TdApi.User) -> Unit,
    private val onContactLongClick: (TdApi.User) -> Unit,
    private val getFile: (Int) -> TdApi.File?,
    private val isFavorite: (Long) -> Boolean
) : ListAdapter<TdApi.User, ContactAdapter.ViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: TdApi.User) {
            val name = "${user.firstName} ${user.lastName}".trim()
            binding.tvName.text = name
            
            // Avatar
            val monogram = getMonogram(user.firstName, user.lastName)
            binding.tvAvatarMonogram.text = monogram
            binding.ivAvatar.visibility = View.GONE
            binding.tvAvatarMonogram.visibility = View.VISIBLE

            user.profilePhoto?.small?.let { photo ->
                val file = getFile(photo.id) ?: photo
                if (file.local.isDownloadingCompleted) {
                    binding.ivAvatar.load(File(file.local.path))
                    binding.ivAvatar.visibility = View.VISIBLE
                    binding.tvAvatarMonogram.visibility = View.GONE
                }
            }

            // Status
            binding.tvStatus.text = getUserStatus(user.status)
            binding.tvStatus.setTextColor(
                if (user.status is TdApi.UserStatusOnline) 
                    binding.root.context.getColor(android.R.color.holo_blue_dark)
                else 
                    binding.root.context.getColor(android.R.color.darker_gray)
            )

            // Badges
            binding.ivVerified.visibility = if (user.verificationStatus?.isVerified == true) View.VISIBLE else View.GONE
            binding.ivPremium.visibility = if (user.isPremium) View.VISIBLE else View.GONE
            binding.ivFavorite.visibility = if (isFavorite(user.id)) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onContactClick(user) }
            binding.root.setOnLongClickListener {
                onContactLongClick(user)
                true
            }
        }
    }

    private fun getMonogram(firstName: String, lastName: String): String {
        val f = if (firstName.isNotEmpty()) firstName[0].uppercase() else ""
        val l = if (lastName.isNotEmpty()) lastName[0].uppercase() else ""
        return "$f$l"
    }

    private fun getUserStatus(status: TdApi.UserStatus): String {
        return when (status) {
            is TdApi.UserStatusOnline -> "online"
            is TdApi.UserStatusOffline -> {
                if (status.wasOnline == 0) "offline"
                else "last seen ${formatDate(status.wasOnline)}"
            }
            is TdApi.UserStatusRecently -> "last seen recently"
            is TdApi.UserStatusLastWeek -> "last seen last week"
            is TdApi.UserStatusLastMonth -> "last seen last month"
            else -> "offline"
        }
    }

    private fun formatDate(seconds: Int): String {
        val date = java.util.Date(seconds.toLong() * 1000)
        return android.text.format.DateFormat.format("HH:mm", date).toString()
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<TdApi.User>() {
        override fun areItemsTheSame(oldItem: TdApi.User, newItem: TdApi.User) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TdApi.User, newItem: TdApi.User) = 
            oldItem.firstName == newItem.firstName && 
            oldItem.lastName == newItem.lastName &&
            oldItem.status.javaClass == newItem.status.javaClass
    }
}
