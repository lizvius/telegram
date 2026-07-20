package com.azurlize.team.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentSplashBinding
import com.azurlize.team.telegram.client.TelegramClientProvider
import com.azurlize.team.telegram.session.TelegramSessionManager
import org.drinkless.tdlib.TdApi

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: TelegramSessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = TelegramSessionManager(requireContext())

        // Add minor artificial delay for premium Apple scale/fade layout experience
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthorizationState()
        }, 2000)
    }

    private fun checkAuthorizationState() {
        if (!isAdded) return
        
        binding.tvStatus.text = getString(R.string.splash_status_check)
        val currentState = TelegramClientProvider.getCurrentState()

        when (currentState) {
            is TdApi.AuthorizationStateReady -> {
                // If TDLib is ready and local cached user is present, route straight to Home
                if (sessionManager.restoreSession() != null) {
                    navigateToHome()
                } else {
                    // Pull User Details if not cached but TDLib says authorized
                    fetchMeAndNavigate()
                }
            }
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                // Wait for provider to finish parameters set. Listen for updates.
                listenForStateChange()
            }
            else -> {
                // WaitPhoneNumber or WaitCode, etc. means login is required.
                navigateToLogin()
            }
        }
    }

    private fun listenForStateChange() {
        val listener = object : (TdApi.AuthorizationState) -> Unit {
            override fun invoke(state: TdApi.AuthorizationState) {
                if (state !is TdApi.AuthorizationStateWaitTdlibParameters) {
                    TelegramClientProvider.removeStateListener(this)
                    checkAuthorizationState()
                }
            }
        }
        TelegramClientProvider.addStateListener(listener)
    }

    private fun fetchMeAndNavigate() {
        try {
            val client = TelegramClientProvider.getClient()
            client.send(TdApi.GetMe(), { result ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    if (result is TdApi.User) {
                        sessionManager.saveSession(result)
                        navigateToHome()
                    } else {
                        navigateToLogin()
                    }
                }
            })
        } catch (e: Exception) {
            navigateToLogin()
        }
    }

    private fun navigateToHome() {
        if (!isAdded) return
        findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
