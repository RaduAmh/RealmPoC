package com.example.realmpoc

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import org.bson.types.ObjectId

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