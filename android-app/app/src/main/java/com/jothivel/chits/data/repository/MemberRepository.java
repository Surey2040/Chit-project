package com.jothivel.chits.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.jothivel.chits.data.local.AppDatabase;
import com.jothivel.chits.data.local.dao.MemberDao;
import com.jothivel.chits.data.local.entity.MemberEntity;
import com.jothivel.chits.data.local.dao.ActivityLogDao;
import com.jothivel.chits.data.local.entity.ActivityLogEntity;
import com.jothivel.chits.data.remote.ApiClient;
import com.jothivel.chits.data.remote.ApiService;
import com.jothivel.chits.utils.TokenManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemberRepository {

    private MemberDao memberDao;
    private ActivityLogDao activityLogDao;
    private ApiService apiService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public MemberRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        memberDao = db.memberDao();
        activityLogDao = db.activityLogDao();
        TokenManager tokenManager = new TokenManager(application);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
    }

    public LiveData<List<MemberEntity>> getAllMembers() {
        return memberDao.getAllMembers();
    }

    public LiveData<List<MemberEntity>> getMembersByChitId(String chitId) {
        return memberDao.getMembersByChitId(chitId);
    }

    public LiveData<MemberEntity> getMemberById(String memberId) {
        return memberDao.getMemberById(memberId);
    }

    public void updateMember(MemberEntity member) {
        executor.execute(() -> {
            try {
                memberDao.updateMember(member);
                ActivityLogEntity log = new ActivityLogEntity(
                        java.util.UUID.randomUUID().toString(),
                        "MEMBER_UPDATED",
                        "Member Profile Updated",
                        "Member " + member.name + " profile was updated.",
                        System.currentTimeMillis()
                );
                activityLogDao.insertLog(log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void refreshMembers() {
        // TODO: Call API and insert into Room
        /*
        apiService.getMembers().enqueue(new Callback<List<MemberEntity>>() {
            @Override
            public void onResponse(...) {
                if (response.isSuccessful()) {
                    new Thread(() -> memberDao.insertAll(response.body())).start();
                }
            }
            ...
        });
        */
    }

    public void insertMember(MemberEntity member) {
        executor.execute(() -> {
            try {
                memberDao.insertMember(member);
                
                ActivityLogEntity log = new ActivityLogEntity(
                        java.util.UUID.randomUUID().toString(),
                        "MEMBER_ADDED",
                        "Member Profile Created/Updated",
                        "Member " + member.name + " profile was saved successfully.",
                        System.currentTimeMillis()
                );
                activityLogDao.insertLog(log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LiveData<Integer> getMemberCount() {
        return memberDao.getMemberCount();
    }
}
