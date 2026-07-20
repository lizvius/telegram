package com.azurlize.team.ui.settings

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.azurlize.team.BuildConfig
import com.azurlize.team.databinding.FragmentAboutBinding
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        displayAppInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun displayAppInfo() {
        binding.tvVersion.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        binding.tvAndroidVersion.text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        
        // Build date
        val sdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        binding.tvBuildDate.text = sdf.format(Date())
        
        // TDLib version - normally we could get this from an option in TDLib
        binding.tvTdlibVersion.text = "1.8.25" // Example version
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
