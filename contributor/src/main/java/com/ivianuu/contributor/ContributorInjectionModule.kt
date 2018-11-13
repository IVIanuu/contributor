package com.ivianuu.contributor

import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.Multibinds

/**
 * Use this module only if you don't add the AndroidInjectionModule
 */
@Module
abstract class ContributorInjectionModule {

    @Multibinds
    internal abstract fun allInjectorFactories(): Map<Class<*>, AndroidInjector.Factory<*>>

    @Multibinds
    internal abstract fun allInjectorFactoriesWithStringKeys(): Map<String, AndroidInjector.Factory<*>>
}