/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.contributor.view

import android.view.View
import dagger.MapKey
import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.Multibinds
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface HasViewInjector {
    fun viewInjector(): AndroidInjector<View>
}

/**
 * View injection
 */
object ViewInjection {

    fun inject(view: View) {
        val hasViewInjector = findHasViewInjector(view)
        val viewInjector = hasViewInjector.viewInjector()
        viewInjector.inject(view)
    }

    private fun findHasViewInjector(view: View): HasViewInjector {
        if (view.parent != null) {
            var parent = view.parent

            while (parent != null) {
                if (parent is HasViewInjector) {
                    return parent
                }

                parent = parent.parent
            }
        }

        val context = view.context
        if (context is HasViewInjector) {
            return context
        }

        val applicationContext = context.applicationContext
        if (applicationContext is HasViewInjector) {
            return applicationContext
        }

        throw IllegalArgumentException("no injector found for ${view.javaClass.name}")
    }
}

/**
 * View injection module
 */
@Module
abstract class ViewInjectionModule {

    @Multibinds
    abstract fun viewInjectorFactories(): Map<Class<out View>, AndroidInjector.Factory<out View>>

}

/**
 * View key
 */
@MapKey
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ViewKey(val value: KClass<out View>)