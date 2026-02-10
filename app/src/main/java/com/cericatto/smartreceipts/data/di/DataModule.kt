package com.cericatto.smartreceipts.data.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.cericatto.smartreceipts.data.local.entity.ReceiptDatabase
import com.cericatto.smartreceipts.data.model.BrazilianReceiptParser
import com.cericatto.smartreceipts.data.repository.ReceiptRepositoryImpl
import com.cericatto.smartreceipts.domain.repository.ReceiptRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideContext(
        app: Application
    ): Context {
        return app.applicationContext
    }

    @Provides
    @Singleton
    fun provideReceiptDatabase(app: Application): ReceiptDatabase {
        return Room.databaseBuilder(
            app,
            ReceiptDatabase::class.java,
            "receipt_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBrazilianReceiptParser(): BrazilianReceiptParser {
        return BrazilianReceiptParser()
    }

    @Provides
    @Singleton
    fun provideReceiptRepository(
        context: Context,
        db: ReceiptDatabase,
        parser: BrazilianReceiptParser
    ): ReceiptRepository {
        return ReceiptRepositoryImpl(
            context = context,
            dao = db.dao,
            parser = parser
        )
    }
}
