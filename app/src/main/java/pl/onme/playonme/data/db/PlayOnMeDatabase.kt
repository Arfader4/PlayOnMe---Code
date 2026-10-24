package pl.onme.playonme.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CardProgressEntity::class, RoundResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PlayOnMeDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        fun create(context: Context): PlayOnMeDatabase =
            Room.databaseBuilder(context, PlayOnMeDatabase::class.java, "playonme.db").build()
    }
}
