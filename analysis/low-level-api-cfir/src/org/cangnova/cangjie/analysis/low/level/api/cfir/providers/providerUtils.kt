/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.providers

import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.DeserializedContainerSourceWithJvmClassName
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.JvmStubDeserializedBuiltInsContainerSource
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.load.kotlin.FacadeClassSource
import org.cangnova.cangjie.load.kotlin.KotlinJvmBinarySourceElement
import org.cangnova.cangjie.resolve.jvm.JvmClassName
import java.util.*

internal fun <T : Any> Optional<T>.getOrNull(): T? = orElse(null)

fun CfirCallableSymbol<*>.jvmClassNameIfDeserialized(): JvmClassName? {
    return when (val containerSource = fir.containerSource) {
        is JvmStubDeserializedBuiltInsContainerSource -> containerSource.facadeClassName
        is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
        is DeserializedContainerSourceWithJvmClassName -> containerSource.className
        is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
        else -> null
    }
}
