package com.jothivel.chits.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class TokenManager {
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            prefs = EncryptedSharedPreferences.create(
                    context,
                    "ChitsAuthPrefsSecure",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            editor = prefs.edit();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback for extreme cases
            prefs = context.getSharedPreferences("ChitsAuthPrefs", Context.MODE_PRIVATE);
            editor = prefs.edit();
        }
    }

    public void saveTokens(String accessToken, String refreshToken) {
        editor.putString("ACCESS_TOKEN", accessToken);
        editor.putString("REFRESH_TOKEN", refreshToken);
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString("ACCESS_TOKEN", null);
    }
    
    public String getRefreshToken() {
        return prefs.getString("REFRESH_TOKEN", null);
    }

    public void clearTokens() {
        prefs.edit().clear().apply();
    }
    

}
