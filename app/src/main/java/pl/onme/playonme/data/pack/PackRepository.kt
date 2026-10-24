package pl.onme.playonme.data.pack

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Ładuje paczki z assets/packs i trzyma je w pamięci (paczki są małe). */
class PackRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private var cache: List<Pack>? = null

    suspend fun allPacks(): List<Pack> = mutex.withLock {
        cache ?: withContext(Dispatchers.IO) {
            assets.list(PACKS_DIR).orEmpty()
                .filter { it.endsWith(".json") }
                .sorted()
                .map { name -> PackParser.parse(assets.open("$PACKS_DIR/$name").bufferedReader().use { it.readText() }) }
        }.also { cache = it }
    }

    suspend fun packsForSubject(subject: String): List<Pack> = allPacks().filter { it.subject == subject }

    suspend fun pack(id: String): Pack = allPacks().first { it.id == id }

    private companion object {
        const val PACKS_DIR = "packs"
    }
}
