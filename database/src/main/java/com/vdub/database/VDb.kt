package com.vdub.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vdub.database.dao.AnalysisDao
import com.vdub.database.dao.SegmentDao
import com.vdub.database.dao.SpeakerDao
import com.vdub.database.entity.AnalysisEntity
import com.vdub.database.entity.SegmentEntity
import com.vdub.database.entity.SpeakerEntity

@Database(
    entities = [
        AnalysisEntity::class,
        SegmentEntity::class,
        SpeakerEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class VDb : RoomDatabase() {
    abstract fun analysisDao(): AnalysisDao
    abstract fun segmentDao(): SegmentDao
    abstract fun speakerDao(): SpeakerDao

    companion object {
        const val DATABASE_NAME = "vdub_database"
    }
}
