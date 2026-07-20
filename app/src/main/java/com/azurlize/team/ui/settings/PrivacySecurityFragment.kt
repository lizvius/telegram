package com.azurlize.team.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentPrivacySecurityBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PrivacySecurityFragment : Fragment() {

    private var _binding: FragmentPrivacySecurityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PrivacySecurityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacySecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupOptions()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupOptions() {
        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAppLockEnabled(isChecked)
        }

        binding.switchScreenshotProtection.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setScreenshotProtection(isChecked)
            updateScreenshotProtection(isChecked)
        }

        binding.optionPhoneNumber.root.apply {
            text = "Phone Number"
            setOnClickListener { /* Privacy sub-settings */ }
        }

        binding.optionLastSeen.root.apply {
            text = "Last Seen & Online"
            setOnClickListener { /* Privacy sub-settings */ }
        }

        binding.optionProfilePhoto.root.apply {
            text = "Profile Photos"
            setOnClickListener { /* Privacy sub-settings */ }
        }

        binding.optionCalls.root.apply {
            text = "Calls"
            setOnClickListener { /* Privacy sub-settings */ }
        }
    }

    private fun updateScreenshotProtection(enabled: Boolean) {
        if (enabled) {
            activity?.window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isAppLockEnabled.collectLatest {
                binding.switchAppLock.isChecked = it
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.screenshotProtection.collectLatest {
                binding.switchScreenshotProtection.isChecked = it
                updateScreenshotProtection(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
