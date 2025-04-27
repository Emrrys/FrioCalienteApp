package com.example.epe3

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "TesoroDB.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_TESORO = "tesoro"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NOMBRE = "nombre"
        private const val COLUMN_LATITUD = "latitud"
        private const val COLUMN_LONGITUD = "longitud"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_TESORO (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_NOMBRE TEXT, " +
                "$COLUMN_LATITUD REAL, " +
                "$COLUMN_LONGITUD REAL)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TESORO")
        onCreate(db)
    }

    fun insertarTesoro(tesoro: Tesoro) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_NOMBRE, tesoro.nombre)
        values.put(COLUMN_LATITUD, tesoro.latitud)
        values.put(COLUMN_LONGITUD, tesoro.longitud)

        db.insert(TABLE_TESORO, null, values)
        db.close()
    }

    fun obtenerTesoros(): List<Tesoro> {
        val listaTesoros = mutableListOf<Tesoro>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM tesoro", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre"))
                val latitud = cursor.getDouble(cursor.getColumnIndexOrThrow("latitud"))
                val longitud = cursor.getDouble(cursor.getColumnIndexOrThrow("longitud"))

                val tesoro = Tesoro(id, nombre, latitud, longitud)
                listaTesoros.add(tesoro)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return listaTesoros
    }

}