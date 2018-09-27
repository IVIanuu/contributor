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

import com.google.auto.common.AnnotationMirrors.getAnnotationValue
import com.squareup.javapoet.JavaFile
import com.sun.tools.javac.code.Attribute
import java.io.IOException
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.AnnotationMirror
import javax.tools.Diagnostic

fun writeFile(env: ProcessingEnvironment, file: JavaFile) {
    try {
        file.writeTo(env.filer)
    } catch (e: IOException) {
        //env.e { "unable to write file $file, ${e.message}" }
    }

}

fun AnnotationMirror.getClassArrayValues(name: String): List<String> {
    return (getAnnotationValue(this, name) as Attribute.Array)
        .values
        .map { it.value.toString() }
}