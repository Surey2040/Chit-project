package com.jothivel.chits.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.jothivel.chits.data.local.entity.InstallmentEntity;

import java.util.List;

@Dao
public interface InstallmentDao {

    @Query("SELECT * FROM installments WHERE groupId = :groupId ORDER BY installmentNo ASC")
    LiveData<List<InstallmentEntity>> getInstallmentsForGroup(String groupId);

    @Query("SELECT * FROM installments WHERE groupId = :groupId ORDER BY installmentNo ASC")
    List<InstallmentEntity> getInstallmentsForGroupSync(String groupId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<InstallmentEntity> installments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InstallmentEntity installment);

    @Update
    void update(InstallmentEntity installment);
}
