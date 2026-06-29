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
    /**
     * 当前 symbol 测试额外注册的符号定位指令。
     *
     * 公共组件指令提供基础目标字段，`SymbolTestDirectives` 补充按全限定名恢复 symbol 的输入。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(SymbolTestDirectives)

    /**
     * 收集当前测试要验证的 symbol 列表。
     *
     * 子类决定 symbol 来源，例如 PSI、FqName、引用或文件本身；基类统一负责渲染和 pointer 恢复断言。
     */
    abstract fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData

    /**
     * 执行 symbol 渲染与 pointer 恢复一致性测试。
     *
     * 第一个分析块收集 symbol 并创建 pointer，第二个分析块恢复 pointer 并确认渲染内容不变。
     */
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

    /**
     * 渲染用于 golden 比较的 symbol 摘要。
     *
     * 输出包含 symbol 类型、名称、origin、location、PSI 类型，以及不同 symbol 家族的稳定身份字段。
     */
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

    /**
     * 将多个 symbol 渲染结果拼接为 declaration 风格的 golden 文本。
     *
     * 空集合显式输出 `NO_SYMBOLS`，避免空文件与缺失输出混淆。
     */
    private fun List<String>.renderAsDeclarations(): String =
        if (isEmpty()) "NO_SYMBOLS" else joinToString(separator = "\n\n")
}

/**
 * symbol 测试使用的附加指令集合。
 *
 * 这些指令支持按全限定名直接恢复 class-like 或 callable symbol。
 */
object SymbolTestDirectives : SimpleDirectivesContainer() {
    /**
     * 当前测试要通过 `ClassId.topLevel` 查找的 class-like 全限定名。
     *
     * 指令可重复声明，用于一次测试多个类型声明。
     */
    val TARGET_CLASS_FQ_NAME by stringDirective("TARGET_CLASS_FQ_NAME", applicability = DirectiveApplicability.File)
    /**
     * 当前测试要通过顶层 callable 查询查找的 callable 全限定名。
     *
     * 指令可重复声明，用于一次测试多个函数或属性符号。
     */
    val TARGET_CALLABLE_FQ_NAME by stringDirective("TARGET_CALLABLE_FQ_NAME", applicability = DirectiveApplicability.File)
}

/**
 * 子类收集到的 symbol 数据。
 *
 * 基类只关心公开 symbol 列表，不关心这些 symbol 的具体收集方式。
 */
data class SymbolsData(
    /**
     * 当前测试需要渲染并创建 pointer 的 symbol 集合。
     */
    val symbols: List<CaSymbol>,
)

/**
 * 首次分析中渲染出的 symbol 文本及其 pointer。
 *
 * 第二次分析会用 pointer 恢复 symbol，并与该初始文本比较。
 */
private data class SymbolWithPointer(
    /**
     * 初始 symbol 的稳定渲染文本。
     */
    val rendered: String,
    /**
     * 初始 symbol 创建出的公开 symbol pointer。
     */
    val pointer: org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer<CaSymbol>,
)

/**
 * 判断声明是否适合作为 symbol 创建测试的输入。
 *
 * 该过滤器排除 function type parameter、匿名函数等当前公开 symbol API 不稳定或不应直接成符号的声明形态。
 */
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
