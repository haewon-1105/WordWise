package au.edu.jcu.cp3406.wordwise

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt code generation and creates the
 * application-level dependency container that ViewModels and repositories are injected from.
 */
@HiltAndroidApp
class WordWiseApp : Application()
