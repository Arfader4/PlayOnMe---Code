package pl.onme.playonme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pl.onme.playonme.ui.PlayOnMeApp
import pl.onme.playonme.ui.theme.PlayOnMeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlayOnMeTheme {
                PlayOnMeApp()
            }
        }
    }
}
