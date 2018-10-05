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

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.collect.Iterables.getOnlyElement
import com.google.common.collect.SetMultimap
import com.ivianuu.contributor.InjectorKeyRegistry
import com.squareup.javapoet.ClassName
import dagger.MapKey
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic

class InjectorKeyFinderProcessingStep(private val processingEnv: ProcessingEnvironment) : BasicAnnotationProcessor.ProcessingStep {

    val keys: Set<InjectorKey>
        get() = _keys

    private val _keys = mutableSetOf<InjectorKey>()

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<Element> {
        elementsByAnnotation[InjectorKeyRegistry::class.java]
            .map(this::collectInjectorKeys)
            .forEach { _keys.addAll(it) }

        return mutableSetOf()
    }

    override fun annotations() = mutableSetOf(InjectorKeyRegistry::class.java)

    private fun collectInjectorKeys(element: Element): Set<InjectorKey> {
        val injectorKeys = mutableSetOf<InjectorKey>()

        val annotation =
            MoreElements.getAnnotationMirror(element, InjectorKeyRegistry::class.java).get()

        val keysValue = annotation.getTypeListValue("keys")

        for (keyClass in keysValue) {
            val key = processingEnv.elementUtils.getTypeElement(keyClass.toString())
            if (MoreElements.isAnnotationPresent(MoreTypes.asElement(key.asType()), MapKey::class.java)) {
                val mapKeyValue = mapKeyValue(key.asType())

                injectorKeys.add(
                    InjectorKey(
                        ClassName.bestGuess(mapKeyValue.toString()),
                        ClassName.bestGuess(key.asType().toString())
                    )
                )
            } else {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "map key annotation must be present for $keyClass")
            }
        }

        return injectorKeys
    }

    private fun mapKeyValue(annotation: TypeMirror): TypeMirror {
        val mapKeyMethods =
            ElementFilter.methodsIn(processingEnv.elementUtils.getTypeElement(annotation.toString()).enclosedElements)
        val returnType = getOnlyElement(mapKeyMethods).returnType
        return (getOnlyElement(MoreTypes.asDeclared(returnType).typeArguments) as WildcardType)
            .extendsBound
    }
}