package com.cericatto.smartreceipts.data.local.entity

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReceiptEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract val dao: ReceiptDao
}
