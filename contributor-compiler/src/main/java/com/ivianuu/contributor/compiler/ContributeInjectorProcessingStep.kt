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
import com.google.common.base.CaseFormat
import com.google.common.base.Joiner
import com.google.common.collect.SetMultimap
import com.ivianuu.contributor.ContributeInjector
import com.squareup.javapoet.ClassName
import javax.annotation.processing.ProcessingEnvironment
import javax.inject.Scope
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

class ContributeInjectorProcessingStep(
    private val processingEnv: ProcessingEnvironment,
    private val useStringKeys: Boolean
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

        createContributesModuleDescriptors(descriptors)
            .map { ContributionsModuleGenerator(it) }
            .map { it.generate() }
            .forEach { writeFile(processingEnv, it) }

        return mutableSetOf()
    }

    override fun annotations() =
        mutableSetOf(ContributeInjector::class.java)

    private fun createContributeInjectorDescriptor(element: ExecutableElement): ContributeInjectorDescriptor? {
        if (!MoreElements.isAnnotationPresent(element.enclosingElement, dagger.Module::class.java)) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "@ContributeInjector must be in @Module class"
            )
            return null
        }

        val enclosingModule = ClassName.get(element.enclosingElement as TypeElement)

        val moduleName = enclosingModule
            .topLevelClassName()
            .peerClass(
                Joiner.on('_').join(enclosingModule.simpleNames())
                        + "_"
                        + CaseFormat.LOWER_CAMEL.to(
                    CaseFormat.UPPER_CAMEL,
                    element.simpleName.toString()
                )
            )

        val target = ClassName.bestGuess(element.returnType.toString())

        val baseName = target.simpleName()
        val subcomponentName = moduleName.nestedClass(baseName + "Subcomponent")
        val subcomponenBuilderName = subcomponentName.nestedClass("Builder")

        val scopes = AnnotationMirrors.getAnnotatedAnnotations(element, Scope::class.java)

        val annotation =
            MoreElements.getAnnotationMirror(element, ContributeInjector::class.java).get()

        val modules = annotation.getTypeListValue("modules")
            .map { processingEnv.elementUtils.getTypeElement(it.toString()) }
            .map { ClassName.get(it) }
            .toSet()

        return ContributeInjectorDescriptor(
            element,
            target,
            moduleName,
            modules,
            scopes,
            subcomponentName,
            subcomponenBuilderName,
            useStringKeys
        )
    }

    private fun createContributesModuleDescriptors(contributions: List<ContributeInjectorDescriptor>): List<ContributionsModuleDescriptor> {
        val byModule = contributions.groupBy {
            it.element.enclosingElement as TypeElement
        }

        return byModule.map { (module, descriptors) ->
            val contributionsName =
                ClassName.bestGuess(module.qualifiedName.toString() + "_Contributions")
            ContributionsModuleDescriptor(
                contributionsName,
                module.modifiers.contains(Modifier.PUBLIC),
                descriptors.toSet()
            )
        }
    }
}