package pl.onme.playonme.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.onme.playonme.domain.QuickMode
import pl.onme.playonme.ui.screens.LanguageScreen
import pl.onme.playonme.ui.screens.LevelsScreen
import pl.onme.playonme.ui.screens.QuickPlayScreen
import pl.onme.playonme.ui.screens.RoundScreen

object Routes {
    const val LANGUAGES = "languages"
    const val LEVELS = "levels/{subject}"
    // Podsumowanie jest częścią rundy (ten sam ViewModel trzyma wynik), więc nie ma osobnej trasy.
    const val ROUND = "round/{packId}/{level}"
    const val QUICK = "quick/{subject}/{mode}"

    fun levels(subject: String) = "levels/$subject"
    fun round(packId: String, level: Int) = "round/$packId/$level"
    fun quick(subject: String, mode: QuickMode) = "quick/$subject/${mode.key}"
}

@Composable
fun PlayOnMeApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.LANGUAGES) {
        composable(Routes.LANGUAGES) {
            LanguageScreen(onPick = { subject -> nav.navigate(Routes.levels(subject)) })
        }
        composable(Routes.LEVELS) { entry ->
            val subject = entry.arguments?.getString("subject").orEmpty()
            LevelsScreen(
                subject = subject,
                onPickLevel = { packId, level -> nav.navigate(Routes.round(packId, level)) },
                onPickMode = { mode -> nav.navigate(Routes.quick(subject, mode)) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.ROUND) { entry ->
            val packId = entry.arguments?.getString("packId").orEmpty()
            val level = entry.arguments?.getString("level")?.toIntOrNull() ?: 1
            RoundScreen(packId = packId, level = level, onExit = { nav.popBackStack() })
        }
        composable(Routes.QUICK) { entry ->
            val subject = entry.arguments?.getString("subject").orEmpty()
            val mode = QuickMode.fromKey(entry.arguments?.getString("mode").orEmpty()) ?: QuickMode.BLITZ
            QuickPlayScreen(subject = subject, mode = mode, onExit = { nav.popBackStack() })
        }
    }
}
