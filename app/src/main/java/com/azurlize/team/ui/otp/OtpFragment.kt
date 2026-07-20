package com.azurlize.team.ui.otp

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.azurlize.team.R
import com.azurlize.team.databinding.FragmentOtpBinding
import com.azurlize.team.telegram.client.TelegramClientProvider
import com.azurlize.team.telegram.session.TelegramSessionManager
import org.drinkless.tdlib.TdApi

class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: TelegramSessionManager
    private var phoneNumber: String = ""
    private var resendTimer: CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sessionManager = TelegramSessionManager(requireContext())
        phoneNumber = arguments?.getString("phoneNumber") ?: ""

        // Set the beautiful localized description
        binding.tvOtpDesc.text = getString(R.string.otp_desc, phoneNumber)

        // Initialize click listeners
        binding.btnVerify.setOnClickListener {
            handleOtpVerify()
        }

        binding.btnSubmitPassword.setOnClickListener {
            handle2faVerify()
        }

        binding.tvWrongNumber.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set("phoneNumber", phoneNumber)
            findNavController().popBackStack()
        }

        binding.btnResend.setOnClickListener {
            setLoading(true)
            try {
                val client = TelegramClientProvider.getClient()
                client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), { result ->
                    activity?.runOnUiThread {
                        if (!isAdded) return@runOnUiThread
                        setLoading(false)
                        if (result is TdApi.Error) {
                            Toast.makeText(context, "Resend Failed: ${result.message}", Toast.LENGTH_LONG).show()
                        } else {
                            startResendTimer()
                            Toast.makeText(context, "Verification code sent again", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Start the 60s countdown timer
        startResendTimer()
    }

    private fun startResendTimer() {
        binding.btnResend.isEnabled = false
        
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.btnResend.text = getString(R.string.otp_btn_resend_waiting, secondsRemaining)
            }

            override fun onFinish() {
                if (!isAdded) return
                binding.btnResend.isEnabled = true
                binding.btnResend.text = getString(R.string.otp_btn_resend)
            }
        }.start()
    }

    private fun handleOtpVerify() {
        val code = binding.etCode.text.toString().trim()

        if (code.isEmpty()) {
            binding.tilCode.error = getString(R.string.otp_error_empty_code)
            return
        } else if (code.length < 5 || code.length > 6) {
            binding.tilCode.error = getString(R.string.otp_error_invalid_code_length)
            return
        } else {
            binding.tilCode.error = null
        }

        setLoading(true)

        try {
            val client = TelegramClientProvider.getClient()
            
            // Send OTP code verification request to TDLib Client
            client.send(TdApi.CheckAuthenticationCode(code), { result ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)

                    if (result is TdApi.Error) {
                        Toast.makeText(context, "Verification Failed: ${result.message}", Toast.LENGTH_LONG).show()
                    } else {
                        // Check if TDLib state moved to wait 2FA password
                        val state = TelegramClientProvider.getCurrentState()
                        if (state is TdApi.AuthorizationStateWaitPassword) {
                            switchTo2faLayout()
                        } else if (state is TdApi.AuthorizationStateReady) {
                            fetchUserAndNavigate()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handle2faVerify() {
        val password = binding.etPassword.text.toString().trim()

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.otp_error_empty_password)
            return
        } else {
            binding.tilPassword.error = null
        }

        setLoading(true)

        try {
            val client = TelegramClientProvider.getClient()
            
            // Check 2FA cloud password with TDLib Client
            client.send(TdApi.CheckAuthenticationPassword(password), { result ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)

                    if (result is TdApi.Error) {
                        Toast.makeText(context, "Incorrect Password: ${result.message}", Toast.LENGTH_LONG).show()
                    } else {
                        fetchUserAndNavigate()
                    }
                }
            })
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun switchTo2faLayout() {
        binding.layoutOtpCode.visibility = View.GONE
        binding.layout2fa.visibility = View.VISIBLE
    }

    private fun fetchUserAndNavigate() {
        setLoading(true)
        try {
            val client = TelegramClientProvider.getClient()
            client.send(TdApi.GetMe(), { result ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)

                    if (result is TdApi.User) {
                        // Cache session details securely
                        sessionManager.saveSession(result)
                        
                        // Route to main dashboard
                        findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
                    } else {
                        Toast.makeText(context, "Failed to retrieve authorized account details", Toast.LENGTH_LONG).show()
                    }
                }
            })
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(context, "Session retrieval error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressOtp.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnVerify.isEnabled = !isLoading
        binding.btnSubmitPassword.isEnabled = !isLoading
        binding.etCode.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resendTimer?.cancel()
        _binding = null
    }
}
