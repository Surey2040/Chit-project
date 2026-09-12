package com.jothivel.chits.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jothivel.chits.data.firebase.AgentLoginResult
import com.jothivel.chits.data.firebase.AgentAuthRepository
import kotlinx.coroutines.launch

/**
 * Labour/field-agent counterpart to [LoginViewModel]. Kept as a separate Kotlin ViewModel
 * (rather than folded into the Java admin one) so the existing PIN-login path stays untouched —
 * this one talks to Firestore via [AgentAuthRepository] instead of [com.jothivel.chits.utils.AppPreferences].
 */
class AgentLoginViewModel(application: Application) : AndroidViewModel(application) {

    val loginSuccess = MutableLiveData<Boolean>()
    val loginError = MutableLiveData<String?>()
    val isLoading = MutableLiveData(false)

    fun login(phone: String, pin: String) {
        if (phone.isBlank()) {
            loginError.value = "Enter your mobile number"
            return
        }
        if (pin.length != 4) {
            loginError.value = "Enter your 4-digit PIN"
            return
        }
        isLoading.value = true
        viewModelScope.launch {
            when (val result = AgentAuthRepository.login(getApplication(), phone.trim(), pin)) {
                is AgentLoginResult.Success -> {
                    isLoading.value = false
                    loginSuccess.value = true
                }
                is AgentLoginResult.Failure -> {
                    isLoading.value = false
                    loginError.value = result.message
                }
            }
        }
    }
}
