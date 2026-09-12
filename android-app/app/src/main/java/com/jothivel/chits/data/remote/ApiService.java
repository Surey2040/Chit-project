package com.jothivel.chits.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.data.local.entity.MemberEntity;
import java.util.List;
import java.util.Map;

public interface ApiService {
    @POST("auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> credentials);

    @GET("groups")
    Call<List<ChitGroupEntity>> getGroups();

    @GET("members")
    Call<List<MemberEntity>> getMembers();

    @POST("groups")
    Call<Map<String, Object>> createGroup(@Body Map<String, Object> groupData);

    @POST("groups/{id}/installments/{no}/auction")
    Call<Map<String, Object>> recordAuction(
            @retrofit2.http.Path("id") String groupId,
            @retrofit2.http.Path("no") int installmentNo,
            @Body Map<String, Object> auctionData
    );

    @POST("payments")
    Call<Map<String, Object>> recordPayment(@Body Map<String, Object> paymentData);

    @GET("reports/dashboard")
    Call<Map<String, Object>> getDashboardStats();
}
