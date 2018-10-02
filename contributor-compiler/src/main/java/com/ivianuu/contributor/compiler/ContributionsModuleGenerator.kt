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

package com.ivianuu.contributor.compiler

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import dagger.Module
import javax.lang.model.element.Modifier

class ContributionsModuleGenerator(private val descriptor: ContributionsModuleDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.classBuilder(descriptor.moduleName)
            .addAnnotation(
                AnnotationSpec.builder(Module::class.java)
                    .apply {
                        descriptor.contributions
                            .map { it.moduleName }
                            .forEach { addMember("includes", "\$T.class", it) }
                    }
                    .build()
            )
            .apply {
                if (descriptor.isPublic) {
                    addModifiers(Modifier.PUBLIC)
                }
            }

        return JavaFile.builder(descriptor.moduleName.packageName(), type.build()).build()
    }

}