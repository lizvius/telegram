package com.azurlize.team.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.azurlize.team.R
import com.azurlize.team.data.local.AppTheme
import com.azurlize.team.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupOptions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupOptions() {
        binding.optionNotifications.root.apply {
            text = "Notifications and Sounds"
            setIconResource(R.drawable.ic_notifications)
            setOnClickListener {
                // Navigate to Notification settings
            }
        }

        binding.optionPrivacy.root.apply {
            text = "Privacy and Security"
            setIconResource(R.drawable.ic_lock)
            setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_privacySecurityFragment)
            }
        }

        binding.optionStorage.root.apply {
            text = "Data and Storage"
            setIconResource(R.drawable.ic_storage)
            setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_storageFragment)
            }
        }

        binding.optionChatSettings.root.apply {
            text = "Chat Settings"
            setIconResource(R.drawable.ic_chat)
            setOnClickListener {
                // Chat settings
            }
        }

        binding.optionAppearance.root.apply {
            text = "Appearance"
            setIconResource(R.drawable.ic_palette)
            setOnClickListener {
                // Show theme dialog or navigate to AppearanceFragment
                showThemeDialog()
            }
        }

        binding.optionDevices.root.apply {
            text = "Devices"
            setIconResource(R.drawable.ic_devices)
            setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_devicesFragment)
            }
        }

        binding.optionLanguage.root.apply {
            text = "Language"
            setIconResource(R.drawable.ic_language)
            setOnClickListener {
                // Language settings
            }
        }

        binding.optionAbout.root.apply {
            text = "About"
            setIconResource(R.drawable.ic_info)
            setOnClickListener {
                findNavController().navigate(R.id.action_settingsFragment_to_aboutFragment)
            }
        }

        binding.optionAskQuestion.root.apply {
            text = "Ask a Question"
            setIconResource(R.drawable.ic_chat)
        }

        binding.optionTelegramFaq.root.apply {
            text = "Telegram FAQ"
            setIconResource(R.drawable.ic_info)
        }

        binding.optionPrivacyPolicy.root.apply {
            text = "Privacy Policy"
            setIconResource(R.drawable.ic_lock)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.logout { success ->
                if (success) {
                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment) // Navigate to login
                }
            }
        }
    }

    private fun showThemeDialog() {
        val themes = arrayOf("System", "Light", "Dark", "AMOLED Black")
        val themeValues = arrayOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK, AppTheme.AMOLED)
        
        var checkedItem = themeValues.indexOf(viewModel.theme.value)
        
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, checkedItem) { _, which ->
                checkedItem = which
            }
            .setPositiveButton("Apply") { _, _ ->
                viewModel.setTheme(themeValues[checkedItem])
                activity?.recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
