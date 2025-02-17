package io.chuckstein.doable.di

import io.chuckstein.doable.database.AndroidDatabaseDriverFactory
import io.chuckstein.doable.database.DatabaseDriverFactory
import io.chuckstein.doable.database.DoableDataSource
import io.chuckstein.doable.tracker.TrackerStateEngine
import io.chuckstein.doable.tracker.TrackerStateMapper
import io.chuckstein.doable.tracker.TrackerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    factory<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }

    single { DoableDataSource(databaseDriverFactory = get()) }

    factory { TrackerStateMapper() }

    factory { TrackerStateEngine(trackerStateMapper = get(), dataSource = get()) }

    viewModel { TrackerViewModel(stateEngine = get()) }
}