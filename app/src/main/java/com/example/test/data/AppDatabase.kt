package com.example.test.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class AppDatabase(context: Context, name: String) : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
    private val appContext = context.applicationContext

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        val schema = appContext.assets.open("database/schema_v1.sql").bufferedReader().use { it.readText() }
        schema.lineSequence().filterNot { it.trimStart().startsWith("--") }.joinToString("\n")
            .split(';').map(String::trim).filter(String::isNotEmpty).forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future schema versions must supply a reviewed, data-preserving migration.
        error("No migration defined for database version $oldVersion → $newVersion")
    }
}
