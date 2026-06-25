package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirBuilderDslAnnotation
import org.cangnova.cangjie.cfir.tree.generator.cfirImplementationDetailType
import org.cangnova.cangjie.cfir.tree.generator.model.Element
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.cfir.tree.generator.model.Implementation
import org.cangnova.cangjie.cfir.tree.generator.model.ListField
import org.cangnova.cangjie.cfir.tree.generator.toMutableOrEmptyImport
import org.cangnova.cangjie.cfir.tree.generator.util.getMutableType
import org.cangnova.cangjie.generators.tree.AbstractBuilderPrinter
import org.cangnova.cangjie.generators.tree.ClassRef
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter

/**
 * CFIR builder 源码打印器。
 */
internal class BuilderPrinter(
    printer: ImportCollectingPrinter,
) : AbstractBuilderPrinter<Element, Implementation, Field>(printer) {

    /**
     * builder 生成代码使用的内部实现注解。
     */
    override val implementationDetailAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    /**
     * builder DSL 注解类型。
     */
    override val builderDslAnnotation: ClassRef<*>
        get() = cfirBuilderDslAnnotation

    /**
     * 返回 builder 中字段应使用的实际可变类型。
     */
    override fun actualTypeOfField(field: Field) = field.getMutableType(forBuilder = true)

    /**
     * 打印实现构造调用中的字段实参。
     */
    override fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: Field) {
        print(field.name)
        if (field is ListField && field.isMutableOrEmptyList) {
            addImport(toMutableOrEmptyImport)
            print(".toMutableOrEmpty()")
        }
    }

    /**
     * 打印 builder copy 逻辑中的字段复制语句。
     */
    override fun copyField(field: Field, originalParameterName: String, copyBuilderVariableName: String) {
        if (field.name == "attributes") {
            printer.println(copyBuilderVariableName, ".", field.name, " = ", originalParameterName, ".", field.name, ".copy()")
        } else {
            super.copyField(field, originalParameterName, copyBuilderVariableName)
        }
    }
}
