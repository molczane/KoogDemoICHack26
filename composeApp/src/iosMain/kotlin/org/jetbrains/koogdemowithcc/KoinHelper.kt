package org.jetbrains.koogdemowithcc

import org.jetbrains.koogdemowithcc.di.initKoin

/**
 * Helper function to initialize Koin from iOS.
 * Call this from AppDelegate or SwiftUI App init.
 */
fun doInitKoin() {
    initKoin(
        platformModules = listOf()
    )
}
