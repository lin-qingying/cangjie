

package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.compile.CaCodeFragmentCapturedValue
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.containingCjFileIfAny
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import java.util.*

/**
 * 代码片段捕获到的外部符号及其公开 API 表示。
 */
@CaImplementationDetail
@OptIn(CaExperimentalApi::class)
class CodeFragmentCapturedSymbol(
    /**
     * Analysis API 暴露给调用方的捕获值描述。
     */
    val value: CaCodeFragmentCapturedValue,
    /**
     * 捕获值对应的底层 CFIR symbol。
     */
    val symbol: CfirBasedSymbol<*>,
    /**
     * 捕获值在代码片段中的类型引用。
     */
    val typeRef: CfirTypeRef,
)

/**
 * 以 CFIR symbol 作为捕获值去重键。
 */
@CaImplementationDetail
data class CodeFragmentCapturedId(
    /**
     * 唯一标识被捕获声明的 CFIR symbol。
     */
    val symbol: CfirBasedSymbol<*>,
)

/**
 * inline lambda 参数实际传入表达式及其捕获深度回滚信息。
 */
@CaImplementationDetail
class InlineLambdaArgument(
    /**
     * inline lambda 参数对应的实际实参表达式。
     */
    val expr: CfirExpression,
    /**
     * 访问该实参表达式时需要恢复到的捕获深度。
     */
    val stackRollbackDepth: Int,
)

/**
 * 分析 code fragment 中对外部局部值、this/super 和相关源码文件的捕获关系。
 */
@CaImplementationDetail
@OptIn(CaExperimentalApi::class)
object CodeFragmentCapturedValueAnalyzer {
    /**
     * 对给定 code fragment 执行捕获值分析。
     */
    fun analyze(
        resolutionFacade: LLResolutionFacade,
        codeFragment: CfirCodeFragment,
        inlineLambdaArgumentsToDepth: Map<CfirValueParameterSymbol, InlineLambdaArgument>,
    ): CodeFragmentCapturedValueData {
        val selfSymbols = CodeFragmentDeclarationCollector().apply {
            codeFragment.accept(this)
            inlineLambdaArgumentsToDepth.values.map { it.expr }.forEach { inlineLambdaArgumentExpr ->
                inlineLambdaArgumentExpr.accept(this)
            }
        }.symbols.toSet()
        val capturedVisitor = CodeFragmentCapturedValueVisitor(resolutionFacade, selfSymbols, inlineLambdaArgumentsToDepth)
        codeFragment.accept(capturedVisitor)
        return CodeFragmentCapturedValueData(capturedVisitor.values, capturedVisitor.files, selfSymbols)
    }
}


/**
 * code fragment 捕获分析的完整结果。
 */
@OptIn(CaExperimentalApi::class)
class CodeFragmentCapturedValueData(
    /**
     * 捕获到的外部值或 this/super 符号列表。
     */
    val symbols: List<CodeFragmentCapturedSymbol>,
    /**
     * 捕获本地函数或属性时需要一起提交的源码文件。
     */
    val files: List<CjFile>,
    /**
     * code fragment 自身声明的 symbol 集合，用于排除内部声明。
     */
    val selfSymbols: Set<CfirBasedSymbol<*>>,
)

/**
 * 收集 code fragment 自身声明的 CFIR symbol。
 */
private class CodeFragmentDeclarationCollector : CfirDefaultVisitorVoid() {
    /**
     * 遍历期间收集到的声明 symbol。
     */
    private val collectedSymbols = mutableListOf<CfirBasedSymbol<*>>()

    /**
     * 已收集 symbol 的只读视图。
     */
    val symbols: List<CfirBasedSymbol<*>>
        get() = Collections.unmodifiableList(collectedSymbols)

