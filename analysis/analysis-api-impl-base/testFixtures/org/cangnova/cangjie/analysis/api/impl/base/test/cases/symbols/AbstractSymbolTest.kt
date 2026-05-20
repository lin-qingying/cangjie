package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * symbol 抽象测试公共基座。
 *
 * 对齐 Kotlin `AbstractSymbolTest` 的测试形态：子类负责收集 symbol，
 * 基座负责渲染、创建 pointer、跨 analyze 边界恢复并与 golden 对比。
 */
abstract class AbstractSymbolTest : AbstractAnalysisApiComponentTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolTestDirectives)

    abstract fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val initialData = analyzeForTest(mainFile) {
            collectSymbols(mainFile, testServices).symbols.map { symbol ->
                SymbolWithPointer(
                    rendered = renderSymbolForComparison(symbol),
                    pointer = symbol.createPointer(),
                )
            }
        }

        val restored = analyzeForTest(mainFile) {
            initialData.map { data ->
                val restoredSymbol = restoreSymbol(data.pointer)
                    ?: error("Symbol was not restored:\n${data.rendered}")
                val restoredRendered = renderSymbolForComparison(restoredSymbol)
                testServices.assertions.assertEquals(data.rendered, restoredRendered) {
                    "Restored symbol content is not the same."
                }
                restoredRendered
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(restored.renderAsDeclarations())
    }

    protected fun CaSession.renderSymbolForComparison(symbol: CaSymbol): String = buildString {
        appendLine(symbol::class.simpleName ?: "<anonymous symbol class>")
        appendLine("name: ${symbol.name?.asString() ?: "<anonymous>"}")
        appendLine("origin: ${symbol.origin}")
        appendLine("location: ${symbol.location}")
        appendLine("psi: ${symbol.psi?.let { it::class.simpleName } ?: "<no psi>"}")
        when (symbol) {
            is CaFileSymbol -> {
                appendLine("file: ${symbol.file.name}")
                appendLine("package: ${symbol.packageFqName.asString()}")
            }
            is CaPackageSymbol -> {
                appendLine("fqName: ${symbol.fqName.asString()}")
            }
            is CaClassLikeSymbol -> {
                appendLine("classId: ${symbol.classId?.asFqNameString() ?: "<local>"}")
            }
            is CaCallableSymbol -> {
                appendLine("callableId: ${symbol.callableId?.asSingleFqName()?.asString() ?: "<local>"}")
            }
        }
    }.trimEnd()

    private fun List<String>.renderAsDeclarations(): String =
        if (isEmpty()) "NO_SYMBOLS" else joinToString(separator = "\n\n")
}

object SymbolTestDirectives : SimpleDirectivesContainer() {
    val TARGET_CLASS_FQ_NAME by stringDirective("TARGET_CLASS_FQ_NAME", applicability = DirectiveApplicability.File)
    val TARGET_CALLABLE_FQ_NAME by stringDirective("TARGET_CALLABLE_FQ_NAME", applicability = DirectiveApplicability.File)
}

data class SymbolsData(
    val symbols: List<CaSymbol>,
)

private data class SymbolWithPointer(
    val rendered: String,
    val pointer: org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaSymbol>,
)

internal val CjDeclaration.isValidForSymbolCreation: Boolean
    get() = when (this) {
        is CjBindingPattern,
        is CjEnumConstructor,
        is CjFunctionLiteral,
        is CjConstructor<*>,
        is CjMacroDeclaration,
        is CjFinalizer,
        is CjTypeParameter,
        is CjTypeAlias,
        is CjProperty,
        is CjPropertyAccessor,
        is CjFieldVariable,
        is CjPatternVariable,
        is CjExtend,
        is CjTypeStatement,
            -> true

        is CjParameter -> !isFunctionTypeParameter() && ownerDeclaration != null
        is CjNamedFunction -> name != null
        else -> false
    }
