package com.callora.app.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DestinationEntity): Long

    @Query("SELECT * FROM destinations ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations WHERE id = :id")
    suspend fun getById(id: Long): DestinationEntity?

    @Delete
    suspend fun delete(entity: DestinationEntity)
}

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleEntity): Long

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Delete
    suspend fun delete(entity: ScheduleEntity)
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity): Long

    @Update
    suspend fun update(entity: ProfileEntity)

    @Query("SELECT * FROM profiles ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Delete
    suspend fun delete(entity: ProfileEntity)
}

@Dao
interface SubscriptionConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionConfigEntity)

    @Query("SELECT * FROM subscription_configs")
    fun observeAll(): Flow<List<SubscriptionConfigEntity>>

    @Query("SELECT * FROM subscription_configs WHERE subscriptionId = :subscriptionId")
    suspend fun getById(subscriptionId: Int): SubscriptionConfigEntity?

    @Query("DELETE FROM subscription_configs WHERE subscriptionId = :subscriptionId")
    suspend fun delete(subscriptionId: Int)
}

@Dao
interface OperationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OperationEntity)

    @Query("SELECT * FROM operations WHERE operationId = :id")
    suspend fun getById(id: String): OperationEntity?

    @Query("SELECT * FROM operations ORDER BY updatedAtEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<OperationEntity>>

    @Query("SELECT * FROM operations WHERE status IN ('PENDING','SUBMITTED')")
    suspend fun getInFlight(): List<OperationEntity>

    @Query("SELECT COUNT(*) FROM operations WHERE subscriptionId = :subscriptionId AND condition = :condition AND action = :action AND status IN ('PENDING','SUBMITTED')")
    suspend fun countInFlight(subscriptionId: Int, condition: String, action: String): Int
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<HistoryEntity>>

    @Query("DELETE FROM history")
    suspend fun clear()
}

@Dao
interface ObservedStatusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ObservedStatusEntity)

    @Query("SELECT * FROM observed_status")
    fun observeAll(): Flow<List<ObservedStatusEntity>>

    @Query("SELECT * FROM observed_status WHERE subscriptionId = :subscriptionId AND condition = :condition")
    suspend fun get(subscriptionId: Int, condition: String): ObservedStatusEntity?
}