    /**
     * 记录当前声明节点 symbol，并继续访问子节点。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirDeclaration) {
            collectedSymbols += element.symbol
        }

        element.acceptChildren(this)
    }
}

/**
 * 遍历 code fragment CFIR 树并识别所有外部捕获值。
 */
@OptIn(CaExperimentalApi::class)
private class CodeFragmentCapturedValueVisitor(
    /**
     * 捕获分析所在 use-site 的 low-level resolution facade。
     */
    private val resolutionFacade: LLResolutionFacade,
    /**
     * code fragment 与 inline 实参内部自声明的 symbol 集合。
     */
    private val selfSymbols: Set<CfirBasedSymbol<*>>,
    /**
     * inline lambda value parameter 到实际实参表达式的映射。
     */
    private val inlineLambdaParametersMapping: Map<CfirValueParameterSymbol, InlineLambdaArgument>,
) : CfirDefaultVisitorVoid() {
    /**
     * 按 symbol 去重后的捕获值映射。
     */
    private val collectedMappings = LinkedHashMap<CodeFragmentCapturedId, CodeFragmentCapturedSymbol>()

    /**
     * 捕获本地 callable 时需要额外提交的源码文件集合。
     */
    private val collectedFiles = LinkedHashSet<CjFile>()

    /**
     * 当前遍历栈上处于赋值左侧的 symbol。
     */
    private val assignmentLhs = mutableListOf<CfirBasedSymbol<*>>()

    /**
     * 捕获值列表，保持首次发现顺序并合并 mutation 信息。
     */
    val values: List<CodeFragmentCapturedSymbol>
        get() = collectedMappings.values.toList()

    /**
     * 需要随 code fragment 一起处理的源码文件列表。
     */
    val files: List<CjFile>
        get() = collectedFiles.toList()

    /**
     * 当前捕获分析使用的 CFIR session。
     */
    private val session: CfirSession
        get() = resolutionFacade.useSiteCfirSession

    /**
     * 当前捕获值跨越 inline lambda 边界的深度。
     */
    private var depth = 0

    /**
     * 访问元素并处理 inline lambda 实参替换、赋值左侧标记和子节点递归。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirQualifiedAccessExpression) {
            val symbol = element.resolvedCallableSymbolFromExpressionOrNull() as? CfirValueParameterSymbol
            val inlineLambdaArgument = inlineLambdaParametersMapping[symbol]
            if (inlineLambdaArgument != null) {
                val oldDepth = depth
                depth = inlineLambdaArgument.stackRollbackDepth
                visitElement(inlineLambdaArgument.expr)
                depth = oldDepth
                return
            }
        }

        processElement(element)

        val lhs = (element as? CfirAssignment)?.lValue?.resolvedCallableSymbolFromExpressionOrNull()
        if (lhs != null) {
            assignmentLhs.add(lhs)
        }

        element.acceptChildren(this)

        if (lhs != null) {
            require(assignmentLhs.removeLast() == lhs)
        }
    }

    /**
     * 对单个 CFIR 元素识别 this/super/callable 引用等捕获来源。
     */
    private fun processElement(element: CfirElement) {
        if (element is CfirExpression) {
            element.resolvedType.toClassSymbol(session)?.let(::registerFileIfRequired)
        }

        when (element) {
            is CfirSuperReference -> {
                val symbol = (element.superTypeRef as? CfirResolvedTypeRef)?.coneType?.toClassSymbol(session)
                if (symbol != null && symbol !in selfSymbols) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = CaCodeFragmentCapturedValue.SuperClass(symbol.classId, isCrossingInlineBounds, depth)
                    register(CodeFragmentCapturedSymbol(capturedValue, symbol, element.superTypeRef))
                }
            }
            is CfirThisReference -> {
                val symbol = element.boundSymbol
                if (symbol != null && (symbol as CfirBasedSymbol<*>?) !in selfSymbols) {
                    fun registerContainingClass(classSymbol: CfirClassLikeSymbol<*>) {
                        val isCrossingInlineBounds = isCrossingInlineBounds(element, classSymbol)
                        val capturedValue = CaCodeFragmentCapturedValue.ContainingClass(classSymbol.classId, isCrossingInlineBounds, depth)
                        val typeRef = buildResolvedTypeRef { coneType = classSymbol.defaultType() }
                        register(CodeFragmentCapturedSymbol(capturedValue, classSymbol, typeRef))
                    }

                    when (symbol) {
                        is CfirClassLikeSymbol<*> -> registerContainingClass(symbol)
                        is CfirExtendSymbol -> {
                            // 仓颉没有 Kotlin 式 extension receiver captured value，
                            // `extend` 中的 `this` 只能收口到其真实扩展目标类型。
                            symbol.cfir.extendedTypeRef.coneTypeOrNull
                                ?.toClassSymbol(session)
                                ?.let(::registerContainingClass)
                        }
                        is CfirTypeAliasSymbol, is CfirTypeParameterSymbol -> errorWithCfirSpecificEntries(
                            message = "Unexpected CfirThisOwnerSymbol ${symbol::class.simpleName}", cfir = symbol.cfir
                        )
                    }
                }
            }
            is CfirResolvable -> {
                val symbol = element.resolvedCallableSymbolOrNull()
                if (symbol != null && symbol !in selfSymbols) {
                    processCall(element, symbol)
                }
            }
        }
    }

