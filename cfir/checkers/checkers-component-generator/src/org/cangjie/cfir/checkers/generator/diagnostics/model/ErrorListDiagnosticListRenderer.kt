package org.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.ifNotEmpty
import org.cangnova.cangjie.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

object ErrorListDiagnosticListRenderer : DiagnosticListRenderer() {
    const val BASE_PACKAGE = "org.cangjie.cfir.analysis.diagnostics"
    const val DIAGNOSTICS_PACKAGE = "org.cangjie.cfir.diagnostics"

    override fun render(
        file: File,
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>,
    ) {
        file.writeToFileUsingSmartPrinterIfFileContentChanged {
            render(diagnosticList, packageName, starImportsToAdd)
        }
    }

    private fun SmartPrinter.render(
        diagnosticList: DiagnosticList,
        packageName: String,
        starImportsToAdd: Set<String>,
    ) {
        printCopyright()
        println("package $packageName")
        println()
        printImports(diagnosticList, starImportsToAdd)
        println("/** Generated from: ${diagnosticList.diagnosticListDefinitionFQN} */")
        printErrorsObject(diagnosticList)
    }

    private fun SmartPrinter.printImports(diagnosticList: DiagnosticList, starImportsToAdd: Set<String>) {
        val importSet = sortedSetOf<String>()
        importSet += "org.cangjie.cfir.diagnostics.*"
        importSet += "org.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory"
        importSet += "org.cangjie.config.LanguageFeature"
        importSet += starImportsToAdd.map { "$it.*" }
        diagnosticList.allDiagnostics.forEach { diagnostic ->
            importSet += diagnostic.psiType.kClass.qualifiedName.orEmpty()
            diagnostic.parameters.forEach { parameter ->
                importSet += parameter.type.kClass.qualifiedName.orEmpty()
            }
        }
        importSet.filter { it.isNotBlank() }.forEach { println("import $it") }
        println()
    }

    private fun SmartPrinter.printErrorsObject(diagnosticList: DiagnosticList) {
        println("@Suppress(\"IncorrectFormatting\")")
        println("object ${diagnosticList.objectName} : CjDiagnosticsContainer() {")
        withIndent {
            for (group in diagnosticList.groups) {
                println("// ${group.name}")
                for (diagnostic in group.diagnostics) {
                    printDiagnostic(diagnostic)
                }
                println()
            }
            println("override fun getRendererFactory(): BaseDiagnosticRendererFactory = ${diagnosticList.objectName}DefaultMessages")
        }
        println("}")
    }

    private fun SmartPrinter.printDiagnostic(diagnostic: DiagnosticData) {
        val type = diagnostic.getFactoryTypeName()
        val arguments = buildList {
            add("\"CFIR_${diagnostic.name}\"")
            when (diagnostic) {
                is RegularDiagnosticData -> add("Severity.${diagnostic.severity.name}")
                is DeprecationDiagnosticData -> add("LanguageFeature.${diagnostic.featureForError.name}")
            }
            add(diagnostic.positioningStrategy.expressionToCreate)
            add("${diagnostic.psiType.kClass.simpleName}::class")
            add("getRendererFactory()")
        }

        print("val ${diagnostic.name}: $type")
        diagnostic.parameters.map { it.type }.ifNotEmpty { printTypeArguments(this) }
        print(" = $type(")
        print(arguments.joinToString(", "))
        println(")")
    }

    private fun SmartPrinter.printTypeArguments(typeArguments: List<KType>) {
        print("<")
        typeArguments.forEachIndexed { index, typeArgument ->
            printType(typeArgument)
            if (index != typeArguments.lastIndex) print(", ")
        }
        print(">")
    }

    private fun SmartPrinter.printType(type: KType) {
        print(type.kClass.simpleName!!)
        if (type.arguments.isNotEmpty()) {
            print("<")
            type.arguments.forEachIndexed { index, typeArgument ->
                printTypeArgument(typeArgument)
                if (index != type.arguments.lastIndex) print(", ")
            }
            print(">")
        }
        if (type.isMarkedNullable) print("?")
    }

    private fun SmartPrinter.printTypeArgument(typeArgument: KTypeProjection) {
        val typeArgumentType = typeArgument.type
        if (typeArgumentType == null) {
            print("*")
            return
        }
        when (typeArgument.variance) {
            KVariance.IN -> print("in ")
            KVariance.OUT -> print("out ")
            KVariance.INVARIANT, null -> {}
        }
        printType(typeArgumentType)
    }

    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    private fun DiagnosticData.getFactoryTypeName(): String = when (this) {
        is RegularDiagnosticData -> "CjDiagnosticFactory"
        is DeprecationDiagnosticData -> "CjDiagnosticFactoryForDeprecation"
    } + parameters.size
}
