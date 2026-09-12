package com.jothivel.chits.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import com.jothivel.chits.utils.TokenManager;

import java.io.IOException;

public class TokenAuthenticator implements Authenticator {

    private final TokenManager tokenManager;

    public TokenAuthenticator(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        // If we already tried to refresh the token and failed, give up to avoid infinite loop
        if (response.request().header("Authorization") != null &&
            response.priorResponse() != null && 
            response.priorResponse().code() == 401) {
            
            tokenManager.clearTokens();
            return null;
        }

        String refreshToken = tokenManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return null;
        }

        // MVP fallback: Just clear tokens and force logout on 401
        // TODO: Implement actual OkHttp synchronous POST to /auth/refresh here
        
        tokenManager.clearTokens();
        return null;
    }
}
