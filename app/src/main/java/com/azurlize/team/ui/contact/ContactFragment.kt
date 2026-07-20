package com.azurlize.team.ui.contact

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentContactBinding
import com.azurlize.team.databinding.SheetProfilePreviewBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class ContactFragment : Fragment() {

    private var _binding: FragmentContactBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ContactViewModel by viewModels()
    private lateinit var contactAdapter: ContactAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        contactAdapter = ContactAdapter(
            onContactClick = { user ->
                viewModel.createChat(user.id) { chat ->
                    chat?.let {
                        val bundle = Bundle().apply { putLong("chatId", it.id) }
                        findNavController().navigate(R.id.chatFragment, bundle)
                    }
                }
            },
            onContactLongClick = { user ->
                showProfilePreview(user)
            },
            getFile = { viewModel.getFile(it) },
            isFavorite = { viewModel.favoriteIds.value.contains(it) }
        )
        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = contactAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onSearch(newText ?: "")
                return true
            }
        })
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.contacts,
                    viewModel.searchResults,
                    viewModel.searchQuery,
                    viewModel.favoriteIds,
                    viewModel.pinnedIds
                ) { contacts, searchResults, query, favs, pins ->
                    if (query.isEmpty()) {
                        // Sort: Pinned first, then Favorite, then regular
                        contacts.sortedWith(compareByDescending<TdApi.User> { pins.contains(it.id) }
                            .thenByDescending { favs.contains(it.id) }
                            .thenBy { it.firstName })
                    } else {
                        searchResults
                    }
                }.collect {
                    contactAdapter.submitList(it)
                }
            }
        }
    }

    private fun showProfilePreview(user: TdApi.User) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = SheetProfilePreviewBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val name = "${user.firstName} ${user.lastName}".trim()
        sheetBinding.tvName.text = name
        val username = user.usernames?.activeUsernames?.firstOrNull()
        sheetBinding.tvUsername.text = if (username != null) "@$username" else ""
        sheetBinding.tvStatus.text = getUserStatus(user.status)

        // Monogram
        val f = if (user.firstName.isNotEmpty()) user.firstName[0].uppercase() else ""
        val l = if (user.lastName.isNotEmpty()) user.lastName[0].uppercase() else ""
        sheetBinding.tvAvatarMonogram.text = "$f$l"

        user.profilePhoto?.big?.let { photo ->
            val file = viewModel.getFile(photo.id) ?: photo
            if (file.local.isDownloadingCompleted) {
                sheetBinding.ivAvatar.load(File(file.local.path))
                sheetBinding.tvAvatarMonogram.visibility = View.GONE
            }
        }

        viewModel.getUserFullInfo(user.id) { info ->
            lifecycleScope.launch {
                info?.let {
                    sheetBinding.tvBio.text = it.bio?.text
                    sheetBinding.tvPhone.text = user.phoneNumber
                }
            }
        }

        sheetBinding.btnChat.setOnClickListener {
            viewModel.createChat(user.id) { chat ->
                chat?.let {
                    val bundle = Bundle().apply { putLong("chatId", it.id) }
                    findNavController().navigate(R.id.chatFragment, bundle)
                    dialog.dismiss()
                }
            }
        }

        sheetBinding.btnCopyUsername.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val usernameToCopy = user.usernames?.activeUsernames?.firstOrNull() ?: user.id.toString()
            val clip = ClipData.newPlainText("username", usernameToCopy)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Username/ID copied", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun getUserStatus(status: TdApi.UserStatus): String {
        return when (status) {
            is TdApi.UserStatusOnline -> "online"
            is TdApi.UserStatusRecently -> "last seen recently"
            is TdApi.UserStatusOffline -> "offline"
            else -> "offline"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
