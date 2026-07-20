package com.azurlize.team.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentProfileBinding
import com.azurlize.team.utils.getUserStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeViewModel()

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_settingsFragment)
        }

        binding.layoutUsername.setOnClickListener {
            // Copy username logic
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
                    true
                }
                R.id.action_logout -> {
                    viewModel.logout { success ->
                        if (success) {
                            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.me.collectLatest { user ->
                user?.let {
                    updateUI(it)
                    viewModel.fetchUserFullInfo(it.id)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userFullInfo.collectLatest { info ->
                info?.let {
                    binding.tvBio.text = it.bio?.text ?: "No bio set"
                }
            }
        }
    }

    private fun updateUI(user: TdApi.User) {
        val name = "${user.firstName} ${user.lastName}".trim()
        binding.collapsingToolbar.title = name
        binding.tvStatus.text = getUserStatus(user.status)
        binding.tvPhone.text = user.phoneNumber
        val username = user.usernames?.activeUsernames?.firstOrNull()
        binding.tvUsername.text = if (username != null) "@$username" else "None"
        binding.tvTelegramId.text = "Telegram ID: ${user.id}"

        // Load profile photo
        val photo = user.profilePhoto?.big
        if (photo != null) {
            if (photo.local.isDownloadingCompleted) {
                binding.ivProfilePhoto.load(photo.local.path)
            } else {
                viewModel.getFile(photo.id)
                binding.ivProfilePhoto.setImageResource(R.drawable.bg_splash_gradient)
            }
        } else {
            binding.ivProfilePhoto.setImageResource(R.drawable.bg_splash_gradient)
        }
        
        binding.ivPremiumBadge.visibility = if (user.isPremium) View.VISIBLE else View.GONE
        binding.ivVerifiedBadge.visibility = if (user.verificationStatus?.isVerified == true) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
