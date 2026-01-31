package org.jetbrains.koogdemowithcc

import android.app.Application
import org.jetbrains.koogdemowithcc.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            platformModules = listOf()
        ) {
            androidLogger(Level.DEBUG)
            androidContext(this@MainApplication)
        }
    }
}
