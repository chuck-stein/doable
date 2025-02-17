package io.chuckstein.doable

import android.app.Application
import io.chuckstein.doable.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class DoableApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@DoableApplication)
            modules(appModule)
        }
    }
}