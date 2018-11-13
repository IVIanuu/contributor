package com.ivianuu.contributor.sample

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Module
abstract class ServiceBindingModule {

    @ContributesAndroidInjector
    abstract fun bindMyService(): MyService
}