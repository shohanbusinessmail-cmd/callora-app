package com.callora.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DestinationEntity::class,
        ScheduleEntity::class,
        ProfileEntity::class,
        SubscriptionConfigEntity::class,
        OperationEntity::class,
        HistoryEntity::class,
        ObservedStatusEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CalloraConverters::class)
abstract class CalloraDatabase : RoomDatabase() {
    abstract fun destinationDao(): DestinationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun profileDao(): ProfileDao
    abstract fun subscriptionConfigDao(): SubscriptionConfigDao
    abstract fun operationDao(): OperationDao
    abstract fun historyDao(): HistoryDao
    abstract fun observedStatusDao(): ObservedStatusDao

    companion object {
        const val NAME = "callora.db"

        fun build(context: Context): CalloraDatabase =
            Room.databaseBuilder(context, CalloraDatabase::class.java, NAME)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
