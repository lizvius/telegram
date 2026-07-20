package com.azurlize.team.ui.settings

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.azurlize.team.databinding.FragmentStorageBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StorageFragment : Fragment() {

    private var _binding: FragmentStorageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StorageViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        observeViewModel()

        binding.btnClearCache.setOnClickListener {
            viewModel.clearCache()
            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.storageStats.collectLatest { stats ->
                stats?.let { s ->
                    binding.tvCacheSize.text = Formatter.formatShortFileSize(context, s.size)
                    binding.tvDatabaseSize.text = "${s.count} files"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
