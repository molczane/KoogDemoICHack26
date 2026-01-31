package org.jetbrains.koogdemowithcc.di

import com.russhwolf.settings.Settings
import org.jetbrains.koogdemowithcc.data.local.SettingsRepository
import org.jetbrains.koogdemowithcc.data.location.LocationService
import org.jetbrains.koogdemowithcc.data.remote.HttpClientFactory
import org.jetbrains.koogdemowithcc.data.remote.WeatherApiService
import org.jetbrains.koogdemowithcc.data.repository.AgentRepository
import org.jetbrains.koogdemowithcc.data.repository.AgentRepositoryImpl
import org.jetbrains.koogdemowithcc.data.repository.ChatRepository
import org.jetbrains.koogdemowithcc.data.repository.ChatRepositoryImpl
import org.jetbrains.koogdemowithcc.data.repository.MarkersRepository
import org.jetbrains.koogdemowithcc.data.repository.MarkersRepositoryImpl
import org.jetbrains.koogdemowithcc.domain.AgentEventBus
import org.jetbrains.koogdemowithcc.ui.tripplan.TripPlanViewModel
import org.jetbrains.koogdemowithcc.ui.weather.WeatherViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Common Koin modules for the application.
 * Platform-specific modules can be added via [initKoin].
 */

val dataModule = module {
    // Networking
    single { HttpClientFactory.create() }

    // API Services
    single { WeatherApiService(get()) }

    // Location
    single { LocationService() }

    // Settings/Persistence
    single { Settings() }
    single { SettingsRepository(get()) }

    // Repositories
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    single<MarkersRepository> { MarkersRepositoryImpl(get()) }
}

val agentModule = module {
    // Event bus for tool -> UI communication
    single { AgentEventBus() }

    // Agent repository
    single<AgentRepository> { AgentRepositoryImpl(get(), get(), get()) }
}

val viewModelModule = module {
    // WeatherViewModel
    factory { WeatherViewModel(get(), get(), get()) }

    // TripPlanViewModel
    factory { TripPlanViewModel(get(), get(), get(), get(), get()) }
}

/**
 * All common modules combined.
 */
fun appModules(): List<Module> = listOf(
    dataModule,
    agentModule,
    viewModelModule
)

/**
 * Initialize Koin with common and platform-specific modules.
 * Call this from platform entry points (Android Application, iOS App).
 */
fun initKoin(
    platformModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    modules(appModules() + platformModules)
}