    /**
     * 根据已解析 callable symbol 识别局部值、foreign value 或需要登记源码文件的本地函数。
     */
    private fun processCall(element: CfirElement, symbol: CfirCallableSymbol<*>) {
        val isMutated = assignmentLhs.lastOrNull() == symbol
        when (symbol) {
            is CfirValueParameterSymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CaCodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds, depth)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is CfirPropertySymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = when {
                    symbol.isForeignValue -> CaCodeFragmentCapturedValue.ForeignValue(symbol.name, isCrossingInlineBounds, depth)
                    else -> CaCodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds, depth)
                }
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is CfirNamedFunctionSymbol -> {
                registerFileIfRequired(symbol)
            }
            else -> Unit
        }
    }

    /**
     * 登记捕获值；同一 symbol 重复出现时只用 mutation 信息升级已有记录。
     */
    private fun register(mapping: CodeFragmentCapturedSymbol) {
        val id = CodeFragmentCapturedId(mapping.symbol)
        val previousMapping = collectedMappings[id]

        if (previousMapping != null) {
            val previousValue = previousMapping.value
            val newValue = mapping.value

            require(previousValue.javaClass == newValue.javaClass)

            // Only replace non-mutated value with a mutated one.
            if (previousValue.isMutated || !newValue.isMutated) {
                return
            }
        }

        collectedMappings[id] = mapping
        registerFileIfRequired(mapping.symbol)
    }

    /**
     * 如果捕获 symbol 属于源码本地 callable，则登记其所在源码文件。
     */
    private fun registerFileIfRequired(symbol: CfirBasedSymbol<*>) {
        val needsRegistration = when (symbol) {
            is CfirNamedFunctionSymbol -> symbol.cfir.isLocal
            is CfirPropertySymbol -> symbol.cfir.isLocal
            else -> false
        }

        if (!needsRegistration) {
            return
        }

        val file = symbol.cfir.containingCjFileIfAny ?: return
        if (!file.isCompiled) {
            collectedFiles.add(file)
        }
    }

    /**
     * 仓颉主干当前没有 Kotlin FIR 对位的 inline 边界语义，
     * 因而代码片段捕获分析不会再伪造 crossing-inline-bounds 结果。
     */
    private fun isCrossingInlineBounds(element: CfirElement, symbol: CfirBasedSymbol<*>): Boolean = false
}

/**
 * 从 resolvable 的 callee reference 中取出已解析 callable symbol。
 */
private fun CfirResolvable.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
    (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>

/**
 * 当表达式本身可解析时返回其 callable symbol。
 */
private fun CfirExpression.resolvedCallableSymbolFromExpressionOrNull(): CfirCallableSymbol<*>? =
    (this as? CfirResolvable)?.resolvedCallableSymbolOrNull()

/**
 * 构造 class-like symbol 的默认类型，用于 this/super 捕获值类型引用。
 */
private fun CfirClassLikeSymbol<*>.defaultType() =
    toLookupTag().constructClassType()
