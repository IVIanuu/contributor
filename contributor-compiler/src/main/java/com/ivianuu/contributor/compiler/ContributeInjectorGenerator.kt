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
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjectionKey
import dagger.android.AndroidInjector
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.lang.model.element.Modifier

class ContributeInjectorGenerator(private val descriptor: ContributeInjectorDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.classBuilder(descriptor.moduleName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(
                AnnotationSpec.builder(Module::class.java)
                    .addMember("subcomponents", "\$T.class", descriptor.subcomponentName)
                    .build()
            )
            .addMethod(constructor())
            .addMethod(bindInjectorMethod())
            .addType(subcomponent())
            .build()

        return JavaFile.builder(descriptor.moduleName.packageName(), type)
            .build()
    }

    private fun constructor(): MethodSpec {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build()
    }

    private fun subcomponent(): TypeSpec {
        val subcomponent = TypeSpec.interfaceBuilder(descriptor.subcomponentName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector::class.java),
                    descriptor.target
                )
            )
            .addAnnotation(subcomponentAnnotation())
            .addType(subcomponentBuilder())

        descriptor.scopes.forEach { subcomponent.addAnnotation(AnnotationSpec.get(it)) }

        return subcomponent.build()
    }

    private fun subcomponentAnnotation(): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Subcomponent::class.java)

        descriptor.modules
            .forEach { annotation.addMember("modules", "\$T.class", it) }

        return annotation.build()
    }

    private fun bindInjectorMethod(): MethodSpec {
        return MethodSpec.methodBuilder("bindInjectorFactory")
            .addModifiers(Modifier.ABSTRACT)
            .addAnnotation(Binds::class.java)
            .addAnnotation(IntoMap::class.java)
            .apply {
                if (descriptor.useStringKeys) {
                    addAnnotation(
                        AnnotationSpec.builder(AndroidInjectionKey::class.java)
                            .addMember("value", "\$S", descriptor.target.toString())
                            .build()
                    )
                } else {
                    addAnnotation(
                        AnnotationSpec.builder(ClassKey::class.java)
                            .addMember("value", "\$T.class", descriptor.target)
                            .build()
                    )
                }
            }
            .addParameter(
                descriptor.subcomponentName.nestedClass("Builder"),
                "builder"
            )
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector.Factory::class.java),
                    WildcardTypeName.subtypeOf(TypeName.OBJECT)
                )
            )
            .build()
    }

    private fun subcomponentBuilder(): TypeSpec {
        return TypeSpec.classBuilder(descriptor.subcomponentBuilderName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
            .addAnnotation(Subcomponent.Builder::class.java)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector.Builder::class.java),
                    descriptor.target
                )
            )
            .build()
    }
}