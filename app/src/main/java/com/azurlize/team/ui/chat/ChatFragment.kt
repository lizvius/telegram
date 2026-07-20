package com.azurlize.team.ui.chat

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentChatBinding
import com.azurlize.team.databinding.SheetAttachmentsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { sendMedia(it, "photo") }
    }

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { sendMedia(it, "video") }
    }

    private val pickFile = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sendMedia(it, "file") }
    }

    private val pickAudio = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sendMedia(it, "audio") }
    }

    private val pickVoice = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sendMedia(it, "voice") }
    }

    private lateinit var messageAdapter: MessageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val chatId = arguments?.getLong("chatId") ?: 0L
        viewModel.setChatId(chatId)
        
        setupToolbar()
        setupRecyclerView()
        setupInputArea()
        setupSearch()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.ivSearch.setOnClickListener {
            binding.cvMessageSearch.visibility = if (binding.cvMessageSearch.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            if (binding.cvMessageSearch.visibility == View.VISIBLE) {
                binding.searchViewMessages.requestFocus()
            }
        }
    }

    private fun setupSearch() {
        binding.searchViewMessages.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchMessages(query ?: "")
                messageAdapter.setHighlightQuery(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchMessages(newText ?: "")
                messageAdapter.setHighlightQuery(newText ?: "")
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(
            myUserId = viewModel.me.value?.id ?: 0L,
            getUser = { viewModel.getUser(it) },
            getFile = { viewModel.getFile(it) },
            downloadFile = { viewModel.downloadFile(it) },
            onMediaClick = { file ->
                openFile(file)
            }
        )
        
        val layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        
        binding.rvMessages.apply {
            this.layoutManager = layoutManager
            adapter = messageAdapter
            
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (!recyclerView.canScrollVertically(-1) && dy < 0) {
                        viewModel.loadMessages()
                    }
                }
            })
        }
    }

    private fun setupInputArea() {
        binding.fabSend.setOnClickListener {
            val text = binding.etMessage.text.toString()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text.clear()
            }
        }
        
        binding.ivAttach.setOnClickListener {
            showAttachmentSheet()
        }
    }

    private fun showAttachmentSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = SheetAttachmentsBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.llAttachPhoto.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            dialog.dismiss()
        }
        sheetBinding.llAttachVideo.setOnClickListener {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            dialog.dismiss()
        }
        sheetBinding.llAttachFile.setOnClickListener {
            pickFile.launch("*/*")
            dialog.dismiss()
        }
        sheetBinding.llAttachAudio.setOnClickListener {
            pickAudio.launch("audio/*")
            dialog.dismiss()
        }
        sheetBinding.llAttachVoice.setOnClickListener {
            pickVoice.launch("audio/*")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun sendMedia(uri: Uri, type: String) {
        val path = getFilePathFromUri(uri)
        if (path != null) {
            when (type) {
                "photo" -> viewModel.sendPhoto(path)
                "video" -> viewModel.sendVideo(path)
                "file" -> viewModel.sendDocument(path)
                "audio" -> viewModel.sendAudio(path)
                "voice" -> viewModel.sendVoiceNote(path)
            }
        } else {
            Toast.makeText(context, "Gagal mendapatkan lokasi file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(file: TdApi.File) {
        if (file.local.isDownloadingCompleted) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                File(file.local.path)
            )
            intent.setDataAndType(uri, requireContext().contentResolver.getType(uri))
            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini", Toast.LENGTH_SHORT).show()
            }
        } else {
            viewModel.downloadFile(file.id)
        }
    }
    private fun getFilePathFromUri(uri: Uri): String? {
        val returnCursor = requireContext().contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val name = returnCursor?.getString(nameIndex ?: 0) ?: "temp_file"
        returnCursor?.close()

        val file = File(requireContext().cacheDir, name)
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable: Int = inputStream?.available() ?: 0
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream?.read(buffers).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream?.close()
            outputStream.close()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.chat.collect { chat ->
                        chat?.let { updateChatInfo(it) }
                    }
                }
                launch {
                    viewModel.messages.collect { messages ->
                        messageAdapter.submitList(messages)
                        binding.tvEmptyState.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
                        
                        // Scroll to bottom if we are already near bottom or if it's a new message from us
                        if (messages.isNotEmpty()) {
                            binding.rvMessages.post {
                                if ((binding.rvMessages.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() <= 1) {
                                    binding.rvMessages.smoothScrollToPosition(0)
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.connectionState.collect { state ->
                        when (state) {
                            is TdApi.ConnectionStateWaitingForNetwork -> {
                                binding.tvChatStatus.text = "waiting for network..."
                                binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_neutral50, null))
                            }
                            is TdApi.ConnectionStateConnecting -> {
                                binding.tvChatStatus.text = "connecting..."
                            }
                            is TdApi.ConnectionStateUpdating -> {
                                binding.tvChatStatus.text = "updating..."
                            }
                            is TdApi.ConnectionStateReady -> {
                                viewModel.chat.value?.let { updateChatStatus(it) }
                            }
                        }
                    }
                }
                launch {
                    viewModel.typingActions.collect { actions ->
                        if (actions.isNotEmpty()) {
                            binding.tvChatStatus.text = "typing..."
                            binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary50, null))
                        } else {
                            viewModel.chat.value?.let { updateChatStatus(it) }
                        }
                    }
                }
                launch {
                    viewModel.searchResults.collect { results ->
                        if (results.isNotEmpty()) {
                            // Find the position of the first search result in the main message list
                            val mainMessages = viewModel.messages.value
                            val firstResultId = results.first().id
                            val position = mainMessages.indexOfFirst { it.id == firstResultId }
                            if (position != -1) {
                                binding.rvMessages.smoothScrollToPosition(position)
                            }
                        }
                    }
                }
                launch {
                    viewModel.fileUpdates.collect { file ->
                        file?.let {
                            messageAdapter.notifyDataSetChanged() // Full refresh for any file update
                            if (it.local.isDownloadingCompleted) {
                                viewModel.chat.value?.let { chat -> updateChatInfo(chat) }
                            }
                        }
                    }
                }
                launch {
                    viewModel.downloadProgress.collect {
                        messageAdapter.notifyDataSetChanged() // Refresh to show progress
                    }
                }
            }
        }
    }

    private fun updateChatInfo(chat: TdApi.Chat) {
        binding.tvChatName.text = chat.title
        updateChatStatus(chat)
        
        // Avatar
        val monogram = if (chat.title.isNotEmpty()) chat.title[0].toString().uppercase() else "?"
        binding.tvChatAvatarMonogram.text = monogram
        
        val photo = chat.photo?.small
        if (photo != null) {
            val file = viewModel.getFile(photo.id) ?: photo
            if (file.local.isDownloadingCompleted) {
                binding.ivChatAvatar.visibility = View.VISIBLE
                binding.ivChatAvatar.load(File(file.local.path))
            } else {
                binding.ivChatAvatar.visibility = View.GONE
                viewModel.downloadFile(photo.id)
            }
        } else {
            binding.ivChatAvatar.visibility = View.GONE
        }
    }

    private fun updateChatStatus(chat: TdApi.Chat) {
        when (val type = chat.type) {
            is TdApi.ChatTypePrivate -> {
                val user = viewModel.getUser(type.userId)
                if (user != null) {
                    binding.tvChatStatus.text = when (val status = user.status) {
                        is TdApi.UserStatusOnline -> "online"
                        is TdApi.UserStatusOffline -> "last seen recently" // Simplified
                        else -> "offline"
                    }
                    if (user.status is TdApi.UserStatusOnline) {
                        binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_primary50, null))
                    } else {
                        binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_neutral50, null))
                    }
                }
            }
            is TdApi.ChatTypeSupergroup -> {
                val supergroup = viewModel.getSupergroup(type.supergroupId)
                if (supergroup != null) {
                    binding.tvChatStatus.text = "${supergroup.memberCount} members"
                    binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_neutral50, null))
                }
            }
            is TdApi.ChatTypeBasicGroup -> {
                val basicGroup = viewModel.getBasicGroup(type.basicGroupId)
                if (basicGroup != null) {
                    binding.tvChatStatus.text = "${basicGroup.memberCount} members"
                    binding.tvChatStatus.setTextColor(resources.getColor(com.google.android.material.R.color.material_dynamic_neutral50, null))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
