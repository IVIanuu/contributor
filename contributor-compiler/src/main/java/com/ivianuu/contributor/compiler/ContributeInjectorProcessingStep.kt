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

import com.google.auto.common.AnnotationMirrors
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.ivianuu.contributor.ContributeInjector
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Scope
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

/**
 * @author Manuel Wrage (IVIanuu)
 */
class ContributeInjectorProcessingStep(
    private val processingEnv: ProcessingEnvironment,
    private val keyFinder: InjectorKeyFinderProcessingStep
) : BasicAnnotationProcessor.ProcessingStep {

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<Element> {
        val descriptors = elementsByAnnotation[ContributeInjector::class.java]
            .asSequence()
            .filterIsInstance<ExecutableElement>()
            .mapNotNull { createContributeInjectorDescriptor(it) }
            .toList()

        descriptors
            .map { ContributeInjectorGenerator(it) }
            .map { it.generate() }
            .forEach { writeFile(processingEnv, it) }

        if (descriptors.isNotEmpty()) {
            val descriptor = createContributesModuleDescriptor(descriptors)
            val generator = ContributionsModuleGenerator(descriptor)
            writeFile(processingEnv, generator.generate())
        }

        return mutableSetOf()
    }

    override fun annotations() =
        mutableSetOf(ContributeInjector::class.java)

    private fun createContributeInjectorDescriptor(element: ExecutableElement): ContributeInjectorDescriptor? {
        val injectedType = element.returnType

        val key = keyFinder.keys.firstOrNull {
            processingEnv.typeUtils.isAssignable(
                injectedType,
                processingEnv.elementUtils.getTypeElement(it.baseType.toString()).asType()
            )
        }

        if (key == null) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "no matching binding key found for $element"
            )

            return null
        }

        if (!MoreElements.isAnnotationPresent(element.enclosingElement, dagger.Module::class.java)) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@ContributeInjector must be in @Module class"
            )
            return null
        }

        val builder =
            ContributeInjectorDescriptor.builder(element, key.baseType, key.mapKey)

        AnnotationMirrors.getAnnotatedAnnotations(element, Scope::class.java)
            .forEach { builder.addScope(it) }

        val annotation =
            MoreElements.getAnnotationMirror(element, ContributeInjector::class.java).get()

        annotation.getClassArrayValues(
            "modules")
            .map { processingEnv.elementUtils.getTypeElement(it) }
            .map { ClassName.get(it) }
            .forEach { builder.addModule(it) }

        return builder.build()
    }

    private fun createContributesModuleDescriptor(contributions: List<ContributeInjectorDescriptor>): ContributionsModuleDescriptor {
        val firstContribution = contributions.first()

        val module = firstContribution.element.enclosingElement as TypeElement

        val contributionsName = ClassName.bestGuess(module.qualifiedName.toString() + "_Contributions")

        return ContributionsModuleDescriptor(contributionsName, contributions.toSet())
    }
}