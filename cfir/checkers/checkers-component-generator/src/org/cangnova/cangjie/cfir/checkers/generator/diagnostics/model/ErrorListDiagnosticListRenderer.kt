package org.cangnova.cangjie.cfir.checkers.generator.diagnostics.model

import org.cangnova.cangjie.cfir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.cangnova.cangjie.generators.util.printCopyright
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.ifNotEmpty
import org.cangnova.cangjie.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance

/**
 * 将诊断 DSL 元数据渲染成 `CfirErrors` 风格错误列表源码。
 */
object ErrorListDiagnosticListRenderer : DiagnosticListRenderer() {
    /**
     * 生成错误列表所在的基础包名。
     */
    const val BASE_PACKAGE = "org.cangnova.cangjie.cfir.analysis.diagnostics"
    /**
     * 诊断运行时 API 所在包名。
     */
    const val DIAGNOSTICS_PACKAGE = "org.cangnova.cangjie.cfir.diagnostics"

    /**
     * 渲染诊断列表到目标文件。
     */
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

    /**
     * 打印完整诊断列表文件。
     */
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

    /**
     * 根据诊断锚点类型和参数类型收集 import。
     */
    private fun SmartPrinter.printImports(diagnosticList: DiagnosticList, starImportsToAdd: Set<String>) {
        val importSet = sortedSetOf<String>()
        importSet += "org.cangnova.cangjie.cfir.diagnostics.*"
        importSet += "org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory"
        importSet += "org.cangnova.cangjie.LanguageFeature"
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

    /**
     * 打印包含所有诊断工厂的错误对象。
     */
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

    /**
     * 打印单个诊断工厂声明。
     */
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

    /**
     * 打印诊断工厂泛型实参列表。
     */
    private fun SmartPrinter.printTypeArguments(typeArguments: List<KType>) {
        print("<")
        typeArguments.forEachIndexed { index, typeArgument ->
            printType(typeArgument)
            if (index != typeArguments.lastIndex) print(", ")
        }
        print(">")
    }

    /**
     * 打印 Kotlin 反射类型对应的源码类型文本。
     */
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

    /**
     * 打印带 variance 的泛型实参。
     */
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

    /**
     * 将反射类型的 classifier 收窄为 KClass。
     */
    private val KType.kClass: KClass<*>
        get() = classifier as KClass<*>

    /**
     * 根据诊断元数据选择对应的诊断工厂类型名。
     */
    private fun DiagnosticData.getFactoryTypeName(): String = when (this) {
        is RegularDiagnosticData -> "CjDiagnosticFactory"
        is DeprecationDiagnosticData -> "CjDiagnosticFactoryForDeprecation"
    } + parameters.size
}
