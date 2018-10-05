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

package com.ivianuu.contributor.sample

import com.ivianuu.contributor.ContributeInjector
import dagger.Module

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Module
abstract class ViewBindingModule {

    @PerView
    @ContributeInjector(modules = [MyModule1::class, MyModule2::class])
    abstract fun bindMyView(): MyView

}

@Module
abstract class MyModule1

@Module
abstract class MyModule2