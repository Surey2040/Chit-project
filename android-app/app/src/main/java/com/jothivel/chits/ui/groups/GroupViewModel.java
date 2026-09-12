package com.jothivel.chits.ui.groups;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.jothivel.chits.data.local.entity.ChitGroupEntity;
import com.jothivel.chits.data.local.entity.InstallmentEntity;
import com.jothivel.chits.data.repository.GroupRepository;

import java.util.ArrayList;
import java.util.List;

public class GroupViewModel extends AndroidViewModel {

    private GroupRepository groupRepository;

    public GroupViewModel(@NonNull Application application) {
        super(application);
        groupRepository = new GroupRepository(application);
    }

    public LiveData<List<ChitGroupEntity>> getAllGroups() {
        return groupRepository.getAllGroups();
    }

    public LiveData<ChitGroupEntity> getGroupById(String id) {
        return groupRepository.getGroupById(id);
    }

    public LiveData<List<InstallmentEntity>> getInstallmentsForGroup(String groupId) {
        return groupRepository.getInstallmentsForGroup(groupId);
    }

    public void refreshGroups() {
        groupRepository.refreshGroups();
    }

    public void insertGroup(ChitGroupEntity group, int baseInstallment) {
        groupRepository.insertGroup(group, baseInstallment);
    }

    public void updateInstallmentWithKasar(InstallmentEntity installment, ChitGroupEntity group, int prizeAmount, int commissionPercentage) {
        if (group != null && prizeAmount > 0) {
            // Formula: Total Kasar = Total Chit Value - Prize Amount - Commission
            int commission = (group.chitValue * commissionPercentage) / 100;
            int totalKasar = group.chitValue - prizeAmount - commission;
            int memberKasar = totalKasar / group.subscriberCount;

            installment.payoutAmount = prizeAmount;
            installment.kasaruAmount = memberKasar;
        }
        groupRepository.updateInstallment(installment);
    }
}
