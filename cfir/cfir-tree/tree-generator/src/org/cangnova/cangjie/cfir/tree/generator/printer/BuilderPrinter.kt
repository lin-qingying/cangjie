package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirBuilderDslAnnotation
import org.cangnova.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.generators.tree.AbstractBuilderPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.LeafBuilder
import org.cangnova.cangjie.generators.tree.imports.ArbitraryImportable
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

internal class BuilderPrinter(printer: ImportCollectingPrinter) :
    AbstractBuilderPrinter<Element, Implementation, Field>(printer) {
    override val implementationDetailAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override val builderDslAnnotation: ClassRef<*>
        get() = cfirBuilderDslAnnotation

    /**
     * 判断此 builder 对应的元素是否继承了 `CfirElementWithResolveState`，
     * 从而需要在 `build()` 中初始化 `resolveState`。
     */
    override fun hasPostBuildStatements(builder: LeafBuilder<Field, Element, Implementation>): Boolean {
        return builder.implementation.element.hasResolveStateField()
    }

    /**
     * 在 `.also { }` 中调用 `initDefaultResolveState()`，
     * 将新创建的元素 resolveState 初始化为 `RAW_CFIR`。
     */
    override fun ImportCollectingPrinter.printPostBuildStatements(builder: LeafBuilder<Field, Element, Implementation>) {
        if (builder.implementation.element.hasResolveStateField()) {
            addImport(ArbitraryImportable("org.cangnova.cangjie.cfir.declarations", "initDefaultResolveState"))
            println("it.initDefaultResolveState()")
        }
    }

    private fun Element.hasResolveStateField(): Boolean {
        return allFields.any { it.name == "resolveState" && it.isFinal }
    }
}
