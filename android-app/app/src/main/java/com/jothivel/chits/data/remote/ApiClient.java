package com.jothivel.chits.data.remote;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import com.jothivel.chits.utils.TokenManager;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    // ⚠️ Update this IP to match your backend server's IP/domain.
    // For Android Emulator → use 10.0.2.2 instead of 192.168.x.x
    // For physical device → use your machine's local WiFi IP (e.g. 192.168.1.36)
    // For production → use your hosted domain (e.g. https://api.yourapp.com/)
    public static final String BASE_URL = "http://192.168.0.27:3000/";

    private static Retrofit retrofit = null;

    /**
     * Call reset() whenever you need to force a new Retrofit instance
     * (e.g. after a user logs out or the base URL changes).
     */
    public static void reset() {
        retrofit = null;
    }

    public static Retrofit getClient(TokenManager tokenManager) {
        if (retrofit == null) {
            // Logging interceptor – shows full request/response in Logcat
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            TokenAuthenticator authenticator = new TokenAuthenticator(tokenManager);

            OkHttpClient client = new OkHttpClient.Builder()
                // Auth token interceptor
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = tokenManager.getAccessToken();
                    if (token != null && !token.isEmpty()) {
                        Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + token)
                            .build();
                        return chain.proceed(request);
                    }
                    return chain.proceed(original);
                })
                .authenticator(authenticator)
                // Logging interceptor (add last so it logs the final request)
                .addInterceptor(logging)
                // Connection timeouts to avoid hanging indefinitely
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

            retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        }
        return retrofit;
    }
}
