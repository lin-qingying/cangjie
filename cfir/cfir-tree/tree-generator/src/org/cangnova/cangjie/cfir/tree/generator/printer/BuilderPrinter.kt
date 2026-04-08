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

internal class BuilderPrinter(
    printer: ImportCollectingPrinter,
) : AbstractBuilderPrinter<Element, Implementation, Field>(printer) {

    override val implementationDetailAnnotation: ClassRef<*>
        get() = cfirImplementationDetailType

    override val builderDslAnnotation: ClassRef<*>
        get() = cfirBuilderDslAnnotation

    override fun actualTypeOfField(field: Field) = field.getMutableType(forBuilder = true)

    override fun ImportCollectingPrinter.printFieldReferenceInImplementationConstructorCall(field: Field) {
        print(field.name)
        if (field is ListField && field.isMutableOrEmptyList) {
            addImport(toMutableOrEmptyImport)
            print(".toMutableOrEmpty()")
        }
    }

    override fun copyField(field: Field, originalParameterName: String, copyBuilderVariableName: String) {
        if (field.name == "attributes") {
            printer.println(copyBuilderVariableName, ".", field.name, " = ", originalParameterName, ".", field.name, ".copy()")
        } else {
            super.copyField(field, originalParameterName, copyBuilderVariableName)
        }
    }
}
