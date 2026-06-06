

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

@CaImplementationDetail
@OptIn(CaExperimentalApi::class)
class CodeFragmentCapturedSymbol(
    val value: CaCodeFragmentCapturedValue,
    val symbol: CfirBasedSymbol<*>,
    val typeRef: CfirTypeRef,
)

@CaImplementationDetail
data class CodeFragmentCapturedId(val symbol: CfirBasedSymbol<*>)

@CaImplementationDetail
class InlineLambdaArgument(val expr: CfirExpression, val stackRollbackDepth: Int)

@CaImplementationDetail
@OptIn(CaExperimentalApi::class)
object CodeFragmentCapturedValueAnalyzer {
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


@OptIn(CaExperimentalApi::class)
class CodeFragmentCapturedValueData(
    val symbols: List<CodeFragmentCapturedSymbol>,
    val files: List<CjFile>,
    val selfSymbols: Set<CfirBasedSymbol<*>>,
)

private class CodeFragmentDeclarationCollector : CfirDefaultVisitorVoid() {
    private val collectedSymbols = mutableListOf<CfirBasedSymbol<*>>()

    val symbols: List<CfirBasedSymbol<*>>
        get() = Collections.unmodifiableList(collectedSymbols)

    override fun visitElement(element: CfirElement) {
        if (element is CfirDeclaration) {
            collectedSymbols += element.symbol
        }

        element.acceptChildren(this)
    }
}

@OptIn(CaExperimentalApi::class)
private class CodeFragmentCapturedValueVisitor(
    private val resolutionFacade: LLResolutionFacade,
    private val selfSymbols: Set<CfirBasedSymbol<*>>,
    private val inlineLambdaParametersMapping: Map<CfirValueParameterSymbol, InlineLambdaArgument>,
) : CfirDefaultVisitorVoid() {
    private val collectedMappings = LinkedHashMap<CodeFragmentCapturedId, CodeFragmentCapturedSymbol>()
    private val collectedFiles = LinkedHashSet<CjFile>()

    private val assignmentLhs = mutableListOf<CfirBasedSymbol<*>>()

    val values: List<CodeFragmentCapturedSymbol>
        get() = collectedMappings.values.toList()

    val files: List<CjFile>
        get() = collectedFiles.toList()

    private val session: CfirSession
        get() = resolutionFacade.useSiteCfirSession

    private var depth = 0

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

private fun CfirResolvable.resolvedCallableSymbolOrNull(): CfirCallableSymbol<*>? =
    (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>

private fun CfirExpression.resolvedCallableSymbolFromExpressionOrNull(): CfirCallableSymbol<*>? =
    (this as? CfirResolvable)?.resolvedCallableSymbolOrNull()

private fun CfirClassLikeSymbol<*>.defaultType() =
    toLookupTag().constructClassType()
