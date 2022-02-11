package com.example.realmpoc

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.kotlin.syncSession
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.ProgressMode
import io.realm.mongodb.sync.SyncConfiguration
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var app: App
    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var loader: ProgressBar
    private val viewModel = SyncViewModel()
    private var realm: Realm? = null
    private val partition = "61c058e5559668e69ad62a8e"
    private val appId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loader = findViewById(R.id.myProgressBar)
        textView = findViewById(R.id.sample_text)
        button = findViewById(R.id.button)

        loader.visibility = View.INVISIBLE
        viewModel.status.observe(this) { text ->
            textView.text = text
            if (text.contains("Status: synced") || text.contains("Error")) {
                button.isEnabled = true
                button.isClickable = true
                loader.visibility = View.INVISIBLE
            }
        }
        button.setOnClickListener {
            button.isEnabled = false
            button.isClickable = false
            initRealm()
        }
    }

    private fun initRealm() {
        Realm.init(this)
        app = App(AppConfiguration.Builder(appId).build())
        val credentials: Credentials = Credentials.emailPassword("admin12@example.com", "admin12")

        app.loginAsync(credentials) {
            if (it.isSuccess) {
                Log.i("AUTH", "Successfully authenticated.")
                viewModel.send("Status: user authenticated\n")

                val user = app.currentUser()!!
                val config = SyncConfiguration
                    .Builder(user, partition)
                    .build()
                loader.visibility = View.VISIBLE
                downloadData(config)
            } else {
                Log.e("USER", "Failed to log in. Error: ${it.error}")
                viewModel.send("Status: user failed to log in\nError: ${it.error}")
            }
        }
    }

    private fun downloadData(config: SyncConfiguration) {
        thread(start = true) {
            try {
                if (realm == null) {
                    realm = Realm.getInstance(config)
                }

                Log.v("INSTANCE", "Successfully fetched realm instance.")
                viewModel.send("Status: fetched realm instance\n")

                val startTime = System.currentTimeMillis()
                realm?.syncSession?.addDownloadProgressListener(ProgressMode.CURRENT_CHANGES) { progress ->
                    Log.v("SYNC","Download progress: ${progress.fractionTransferred}")
                    viewModel.send("Status: downloading\nProgress: ${String.format("%.2f", progress.fractionTransferred * 100)}%")

                    if (progress.isTransferComplete) {
                        val total = (System.currentTimeMillis() - startTime) / 1000
                        Log.v("SYNC", "Total time: $total s.")
                        viewModel.send("Status: synced\nTotal time: ${total}s.")
                    }
                }
                realm?.syncSession?.downloadAllServerChanges()
            } catch (ex: Exception) {
                ex.message?.let { it1 ->
                    Log.e("EX", it1)
                    viewModel.send("Error:\n${it1}.")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm?.close()
        app.currentUser()?.logOutAsync {
            if (it.isSuccess) {
                Log.i("USER", "Successfully logged out.")
            } else {
                Log.e("USER", "Failed to log out, error: ${it.error}")
            }
        }
    }
}

