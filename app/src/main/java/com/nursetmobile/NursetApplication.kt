package com.nursetmobile

import android.app.Application
import com.nursetmobile.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Application хранит один общий экземпляр базы данных
class NursetApplication : Application() {

    val database: AppDatabase
        get() = AppDatabase.getInstance(this)

    override fun onCreate() {
        super.onCreate()
        // Заполняем демоданными при первом запуске — в фоне, чтобы не тормозить старт
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.populateDemoDataIfEmpty(database)
        }
    }
}
