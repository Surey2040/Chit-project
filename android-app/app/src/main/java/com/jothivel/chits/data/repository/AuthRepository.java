package com.jothivel.chits.data.repository;

import com.jothivel.chits.data.remote.ApiService;
import com.jothivel.chits.utils.TokenManager;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private ApiService apiService;
    private TokenManager tokenManager;
    private com.jothivel.chits.utils.AppPreferences appPreferences;

    public AuthRepository(ApiService apiService, TokenManager tokenManager, com.jothivel.chits.utils.AppPreferences appPreferences) {
        this.apiService = apiService;
        this.tokenManager = tokenManager;
        this.appPreferences = appPreferences;
    }

    public void login(String phone, String username, String password, final AuthCallback callback) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("phone", phone != null ? phone : "");
        credentials.put("password", password);
        
        apiService.login(credentials).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    String token = (String) body.get("token");
                    String refreshToken = (String) body.get("refreshToken"); // Optional fallback
                    if (refreshToken == null) refreshToken = token; // MVP simplification if backend doesn't send it yet
                    
                    tokenManager.saveTokens(token, refreshToken);
                    callback.onSuccess();
                } else {
                    callback.onError("Login failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (t instanceof ConnectException || t instanceof SocketTimeoutException) {
                    callback.onError("Network error. Check server connection.");
                } else {
                    callback.onError("An error occurred: " + t.getMessage());
                }
            }
        });
    }



    public interface AuthCallback {
        void onSuccess();
        void onError(String message);
    }
}
