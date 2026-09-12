package com.jothivel.chits.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jothivel.chits.data.remote.ApiClient;
import com.jothivel.chits.data.remote.ApiService;
import com.jothivel.chits.data.repository.AuthRepository;
import com.jothivel.chits.utils.TokenManager;

public class LoginViewModel extends AndroidViewModel {

    private AuthRepository authRepository;
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> loginError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private com.jothivel.chits.utils.AppPreferences appPreferences;

    public LoginViewModel(@NonNull Application application) {
        super(application);
        TokenManager tokenManager = new TokenManager(application);
        ApiService apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
        appPreferences = new com.jothivel.chits.utils.AppPreferences(application);
        authRepository = new AuthRepository(apiService, tokenManager, appPreferences);
    }

    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getLoginError() { return loginError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void setupAdminProfile(String name, String phone, String username, String pin) {
        if (name == null || name.isEmpty() || phone == null || phone.isEmpty() || username == null || username.isEmpty() || pin == null || pin.isEmpty()) {
            loginError.setValue("All fields are required");
            return;
        }
        if (pin.length() < 4) {
            loginError.setValue("PIN must be at least 4 digits");
            return;
        }
        
        appPreferences.saveAdminProfile(name, phone, username);
        appPreferences.savePin(pin);
        appPreferences.setAdminSetup(true);
        // Logging in as Admin must always win over any cached Labour/agent session from a
        // previous login on this device - otherwise the app keeps routing to the agent flow
        // even after a correct admin PIN, since getUserRole() only resets via this call.
        appPreferences.clearAgentSession();
        loginSuccess.setValue(true);
    }

    public void verifyPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            loginError.setValue("Please enter your PIN");
            return;
        }

        if (appPreferences.verifyPin(pin)) {
            // Automatically set admin setup to true so session skipping works
            appPreferences.setAdminSetup(true);
            // See setupAdminProfile() above: clear any stale agent session so the app routes
            // to the admin flow, not the last-used Labour session's role.
            appPreferences.clearAgentSession();
            loginSuccess.setValue(true);
        } else {
            loginError.setValue("Invalid PIN");
        }
    }
}
