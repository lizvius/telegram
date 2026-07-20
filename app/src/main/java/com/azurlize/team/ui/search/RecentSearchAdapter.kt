package com.azurlize.team.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azurlize.team.data.local.RecentSearch
import com.azurlize.team.databinding.ItemSearchRecentBinding

class RecentSearchAdapter(
    private val onClick: (RecentSearch) -> Unit,
    private val onDelete: (RecentSearch) -> Unit
) : ListAdapter<RecentSearch, RecentSearchAdapter.ViewHolder>(RecentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSearchRecentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemSearchRecentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(search: RecentSearch) {
            binding.tvQuery.text = search.query
            binding.root.setOnClickListener { onClick(search) }
            binding.ivDelete.setOnClickListener { onDelete(search) }
        }
    }

    class RecentDiffCallback : DiffUtil.ItemCallback<RecentSearch>() {
        override fun areItemsTheSame(oldItem: RecentSearch, newItem: RecentSearch) = oldItem.query == newItem.query
        override fun areContentsTheSame(oldItem: RecentSearch, newItem: RecentSearch) = oldItem == newItem
    }
}
