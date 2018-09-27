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
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.WildcardTypeName
import dagger.Binds
import dagger.Module
import dagger.Subcomponent
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import javax.lang.model.element.Modifier

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ContributeInjectorGenerator(private val injectorDescriptor: ContributeInjectorDescriptor) {

    fun generate(): JavaFile {
        val type = TypeSpec.classBuilder(injectorDescriptor.moduleName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(
                AnnotationSpec.builder(Module::class.java)
                    .addMember("subcomponents", "\$T.class", injectorDescriptor.subcomponentName)
                    .build()
            )
            .addMethod(constructor())
            .addMethod(bindInjectorMethod())
            .addType(subcomponent())
            .build()

        return JavaFile.builder(injectorDescriptor.moduleName.packageName(), type)
            .build()
    }

    private fun constructor(): MethodSpec {
        return MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build()
    }

    private fun subcomponent(): TypeSpec {
        val subcomponent = TypeSpec.interfaceBuilder(injectorDescriptor.subcomponentName)
            .addModifiers(Modifier.PUBLIC)
            .addSuperinterface(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector::class.java),
                    injectorDescriptor.target
                )
            )
            .addAnnotation(subcomponentAnnotation())
            .addType(subcomponentBuilder())

        injectorDescriptor.scopes.forEach { subcomponent.addAnnotation(AnnotationSpec.get(it)) }

        return subcomponent.build()
    }

    private fun subcomponentAnnotation(): AnnotationSpec {
        val annotation = AnnotationSpec.builder(Subcomponent::class.java)

        injectorDescriptor.modules
            .forEach { annotation.addMember("modules", "\$T.class", it) }

        return annotation.build()
    }

    private fun bindInjectorMethod(): MethodSpec {
        return MethodSpec.methodBuilder("bindInjectorFactory")
            .addModifiers(Modifier.ABSTRACT)
            .addAnnotation(Binds::class.java)
            .addAnnotation(IntoMap::class.java)
            .addAnnotation(
                AnnotationSpec.builder(injectorDescriptor.mapKey)
                    .addMember("value", "\$T.class", injectorDescriptor.target)
                    .build()
            )
            .addParameter(
                injectorDescriptor.subcomponentName.nestedClass("Builder"),
                "builder"
            )
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector.Factory::class.java),
                    WildcardTypeName.subtypeOf(injectorDescriptor.baseType)
                )
            )
            .build()
    }

    private fun subcomponentBuilder(): TypeSpec {
        return TypeSpec.classBuilder(injectorDescriptor.subcomponentBuilderName)
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT, Modifier.STATIC)
            .addAnnotation(Subcomponent.Builder::class.java)
            .superclass(
                ParameterizedTypeName.get(
                    ClassName.get(AndroidInjector.Builder::class.java),
                    injectorDescriptor.target
                )
            )
            .build()
    }
}