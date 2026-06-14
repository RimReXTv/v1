package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WalletAccount::class,
        Contact::class,
        BlockRecord::class,
        TransactionRecord::class,
        PeerRecord::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletAccountDao(): WalletAccountDao
    abstract fun contactDao(): ContactDao
    abstract fun blockRecordDao(): BlockRecordDao
    abstract fun transactionRecordDao(): TransactionRecordDao
    abstract fun peerRecordDao(): PeerRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aetheris_ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
