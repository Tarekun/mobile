package database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mydatabase.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREAZIONE_TABELLA_AUDIO = "CREATE TABLE audio (id INTEGER PRIMARY KEY, valore REAL, classificazione TEXT)"
        db?.execSQL(CREAZIONE_TABELLA_AUDIO)

        val CREAZIONE_TABELLA_WIFI = "CREATE TABLE wifi (id INTEGER PRIMARY KEY, valore REAL, classificazione TEXT)"
        db?.execSQL(CREAZIONE_TABELLA_WIFI)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Qui gestisci le migrazioni del database se necessario
    }
}
