package pl.onme.playonme.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CardProgressEntity::class, RoundResultEntity::class, ModeResultEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class PlayOnMeDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        fun create(context: Context): PlayOnMeDatabase =
            Room.databaseBuilder(context, PlayOnMeDatabase::class.java, "playonme.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /** Wersja 2: tabela wyników szybkich trybów. Postęp i XP z wersji 1 zostają. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `mode_result` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mode` TEXT NOT NULL, " +
                        "`subject` TEXT NOT NULL, `day` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL, " +
                        "`correct` INTEGER NOT NULL, `total` INTEGER NOT NULL, `xp` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_mode_result_mode_subject` ON `mode_result` (`mode`, `subject`)")
            }
        }
    }
}
