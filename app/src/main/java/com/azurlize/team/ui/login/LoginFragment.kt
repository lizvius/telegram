package com.azurlize.team.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.azurlize.team.R
import com.azurlize.team.core.Country
import com.azurlize.team.core.CountryAdapter
import com.azurlize.team.core.CountryHelper
import com.azurlize.team.databinding.FragmentLoginBinding
import com.azurlize.team.telegram.client.TelegramClientProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import org.drinkless.tdlib.TdApi

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private var selectedCountry: Country? = null
    private var isUpdatingFromWatcher = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize default country to Indonesia
        updateCountryDisplay(Country("Indonesia", "+62", "🇮🇩", "ID"))

        // Add pre-fill check for when returning from "Wrong number? Edit"
        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<String>("phoneNumber")?.observe(viewLifecycleOwner) { phone ->
            if (!phone.isNullOrEmpty()) {
                binding.etPhone.setText(phone)
                binding.etPhone.setSelection(phone.length)
                // Auto detect and update country
                val detected = CountryHelper.detectCountry(phone)
                if (detected != null) {
                    updateCountryDisplay(detected)
                }
                savedStateHandle.remove<String>("phoneNumber")
            }
        }

        // Set up the interactive Country Selector card click listener
        binding.cardCountryPicker.setOnClickListener {
            showCountryPickerDialog()
        }

        // Real-time country code autocomplete detection as user types
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdatingFromWatcher) return
                val phoneText = s?.toString() ?: ""
                
                // If they cleared the plus symbol or are writing a fresh number, help them
                if (phoneText.isNotEmpty() && !phoneText.startsWith("+")) {
                    isUpdatingFromWatcher = true
                    val updated = "+$phoneText"
                    binding.etPhone.setText(updated)
                    binding.etPhone.setSelection(updated.length)
                    isUpdatingFromWatcher = false
                    return
                }

                // Match prefix to find country
                val detected = CountryHelper.detectCountry(phoneText)
                if (detected != null && detected != selectedCountry) {
                    updateCountryDisplay(detected)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnContinue.setOnClickListener {
            handleLogin()
        }
    }

    private fun updateCountryDisplay(country: Country) {
        selectedCountry = country
        binding.tvCountryFlag.text = country.flagEmoji
        binding.tvCountryName.text = country.name
        binding.tvCountryDial.text = country.dialCode
    }

    private fun showCountryPickerDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_country_picker, null)
        dialog.setContentView(dialogView)

        val etSearch = dialogView.findViewById<TextInputEditText>(R.id.et_search)
        val rvCountries = dialogView.findViewById<RecyclerView>(R.id.rv_countries)

        rvCountries.layoutManager = LinearLayoutManager(requireContext())
        
        val adapter = CountryAdapter(CountryHelper.countries) { country ->
            // Update country display info
            updateCountryDisplay(country)

            // Auto-fill phone field with chosen country's dial code
            isUpdatingFromWatcher = true
            binding.etPhone.setText(country.dialCode)
            binding.etPhone.setSelection(country.dialCode.length)
            isUpdatingFromWatcher = false

            dialog.dismiss()
        }
        rvCountries.adapter = adapter

        // Search text watcher to filter list instantly
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) {
                    CountryHelper.countries
                } else {
                    CountryHelper.countries.filter {
                        it.name.lowercase().contains(query) || it.dialCode.contains(query)
                    }
                }
                adapter.updateCountries(filtered)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialog.show()
    }

    private fun handleLogin() {
        val phoneNumber = binding.etPhone.text.toString().trim()

        if (!validatePhone(phoneNumber)) {
            binding.tilPhone.error = getString(R.string.login_error_invalid_phone)
            return
        } else {
            binding.tilPhone.error = null
        }

        // Show circular progress indicator, disable inputs
        setLoading(true)

        try {
            val client = TelegramClientProvider.getClient()
            
            // Send the phone number to the TDLib backend
            client.send(TdApi.SetAuthenticationPhoneNumber(phoneNumber, null), { result ->
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    setLoading(false)

                    if (result is TdApi.Error) {
                        Toast.makeText(context, "TDLib Authentication Error: ${result.message}", Toast.LENGTH_LONG).show()
                    } else {
                        // Navigate to OTP Screen and pass the phoneNumber as argument
                        val bundle = Bundle().apply {
                            putString("phoneNumber", phoneNumber)
                        }
                        findNavController().navigate(R.id.action_loginFragment_to_otpFragment, bundle)
                    }
                }
            })
        } catch (e: Exception) {
            setLoading(false)
            Toast.makeText(context, "Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun validatePhone(phone: String): Boolean {
        // Starts with '+' and has at least 10 characters
        return phone.startsWith("+") && phone.length >= 10
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressLogin.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnContinue.isEnabled = !isLoading
        binding.etPhone.isEnabled = !isLoading
        binding.cardCountryPicker.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
