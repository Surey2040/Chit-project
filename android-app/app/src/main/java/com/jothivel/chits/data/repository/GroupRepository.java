package com.jothivel.chits.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.jothivel.chits.data.local.AppDatabase;
import com.jothivel.chits.data.local.dao.GroupDao;
import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.data.local.dao.InstallmentDao;
import com.jothivel.chits.data.local.entity.InstallmentEntity;
import com.jothivel.chits.data.local.dao.ActivityLogDao;
import com.jothivel.chits.data.local.entity.ActivityLogEntity;
import com.jothivel.chits.data.remote.ApiClient;
import com.jothivel.chits.data.remote.ApiService;
import com.jothivel.chits.utils.TokenManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupRepository {

    private GroupDao groupDao;
    private InstallmentDao installmentDao;
    private ActivityLogDao activityLogDao;
    private ApiService apiService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GroupRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        groupDao = db.groupDao();
        installmentDao = db.installmentDao();
        activityLogDao = db.activityLogDao();
        TokenManager tokenManager = new TokenManager(application);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
    }

    // Room returns LiveData, so observers are automatically notified of changes
    public LiveData<List<ChitGroupEntity>> getAllGroups() {
        return groupDao.getAllGroups();
    }

    public LiveData<ChitGroupEntity> getGroupById(String id) {
        return groupDao.getGroupById(id);
    }

    public LiveData<List<InstallmentEntity>> getInstallmentsForGroup(String groupId) {
        return installmentDao.getInstallmentsForGroup(groupId);
    }

    public void updateInstallment(InstallmentEntity installment) {
        executor.execute(() -> {
            installmentDao.update(installment);
        });
    }

    public void refreshGroups() {
        // TODO: Call API and insert into Room
        /*
        apiService.getGroups().enqueue(new Callback<List<ChitGroupEntity>>() {
            @Override
            public void onResponse(...) {
                if (response.isSuccessful()) {
                    new Thread(() -> groupDao.insertAll(response.body())).start();
                }
            }
            ...
        });
        */
    }

    public void insertGroup(ChitGroupEntity group, int baseInstallment) {
        executor.execute(() -> {
            try {
                groupDao.insertGroup(group);
                
                // If this is a template-based group, automatically generate installments
                if (baseInstallment > 0) {
                    List<InstallmentEntity> installments = new ArrayList<>();
                    for (int i = 1; i <= group.durationMonths; i++) {
                        InstallmentEntity inst = new InstallmentEntity();
                        inst.id = UUID.randomUUID().toString();
                        inst.groupId = group.id;
                        inst.installmentNo = i;
                        inst.baseAmount = baseInstallment;
                        inst.kasaruAmount = null; // To be calculated
                        inst.payoutAmount = null; // To be entered
                        inst.status = "PENDING";
                        installments.add(inst);
                    }
                    installmentDao.insertAll(installments);
                }
                
                ActivityLogEntity log = new ActivityLogEntity(
                        UUID.randomUUID().toString(),
                        "GROUP_CREATED",
                        "New Chit Group Created",
                        "Group " + group.name + " (" + group.registerNo + ") was successfully created.",
                        System.currentTimeMillis()
                );
                activityLogDao.insertLog(log);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public LiveData<Integer> getGroupCount() {
        return groupDao.getGroupCount();
    }
}
