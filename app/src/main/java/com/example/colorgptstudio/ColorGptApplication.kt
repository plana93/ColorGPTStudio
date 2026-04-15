package com.example.colorgptstudio

import android.app.Application
import com.example.colorgptstudio.di.databaseModule
import com.example.colorgptstudio.di.repositoryModule
import com.example.colorgptstudio.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class ColorGptApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Timber: logging attivo solo in debug
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Koin: avvio del container di Dependency Injection
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR)
            androidContext(this@ColorGptApplication)
            modules(
                databaseModule,
                repositoryModule,
                viewModelModule
            )
        }
    }
}
