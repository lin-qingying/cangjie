/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.utils.errors

import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 抛出带附件的“遇到非预期元素”内部错误。
 */
public fun unexpectedElementError(elementName: String, element: Any?): Nothing {
    errorWithAttachment("Unexpected $elementName ${element?.let { it::class.simpleName }}") {
        withEntry(elementName, element) { element.toString() }
    }
}

/**
 * 以 reified 类型名作为元素名称抛出“遇到非预期元素”内部错误。
 */
public inline fun <reified ELEMENT> unexpectedElementError(element: Any?): Nothing {
    unexpectedElementError(ELEMENT::class.simpleName ?: ELEMENT::class.java.name, element)
}
