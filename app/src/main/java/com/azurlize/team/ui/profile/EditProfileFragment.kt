package com.azurlize.team.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentEditProfileBinding
import com.azurlize.team.utils.FileUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditProfileViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val path = FileUtils.getPath(requireContext(), it)
            if (path != null) {
                viewModel.updateProfilePhoto(path)
            } else {
                Toast.makeText(context, "Failed to get image path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeViewModel()

        binding.cvProfilePhoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()
            val username = binding.etUsername.text.toString().trim().removePrefix("@")

            if (firstName.isEmpty()) {
                binding.tilFirstName.error = "First name is required"
                return@setOnClickListener
            }

            viewModel.updateProfile(firstName, lastName, bio, username)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.me.collectLatest { user ->
                user?.let {
                    binding.etFirstName.setText(it.firstName)
                    binding.etLastName.setText(it.lastName)
                    val username = it.usernames?.activeUsernames?.firstOrNull() ?: ""
                    binding.etUsername.setText(username)
                    
                    // Avatar
                    val photo = it.profilePhoto?.small
                    if (photo?.local?.isDownloadingCompleted == true) {
                        binding.ivEditPhoto.load(photo.local.path) {
                            transformations(CircleCropTransformation())
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSave.isEnabled = !isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateResult.collectLatest { result ->
                result?.let {
                    if (it) {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
                    }
                    viewModel.resetUpdateResult()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
