package com.example.realmpoc

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.sync.SyncConfiguration

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var app: App
    private lateinit var surgeries: RealmResults<Surgery>
    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.list_view)

        Realm.init(this)
        val appID = "application-0-ltvro"
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
                        .waitForInitialRemoteData()
                        .build()

                    Realm.getInstanceAsync(config, object : Realm.Callback() {
                        override fun onSuccess(realm: Realm) {
                            Log.v("INSTANCE", "Successfully fetched realm instance.")

                            surgeries = realm.where<Surgery>().findAllAsync()
                            surgeries.addChangeListener{ collection ->
                                Log.v("COLLECTION", "Successfully fetched collection. Count: ${collection.size}.")
                                val list: MutableList<String> = mutableListOf()
                                collection.forEachIndexed { index, surgery ->
                                    list.add("${index + 1}. ${surgery.Procedure?.Code} ${surgery.Procedure?.Name}")
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

