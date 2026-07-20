package com.azurlize.team.ui.home

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
import com.azurlize.team.databinding.FragmentHomeBinding
import com.azurlize.team.telegram.session.TelegramSessionManager
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var sessionManager: TelegramSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = TelegramSessionManager(requireContext())
        setupRecyclerView()
        setupBottomNavigation()
        setupTabLayout()
        setupSearchView()
        observeViewModel()

        binding.fabNewChat.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_contactFragment)
        }

        binding.cvProfile.setOnClickListener {
            findNavController().navigate(R.id.profileFragment)
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                val category = when (tab?.position) {
                    1 -> HomeViewModel.ChatCategory.PERSONAL
                    2 -> HomeViewModel.ChatCategory.GROUPS
                    3 -> HomeViewModel.ChatCategory.CHANNELS
                    else -> HomeViewModel.ChatCategory.ALL
                }
                viewModel.setCategory(category)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onChatClick = { chat ->
                val bundle = Bundle().apply {
                    putLong("chatId", chat.id)
                }
                findNavController().navigate(R.id.action_homeFragment_to_chatFragment, bundle)
            },
            getUser = { viewModel.getUser(it) },
            getSupergroup = { viewModel.getSupergroup(it) },
            getBasicGroup = { viewModel.getBasicGroup(it) },
            getFile = { viewModel.getFile(it) },
            downloadFile = { viewModel.downloadFile(it) }
        )
        binding.rvChats.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chats -> true
                R.id.nav_profile -> {
                    findNavController().navigate(R.id.profileFragment)
                    false
                }
                else -> {
                    Toast.makeText(context, "${item.title} - Coming Soon", Toast.LENGTH_SHORT).show()
                    false
                }
            }
        }
    }

    private fun setupSearchView() {
        binding.cvSearch.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
            }
        }
        binding.searchView.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredChats.collect { chats ->
                        chatAdapter.submitList(chats)
                    }
                }
                launch {
                    viewModel.me.collect { user ->
                        user?.let { updateToolbar(it) }
                    }
                }
                launch {
                    viewModel.connectionState.collect { state ->
                        updateConnectionStatus(state)
                    }
                }
                launch {
                    viewModel.fileUpdates.collect { file ->
                        file?.let {
                            if (it.local.isDownloadingCompleted) {
                                // Potentially inefficient, but for now it works
                                chatAdapter.notifyDataSetChanged()
                                viewModel.me.value?.let { me -> updateToolbar(me) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateToolbar(user: TdApi.User) {
        val fullName = "${user.firstName} ${user.lastName}".trim()
        binding.tvToolbarName.text = if (fullName.isNotEmpty()) fullName else "Telegram"
        
        // Avatar monogram
        val monogramFirst = if (user.firstName.isNotEmpty()) user.firstName[0].toString() else ""
        val monogramLast = if (user.lastName.isNotEmpty()) user.lastName[0].toString() else ""
        val monogram = (monogramFirst + monogramLast).uppercase()
        binding.tvToolbarAvatarMonogram.text = if (monogram.isNotEmpty()) monogram else "TG"
        
        // Avatar Image
        val photo = user.profilePhoto?.small
        if (photo != null) {
            val file = viewModel.getFile(photo.id) ?: photo
            if (file.local.isDownloadingCompleted) {
                binding.ivToolbarAvatar.visibility = View.VISIBLE
                binding.ivToolbarAvatar.load(File(file.local.path))
            } else {
                binding.ivToolbarAvatar.visibility = View.GONE
                viewModel.downloadFile(photo.id)
            }
        } else {
            binding.ivToolbarAvatar.visibility = View.GONE
        }
        
        // Status
        binding.tvToolbarStatus.text = when (user.status) {
            is TdApi.UserStatusOnline -> "online"
            is TdApi.UserStatusOffline -> "offline"
            is TdApi.UserStatusRecently -> "last seen recently"
            else -> ""
        }
    }

    private fun updateConnectionStatus(state: TdApi.ConnectionState) {
        binding.tvConnectionStatus.text = when (state) {
            is TdApi.ConnectionStateWaitingForNetwork -> "Waiting for network..."
            is TdApi.ConnectionStateConnectingToProxy -> "Connecting to proxy..."
            is TdApi.ConnectionStateConnecting -> "Connecting..."
            is TdApi.ConnectionStateUpdating -> "Updating..."
            is TdApi.ConnectionStateReady -> "Online"
            else -> "Offline"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
