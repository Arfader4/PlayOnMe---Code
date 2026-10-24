package pl.onme.playonme

import android.app.Application
import pl.onme.playonme.data.ProgressRepository
import pl.onme.playonme.data.db.PlayOnMeDatabase
import pl.onme.playonme.data.pack.PackRepository

/** Prosty ręczny kontener zależności; na MVP nie potrzeba Hilta. */
class PlayOnMeApplication : Application() {
    val packs: PackRepository by lazy { PackRepository(assets) }
    val progress: ProgressRepository by lazy { ProgressRepository(PlayOnMeDatabase.create(this)) }
}
