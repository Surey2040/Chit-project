package com.jothivel.chits.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.jothivel.chits.data.local.entity.ChitGroupEntity;

import java.util.List;

@Dao
public interface GroupDao {
    @Query("SELECT * FROM chit_groups")
    LiveData<List<ChitGroupEntity>> getAllGroups();

    @Query("SELECT * FROM chit_groups")
    List<ChitGroupEntity> getAllGroupsSync();

    @Query("SELECT * FROM chit_groups WHERE id = :id LIMIT 1")
    LiveData<ChitGroupEntity> getGroupById(String id);

    @Query("SELECT * FROM chit_groups WHERE id = :id LIMIT 1")
    ChitGroupEntity getGroupByIdSync(String id);

    @Query("SELECT * FROM chit_groups WHERE name = :name LIMIT 1")
    ChitGroupEntity getGroupByNameSync(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChitGroupEntity> groups);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGroup(ChitGroupEntity group);

    @Query("SELECT COUNT(*) FROM chit_groups")
    LiveData<Integer> getGroupCount();
}
