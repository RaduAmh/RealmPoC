package com.example.realmpoc

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.syncSession
import io.realm.kotlin.where
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.ProgressMode
import io.realm.mongodb.sync.SyncConfiguration

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var app: App
    private lateinit var surgeries: RealmResults<Surgery>
    private lateinit var listView: ListView
    private lateinit var loader: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.list_view)
        loader = findViewById(R.id.myProgressBar)

        Realm.init(this)
        val appID = ""
        app = App(AppConfiguration.Builder(appID)
            .build())
        val credentials: Credentials = Credentials.emailPassword("admin12@example.com", "admin12")
        val context = this
        app.loginAsync(credentials) {
            if (it.isSuccess) {
                Log.i("AUTH", "Successfully authenticated.")
                val user = app.currentUser()!!
                val partition = "61c058e5559668e69ad62a8d"

                try {
                    val config = SyncConfiguration
                        .Builder(user, partition)
                        .build()

                    var total : Long = 0

                    Realm.getInstanceAsync(config, object : Realm.Callback() {
                        override fun onSuccess(realm: Realm) {
                            context.realm = realm
                            Log.v("INSTANCE", "Successfully fetched realm instance.")

                            var previousTime = System.currentTimeMillis()
                            realm.syncSession.addDownloadProgressListener(
                                ProgressMode.INDEFINITELY) { progress ->
                                val currentTime = System.currentTimeMillis()
                                val elapsedTime = (currentTime - previousTime) / 1000
                                total += elapsedTime
                                Log.v("SYNC", "Download progress: ${progress.fractionTransferred}. Elapsed time: $elapsedTime s.")
                                previousTime = currentTime

                                if (progress.isTransferComplete) {
                                    Log.v("SYNC", "Total time: $total s.")
                                    loader.visibility = View.INVISIBLE
                                }
                            }

                            surgeries = realm.where<Surgery>().findAllAsync()

                            surgeries.addChangeListener{ collection ->
                                Log.v("COLLECTION", "Successfully fetched collection. Count: ${collection.size}")

                                val list: MutableList<String> = mutableListOf()
                                collection.forEachIndexed { index, surgery ->
                                    list.add("${index + 1}. ${surgery.ProcedureCode} ${surgery.ProcedureName}")
                                }
                                val adapter = ArrayAdapter(
                                    context,
                                    android.R.layout.simple_list_item_1,
                                    list
                                )
                                listView.adapter = adapter
                            }
                        }
                    })
                }
                catch (ex: Exception) {
                    ex.message?.let { it1 -> Log.e("EX", it1) }
                }
            } else {
                Log.e("USER", "Failed to log in. Error: ${it.error}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
        app.currentUser()?.logOutAsync {
            if (it.isSuccess) {
                Log.i("USER", "Successfully logged out.")
            } else {
                Log.e("USER", "Failed to log out, error: ${it.error}")
            }
        }
    }
}

