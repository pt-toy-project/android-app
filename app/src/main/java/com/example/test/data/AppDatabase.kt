package com.example.test.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class AppDatabase(context: Context, name: String) : SQLiteOpenHelper(context.applicationContext, name, null, 3) {
    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        executeAsset(db, "database/schema_v3.sql")
    }

    private fun executeAsset(db: SQLiteDatabase, path: String) {
        val schema = appContext.assets.open(path).bufferedReader().use { it.readText() }
        schema.lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
            .split(';').map(String::trim).filter(String::isNotEmpty).forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion in 1..2 && newVersion == 3) { "No migration defined for $oldVersion → $newVersion" }
        if (oldVersion == 1) executeAsset(db, "database/migration_1_2.sql")
        executeAsset(db, "database/migration_2_3.sql")
    }
}
