package au.edu.jcu.cp3406.wordwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import au.edu.jcu.cp3406.wordwise.ui.navigation.WordWiseNavHost
import au.edu.jcu.cp3406.wordwise.ui.theme.WordWiseTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single-activity app: every screen is a Compose destination inside [WordWiseNavHost]. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WordWiseTheme {
                WordWiseNavHost()
            }
        }
    }
}
