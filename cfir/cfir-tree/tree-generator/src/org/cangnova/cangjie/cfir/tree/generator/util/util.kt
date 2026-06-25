/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.tree.generator.util

import org.cangnova.cangjie.cfir.tree.generator.BASE_PACKAGE
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.StandardTypes
import org.cangnova.cangjie.generators.tree.TypeKind
import org.cangnova.cangjie.generators.tree.TypeRefWithNullability
import org.cangnova.cangjie.generators.tree.printer.FunctionParameter
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printFunctionDeclaration
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.generators.util.GeneratorsFileUtil
import org.cangnova.cangjie.utils.SmartPrinter
import java.io.File

/**
 * 使用 [SmartPrinter] 构造文件内容，并仅在内容变化时写入目标文件。
 */
inline fun File.writeToFileUsingSmartPrinterIfFileContentChanged(block: SmartPrinter.() -> Unit) {
    val newText = buildString { SmartPrinter(this).block() }
    GeneratorsFileUtil.writeFileIfContentChanged(this, newText, logNotChanged = false)
}

/**
 * 计算字段在实现类或 builder 中应使用的可变类型。
 */
fun Field.getMutableType(forBuilder: Boolean = false): TypeRefWithNullability = when (this) {
    is ListField -> when {
        forBuilder -> StandardTypes.mutableList
        !isMutable -> StandardTypes.list
        isMutableOrEmptyList -> type(BASE_PACKAGE, "MutableOrEmptyList", kind = TypeKind.Class, exactPackage = true)
        else -> StandardTypes.mutableList
    }.withArgs(baseType).copy(nullable)
    else -> typeRef
}


/**
 * 打印字段 replace 方法声明。
 */
fun ImportCollectingPrinter.replaceFunctionDeclaration(
    field: Field,
    override: Boolean,
    implementationKind: ImplementationKind,
    overriddenType: TypeRefWithNullability? = null,
    forceNullable: Boolean = false,
) {
    val capName = field.name.replaceFirstChar(Char::uppercaseChar)
    val type = overriddenType ?: field.typeRef
    val typeWithNullable = if (forceNullable) type.copy(nullable = true) else type

    printFunctionDeclaration(
        name = "replace$capName",
        parameters = listOf(FunctionParameter("new$capName", typeWithNullable)),
        returnType = StandardTypes.unit,
        modality = Modality.ABSTRACT.takeIf {
            implementationKind == ImplementationKind.AbstractClass || implementationKind == ImplementationKind.SealedClass
        },
        override = override,
        optInAnnotation = field.replaceOptInAnnotation,
    )
}
