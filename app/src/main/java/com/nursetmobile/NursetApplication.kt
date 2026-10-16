package com.nursetmobile

import android.app.Application
import com.nursetmobile.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Application хранит один общий экземпляр базы данных
class NursetApplication : Application() {

    companion object {
        @Volatile
        private var _database: AppDatabase? = null
    }

    val database: AppDatabase
        get() {
            if (_database == null) {
                synchronized(this) {
                    _database = AppDatabase.getInstance(this@NursetApplication)
                }
            }
            return _database!!
        }

    override fun onCreate() {
        super.onCreate()
        // Заполняем демоданными при первом запуске — в фоне, чтобы не тормозить старт
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(this@NursetApplication)
            _database = db
            AppDatabase.populateDemoDataIfEmpty(db)
        }
    }
}