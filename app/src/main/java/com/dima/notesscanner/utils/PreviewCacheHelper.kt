package com.dima.notesscanner.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PreviewCacheHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "preview_cache.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "preview_cache"
        const val COL_PDF_PATH = "pdf_path"
        const val COL_PREVIEW_PATH = "preview_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_NAME (
                $COL_PDF_PATH TEXT PRIMARY KEY,
                $COL_PREVIEW_PATH TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }
}