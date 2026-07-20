package com.azurlize.team.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azurlize.team.databinding.FragmentDevicesBinding
import com.azurlize.team.databinding.ItemSessionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevicesFragment : Fragment() {

    private var _binding: FragmentDevicesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DevicesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSessions.layoutManager = LinearLayoutManager(context)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessions.collectLatest { sessions ->
                binding.rvSessions.adapter = SessionAdapter(sessions) { sessionId ->
                    viewModel.terminateSession(sessionId)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class SessionAdapter(
        private val sessions: List<TdApi.Session>,
        private val onTerminate: (Long) -> Unit
    ) : RecyclerView.Adapter<SessionAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemSessionBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val session = sessions[position]
            holder.binding.tvDeviceName.text = "${session.deviceModel} (${session.applicationName})"
            holder.binding.tvOs.text = "${session.platform} ${session.systemVersion}, v${session.applicationVersion}"
            
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            val date = Date(session.lastActiveDate.toLong() * 1000)
            holder.binding.tvLastActive.text = "Last active: ${sdf.format(date)}"
            holder.binding.tvLocation.text = "${session.location} • ${session.ipAddress}"

            holder.binding.btnTerminate.setOnClickListener {
                onTerminate(session.id)
            }
        }

        override fun getItemCount(): Int = sessions.size
    }
}
