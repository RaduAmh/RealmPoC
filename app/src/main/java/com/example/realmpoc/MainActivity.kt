package com.example.realmpoc

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.where
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.User
import io.realm.mongodb.sync.SyncConfiguration
import org.bson.types.ObjectId
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask

class MainActivity : AppCompatActivity() {
    private lateinit var realm: Realm
    private lateinit var app: App
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

        app.loginAsync(credentials) {
            if (it.isSuccess) {
                Log.i("AUTH", "Successfully authenticated.")
                val user = app.currentUser()!!
                val partition = "61c058e5559668e69ad62a8d"

                try {
                    val config = SyncConfiguration.Builder(user, partition).build()
                    realm = Realm.getInstance(config)
                    val model = SurgeriesViewModel()
                    model.getSurgeries(realm).observe(this) { surgeries ->
                        val list: MutableList<String> = mutableListOf()
                        surgeries.forEachIndexed { index, surgery ->
                            list.add("${index + 1}. ${surgery.Procedure?.Code} ${surgery.Procedure?.Name}")
                        }
                        val adapter = ArrayAdapter(
                            this,
                            android.R.layout.simple_list_item_1,
                            list
                        )
                        listView.adapter = adapter
                    }
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

