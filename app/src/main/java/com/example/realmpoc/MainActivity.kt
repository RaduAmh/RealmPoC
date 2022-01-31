package com.example.realmpoc

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import io.realm.Realm
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import io.realm.kotlin.where
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import io.realm.mongodb.User
import io.realm.mongodb.sync.SyncConfiguration
import org.bson.types.ObjectId

class MainActivity : AppCompatActivity() {
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

                val config = SyncConfiguration.Builder(user, partition)
                    .allowQueriesOnUiThread(true)
                    .build()
                val realmInstance = Realm.getInstance(config)
                Log.i("INSTANCE", "Successfully initialized instance.")

                val surgeries = realmInstance
                    .where<Surgery>()
                    .findAll()
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

//                val task : FutureTask<String> = FutureTask(Background(user, partition, this, listView), "test")
//                val executorService: ExecutorService = Executors.newFixedThreadPool(1)
//                executorService.execute(task)
            } else {
                Log.e("USER", "Failed to log in. Error: ${it.error}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.currentUser()?.logOutAsync {
            if (it.isSuccess) {
                Log.i("USER", "Successfully logged out.")
            } else {
                Log.e("USER", "Failed to log out, error: ${it.error}")
            }
        }
    }

    class Background(private val user: User, private val partition: String, private val context: Activity, private val listView: ListView) : Runnable {
        override fun run() {
            val config = SyncConfiguration.Builder(user, partition).build()
            val backgroundThreadRealm = Realm.getInstance(config)
            Log.i("INSTANCE", "Successfully initialized instance.")

            val surgeries = backgroundThreadRealm
                .where<Surgery>()
                .limit(10)
                .findAll()

            val list: MutableList<String> = mutableListOf()
            for (surgery in surgeries) {
                Log.i("ITEM", "Document _id: ${surgery._id}.")

                list.add("Procedure: ${surgery.Procedure?.Code} ${surgery.Procedure?.Name}")
            }
            val adapter = ArrayAdapter(
                context,
                android.R.layout.simple_list_item_1,
                list
            )
            listView.adapter = adapter
            backgroundThreadRealm.close()
        }
    }
}

open class Surgery : RealmObject() {
    @PrimaryKey
    var _id = ObjectId()
    var Procedure: Surgery_Procedure? = null
}

@RealmClass(embedded = true)
open class Surgery_Procedure : RealmObject() {
    var Code: String? = null
    var Name: String? = null
}