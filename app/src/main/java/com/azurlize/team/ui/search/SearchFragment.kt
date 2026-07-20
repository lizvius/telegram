package com.azurlize.team.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentSearchBinding
import com.azurlize.team.ui.contact.ContactAdapter
import com.azurlize.team.ui.home.ChatAdapter
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var recentAdapter: RecentSearchAdapter
    private lateinit var resultsAdapter: SearchResultsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerViews()
        setupSearchView()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerViews() {
        recentAdapter = RecentSearchAdapter(
            onClick = { 
                binding.searchView.setQuery(it.query, true)
            },
            onDelete = { viewModel.deleteRecentSearch(it.query) }
        )
        binding.rvRecent.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = recentAdapter
        }

        resultsAdapter = SearchResultsAdapter(
            onChatClick = { chat ->
                val bundle = Bundle().apply { putLong("chatId", chat.id) }
                findNavController().navigate(R.id.chatFragment, bundle)
                viewModel.addRecentSearch(binding.searchView.query.toString())
            },
            onUserClick = { user ->
                // Create private chat and navigate
                // For simplicity, just showing how to trigger chat
                viewModel.addRecentSearch(binding.searchView.query.toString())
            },
            getFile = { viewModel.getFile(it) }
        )
        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = resultsAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    viewModel.onSearch(it)
                    viewModel.addRecentSearch(it)
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearch(newText ?: "")
                return true
            }
        })
        binding.searchView.requestFocus()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.query.collect { query ->
                    if (query.isEmpty()) {
                        binding.layoutRecent.visibility = View.VISIBLE
                        binding.rvResults.visibility = View.GONE
                    } else {
                        binding.layoutRecent.visibility = View.GONE
                        binding.rvResults.visibility = View.VISIBLE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.recentSearches.collect {
                    recentAdapter.submitList(it)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect {
                    resultsAdapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
