package com.vdub.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vdub.database.VDb
import com.vdub.database.dao.AnalysisDao
import com.vdub.database.dao.SegmentDao
import com.vdub.database.dao.SpeakerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VDb {
        return Room.databaseBuilder(
            context,
            VDb::class.java,
            VDb.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.AUTOMATIC)
            .build()
    }

    @Provides
    fun provideAnalysisDao(database: VDb): AnalysisDao = database.analysisDao()

    @Provides
    fun provideSegmentDao(database: VDb): SegmentDao = database.segmentDao()

    @Provides
    fun provideSpeakerDao(database: VDb): SpeakerDao = database.speakerDao()
}
