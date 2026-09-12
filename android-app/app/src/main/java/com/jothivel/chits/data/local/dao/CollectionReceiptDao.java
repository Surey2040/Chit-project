package com.jothivel.chits.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.jothivel.chits.data.local.entity.CollectionReceiptEntity;
import java.util.List;

@Dao
public interface CollectionReceiptDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(CollectionReceiptEntity receipt);

    @Query("SELECT * FROM collection_receipts WHERE requestId = :requestId LIMIT 1")
    CollectionReceiptEntity getByRequestIdSync(String requestId);

    @Query("SELECT * FROM collection_receipts ORDER BY paidAt DESC LIMIT :limit")
    List<CollectionReceiptEntity> getRecentSync(int limit);

    @Query("SELECT COALESCE(SUM(amountPaidPaise), 0) FROM collection_receipts WHERE businessDate = :businessDate AND status = 'SAVED'")
    long getTotalForDateSync(String businessDate);

    @Query("SELECT COUNT(*) FROM collection_receipts WHERE businessDate = :businessDate AND status = 'SAVED'")
    int getCountForDateSync(String businessDate);
}
