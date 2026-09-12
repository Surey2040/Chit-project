package com.jothivel.chits.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jothivel.chits.data.local.entity.PaymentEntity;

import java.util.List;

@Dao
public interface PaymentDao {
    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY paidAt DESC")
    LiveData<List<PaymentEntity>> getPaymentsByMember(String memberId);

    @Query("SELECT * FROM payments WHERE memberId = :memberId ORDER BY paidAt DESC")
    List<PaymentEntity> getPaymentsByMemberSync(String memberId);

    @Query("SELECT * FROM payments ORDER BY paidAt DESC")
    List<PaymentEntity> getAllPaymentsSync();

    @Query("SELECT * FROM payments WHERE memberId = :memberId AND groupId = :groupId AND installmentId = :installmentId")
    List<PaymentEntity> getPaymentsForInstallmentSync(String memberId, String groupId, String installmentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPayment(PaymentEntity payment);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PaymentEntity> payments);
}
