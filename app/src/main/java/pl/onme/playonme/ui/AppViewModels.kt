package pl.onme.playonme.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pl.onme.playonme.PlayOnMeApplication

/** Tworzy ViewModel z dostępem do zależności z [PlayOnMeApplication]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (PlayOnMeApplication) -> VM,
): VM {
    val app = LocalContext.current.applicationContext as PlayOnMeApplication
    return viewModel(key = key, factory = viewModelFactory { initializer { create(app) } })
}
