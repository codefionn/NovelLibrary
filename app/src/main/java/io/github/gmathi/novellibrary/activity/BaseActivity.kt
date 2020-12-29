package io.github.gmathi.novellibrary.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.lang.launchIO
import kotlinx.coroutines.*


abstract class BaseActivity : AppCompatActivity() {

    lateinit var firebaseAnalytics: Deferred<FirebaseAnalytics>
    private val firebaseDispatcher = newSingleThreadContext("FirebaseThread")

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtain the FirebaseAnalytics instance.
        launchFirebase {
            firebaseAnalytics = async { Firebase.analytics }
        }
    }

    fun launchFirebase(block: suspend CoroutineScope.() -> Unit): Job =
            GlobalScope.launch(firebaseDispatcher, CoroutineStart.DEFAULT, block)
}