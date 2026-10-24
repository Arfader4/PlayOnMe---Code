package pl.onme.playonme.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pl.onme.playonme.ui.screens.LanguageScreen
import pl.onme.playonme.ui.screens.LevelsScreen
import pl.onme.playonme.ui.screens.RoundScreen
import pl.onme.playonme.ui.screens.SummaryScreen

object Routes {
    const val LANGUAGES = "languages"
    const val LEVELS = "levels/{subject}"
    const val ROUND = "round/{packId}/{level}"
    const val SUMMARY = "summary/{xp}/{correct}/{total}"

    fun levels(subject: String) = "levels/$subject"
    fun round(packId: String, level: Int) = "round/$packId/$level"
    fun summary(xp: Int, correct: Int, total: Int) = "summary/$xp/$correct/$total"
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
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.ROUND) { entry ->
            val packId = entry.arguments?.getString("packId").orEmpty()
            val level = entry.arguments?.getString("level")?.toIntOrNull() ?: 1
            RoundScreen(
                packId = packId,
                level = level,
                onFinished = { xp, correct, total ->
                    nav.navigate(Routes.summary(xp, correct, total)) {
                        popUpTo(Routes.LEVELS)
                    }
                },
            )
        }
        composable(Routes.SUMMARY) { entry ->
            val args = entry.arguments
            SummaryScreen(
                xp = args?.getString("xp")?.toIntOrNull() ?: 0,
                correct = args?.getString("correct")?.toIntOrNull() ?: 0,
                total = args?.getString("total")?.toIntOrNull() ?: 0,
                onDone = { nav.popBackStack() },
            )
        }
    }
}
