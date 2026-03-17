/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.util

import org.cangnova.cangjie.cfir.tree.generator.BASE_PACKAGE
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.generators.tree.StandardTypes
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.TypeRefWithNullability
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.generators.util.GeneratorsFileUtil
import org.cangnova.cangjie.utils.SmartPrinter
import java.io.File

inline fun File.writeToFileUsingSmartPrinterIfFileContentChanged(block: SmartPrinter.() -> Unit) {
    val newText = buildString { SmartPrinter(this).block() }
    GeneratorsFileUtil.writeFileIfContentChanged(this, newText, logNotChanged = false)
}

fun Field.getMutableType(forBuilder: Boolean = false): TypeRefWithNullability = when (this) {
    is ListField -> when {
        forBuilder -> StandardTypes.mutableList
        !isMutable -> StandardTypes.list
        isMutableOrEmptyList -> type(BASE_PACKAGE, "MutableOrEmptyList",  kind = TypeKind.Class)
        else -> StandardTypes.mutableList
    }.withArgs(baseType).copy(nullable)
    else -> typeRef
}
