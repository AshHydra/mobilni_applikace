package cz.ash.mobilniapplikace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import cz.ash.mobilniapplikace.ui.MobilniAplikaceApp
import cz.ash.mobilniapplikace.ui.theme.MobilniapplikaceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MobilniapplikaceTheme {
                MobilniAplikaceApp()
            }
        }
    }
}

