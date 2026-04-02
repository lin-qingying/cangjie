package org.cangnova.cangjie.cfir.tree.generator.printer

import org.cangnova.cangjie.cfir.tree.generator.cfirTransformerType
import org.cangnova.cangjie.cfir.tree.generator.model.Field
import org.cangnova.cangjie.generators.tree.ImplementationKind
import org.cangnova.cangjie.generators.tree.TypeRef
import org.cangnova.cangjie.generators.tree.TypeVariable
import org.cangnova.cangjie.generators.tree.withArgs
import org.cangnova.cangjie.generators.tree.printer.FunctionParameter
import org.cangnova.cangjie.generators.tree.printer.ImportCollectingPrinter
import org.cangnova.cangjie.generators.tree.printer.printFunctionDeclaration

fun ImportCollectingPrinter.transformFunctionDeclaration(
    field: Field,
    returnType: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration(field.name.replaceFirstChar(Char::uppercaseChar), returnType, override, implementationKind)
}

fun ImportCollectingPrinter.transformOtherChildrenFunctionDeclaration(
    element: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    transformFunctionDeclaration("OtherChildren", element, override, implementationKind)
}

private fun ImportCollectingPrinter.transformFunctionDeclaration(
    transformName: String,
    returnType: TypeRef,
    override: Boolean,
    implementationKind: ImplementationKind,
) {
    val dataTP = TypeVariable("D")
    printFunctionDeclaration(
        name = "transform$transformName",
        parameters = listOf(
            FunctionParameter("transformer", cfirTransformerType.withArgs(dataTP)),
            FunctionParameter("data", dataTP),
        ),
        returnType = returnType,
        typeParameters = listOf(dataTP),
        modality = org.cangnova.cangjie.descriptors.Modality.ABSTRACT.takeIf {
            implementationKind == ImplementationKind.AbstractClass || implementationKind == ImplementationKind.SealedClass
        },
        override = override,
    )
}
