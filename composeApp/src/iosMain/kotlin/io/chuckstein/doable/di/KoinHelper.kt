package io.chuckstein.doable.di

import io.chuckstein.doable.database.DatabaseDriverFactory
import io.chuckstein.doable.database.DoableDataSource
import io.chuckstein.doable.database.IOSDatabaseDriverFactory
import io.chuckstein.doable.tracker.TrackerStateEngine
import io.chuckstein.doable.tracker.TrackerStateMapper
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module

class KoinHelper : KoinComponent {
    val stateEngine: TrackerStateEngine by inject()
}

fun initKoin() {
    startKoin {
        modules(
            module {
                factory<DatabaseDriverFactory> { IOSDatabaseDriverFactory() }

                single { DoableDataSource(databaseDriverFactory = get()) }

                factory { TrackerStateMapper() }

                factory { TrackerStateEngine(trackerStateMapper = get(), dataSource = get()) }
            }
        )
    }
}