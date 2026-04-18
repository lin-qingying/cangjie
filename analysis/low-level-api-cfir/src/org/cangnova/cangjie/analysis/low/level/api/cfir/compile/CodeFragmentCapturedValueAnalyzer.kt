/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjFakeSourceElementKind
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.compile.CaCodeFragmentCapturedValue
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.containingCjFileIfAny
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.parentsCodeFragmentAware
import org.cangnova.cangjie.descriptors.ClassKind
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.isInline
import org.cangnova.cangjie.cfir.declarations.utils.isLocal
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.extensions.captureValueInAnalyze
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirThisReference
import org.cangnova.cangjie.cfir.references.toResolvedCallableSymbol
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.*
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.forEachType
import org.cangnova.cangjie.cfir.types.resolvedType
import org.cangnova.cangjie.cfir.types.toRegularClassSymbol
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.StandardClassIds
import org.cangnova.cangjie.psi
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunction
import java.util.*

@CaImplementationDetail
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

@CaImplementationDetail
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
        if (element is CfirPropertyAccessExpression) {
            val symbol = element.toResolvedCallableSymbol() as? CfirValueParameterSymbol
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

        val lhs = (element as? CfirVariableAssignment)?.lValue?.toResolvedCallableSymbol(session)
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
            element.resolvedType.forEachType { type ->
                val symbol = type.toSymbol(session)
                if (symbol != null) {
                    registerFileIfRequired(symbol)
                }
            }
        }

        when (element) {
            is CfirSuperReference -> {
                val symbol = (element.superTypeRef as? CfirResolvedTypeRef)?.toRegularClassSymbol(session)
                if (symbol != null && symbol !in selfSymbols) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = CaCodeFragmentCapturedValue.SuperClass(symbol.classId, isCrossingInlineBounds, depth)
                    register(CodeFragmentCapturedSymbol(capturedValue, symbol, element.superTypeRef))
                }
            }
            is CfirThisReference -> {
                val symbol = element.boundSymbol
                if (symbol != null && (symbol as CfirBasedSymbol<*>?) !in selfSymbols) {
                    fun registerClassSymbolIfNotObject(classSymbol: CfirClassSymbol<*>) {
                        val isCrossingInlineBounds = isCrossingInlineBounds(element, classSymbol)
                        val capturedValue = CaCodeFragmentCapturedValue.ContainingClass(classSymbol.classId, isCrossingInlineBounds, depth)
                        val typeRef = buildResolvedTypeRef { coneType = classSymbol.defaultType() }
                        register(CodeFragmentCapturedSymbol(capturedValue, classSymbol, typeRef))
                    }

                    when (symbol) {
                        is CfirClassSymbol<*> -> {
                            registerClassSymbolIfNotObject(symbol)
                        }
                        is CfirReceiverParameterSymbol -> {
                            if (symbol.captureValueInAnalyze) {
                                val receiverParameter = symbol.fir
                                val labelName = element.labelName
                                    ?: (receiverParameter.containingDeclarationSymbol as? CfirAnonymousFunctionSymbol)?.label?.name
                                    ?: (receiverParameter.containingDeclarationSymbol as CfirCallableSymbol).name.asString()

                                val typeRef = receiverParameter.typeRef
                                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                val capturedValue = CaCodeFragmentCapturedValue.ExtensionReceiver(labelName, isCrossingInlineBounds, depth)
                                register(
                                    CodeFragmentCapturedSymbol(capturedValue, receiverParameter.symbol, typeRef)
                                )
                            }
                        }
                        is CfirTypeAliasSymbol, is CfirTypeParameterSymbol -> errorWithCfirSpecificEntries(
                            message = "Unexpected CfirThisOwnerSymbol ${symbol::class.simpleName}", fir = symbol.fir
                        )
                    }
                }
            }
            is CfirResolvable -> {
                val symbol = element.calleeReference.toResolvedCallableSymbol()
                if (symbol != null && symbol !in selfSymbols) {
                    processCall(element, symbol)
                }
            }
        }
    }

    private fun processCall(element: CfirElement, symbol: CfirCallableSymbol<*>) {
        // Desugared inc/dec CFIR looks as follows:
        // lval <unary>: R|kotlin/Int| = R|<local>/x|
        // R|<local>/x| = R|<local>/<unary>|.R|kotlin/Int.inc|()
        // We visit the x in the first line before we visit the assignment and need to check the source to determine that the variable
        // is mutated.
        // The x in the second line isn't visited because it's a CfirDesugaredAssignmentValueReferenceExpression.
        val isMutated = assignmentLhs.lastOrNull() == symbol || element.source?.kind is CjFakeSourceElementKind.DesugaredIncrementOrDecrement
        when (symbol) {
            is CfirValueParameterSymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CaCodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds, depth)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is CfirLocalPropertySymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = when {
                    symbol.isForeignValue -> CaCodeFragmentCapturedValue.ForeignValue(symbol.name, isCrossingInlineBounds, depth)
                    else -> CaCodeFragmentCapturedValue.Local(symbol.name, isMutated, isCrossingInlineBounds, depth)
                }
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is CfirRegularPropertySymbol -> {
                // Property call generation depends on complete backing field resolution (Cfir2IrLazyProperty.backingField)
                symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                registerFileIfRequired(symbol)
            }
            is CfirBackingFieldSymbol -> {
                val propertyName = symbol.propertySymbol.name
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CaCodeFragmentCapturedValue.BackingField(propertyName, isMutated, isCrossingInlineBounds, depth)
                register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is CfirNamedFunctionSymbol -> {
                registerFileIfRequired(symbol)
            }
        }

        if (symbol.callableId == StandardClassIds.Callables.coroutineContext) {
            val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
            val capturedValue = CaCodeFragmentCapturedValue.CoroutineContext(isCrossingInlineBounds, depth)
            register(CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
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

    private val CfirFunctionSymbol<*>.isAnnotatedWithNonLiteralJvmName: Boolean
        get() {
            val jvmNameAnnotation = annotations.getAnnotationByClassId(StandardClassIds.Annotations.jvmName, session) ?: return false

            lazyResolveToPhase(CfirResolvePhase.ANNOTATION_ARGUMENTS)

            val argument = jvmNameAnnotation.argumentMapping.mapping[Name.identifier("name")]
            return argument != null && argument !is CfirLiteralExpression
        }

    private val CfirFunctionSymbol<*>.hasAnnotationArgumentShouldBeEvaluated: Boolean
        get() {
            return isAnnotatedWithNonLiteralJvmName
        }

    private fun registerFileIfRequired(symbol: CfirBasedSymbol<*>) {
        val needsRegistration = when (symbol) {
            is CfirRegularClassSymbol -> symbol.isLocal
            is CfirAnonymousObjectSymbol -> true
            is CfirNamedFunctionSymbol -> symbol.isLocal || symbol.hasAnnotationArgumentShouldBeEvaluated
            is CfirPropertySymbol ->
                symbol.getterSymbol?.hasAnnotationArgumentShouldBeEvaluated == true
                        || symbol.setterSymbol?.hasAnnotationArgumentShouldBeEvaluated == true
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

    private fun isCrossingInlineBounds(element: CfirElement, symbol: CfirBasedSymbol<*>): Boolean {
        val callSite = element.source?.psi ?: return false
        val declarationSite = symbol.cfir.source?.psi ?: return false
        val commonParent = findCommonParentContextAware(callSite, declarationSite) ?: return false

        for (elementInBetween in callSite.parentsCodeFragmentAware) {
            if (elementInBetween === commonParent) {
                break
            }

            if (elementInBetween is CjFunction) {
                val symbolInBetween = elementInBetween.resolveToCfirSymbol(resolutionFacade)
                if (symbolInBetween is CfirCallableSymbol<*> && !symbolInBetween.isInline) {
                    return true
                }
            }
        }

        return false
    }

    private fun findCommonParentContextAware(callSite: PsiElement, declarationSite: PsiElement): PsiElement? {
        val directParent = PsiTreeUtil.findCommonParent(callSite, declarationSite)
        if (directParent != null) {
            return directParent
        }

        val codeFragment = callSite.containingFile as? CjCodeFragment ?: return null
        val codeFragmentContext = codeFragment.context ?: return null
        return PsiTreeUtil.findCommonParent(codeFragmentContext, declarationSite)
    }
}
