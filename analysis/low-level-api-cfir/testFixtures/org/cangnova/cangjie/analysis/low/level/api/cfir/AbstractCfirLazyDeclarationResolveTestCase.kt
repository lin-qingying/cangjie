package org.cangnova.cangjie.analysis.low.level.api.cfir

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.renderer.CfirResolvePhaseRenderer
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseWithCallableMembers
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `AbstractFirLazyDeclarationResolveTestCase` 的 low-level CFIR 基类。
 *
 * 这里只保留仓颉当前真实存在、且已经有 low-level API 对位的能力：
 * 1. 通过 caret 选中 declaration；
 * 2. 按需切换 property accessor；
 * 3. 支持 regular / recursive / withCallableMembers 三种 lazy resolve 入口；
 * 4. 允许从 class-like 声明继续选定具体 member。
 */
abstract class AbstractCfirLazyDeclarationResolveTestCase : AbstractAnalysisApiBasedTest() {
    protected fun findCfirDeclarationToResolve(
        cjFile: CjFile,
        testServices: TestServices,
        resolutionFacade: LLResolutionFacade,
        fileWithCaret: CjFile = cjFile,
    ): Pair<CfirElementWithResolveState, (CfirResolvePhase) -> Unit> {
        if (Directives.RESOLVE_FILE in testServices.allDirectivesForAnalysisTest()) {
            val cfirFile = resolutionFacade.getOrBuildCfirFile(cjFile)
            return cfirFile to { phase ->
                cfirFile.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }

        val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjDeclaration>(fileWithCaret).let {
            if (cjFile === fileWithCaret) {
                it
            } else {
                PsiTreeUtil.findSameElementInCopy(it, cjFile)
            }
        }

        val symbol = chooseMemberDeclarationIfNeeded(
            symbol = declaration.resolveToCfirSymbol(resolutionFacade),
            resolutionFacade = resolutionFacade,
            testServices = testServices,
        )
        val cfirDeclaration = symbol.cfir as? CfirElementWithResolveState
            ?: error("Selected symbol `${symbol::class.simpleName}` is not backed by a resolve-state CFIR declaration.")
        return cfirDeclaration to { phase ->
            cfirDeclaration.lazyResolveToPhaseByDirective(phase, testServices)
        }
    }

    private fun chooseMemberDeclarationIfNeeded(
        symbol: CfirBasedSymbol<*>,
        resolutionFacade: LLResolutionFacade,
        testServices: TestServices,
    ): CfirBasedSymbol<*> {
        val directives = testServices.allDirectivesForAnalysisTest()
        val memberName = directives.singleOrZeroValue(Directives.MEMBER_NAME_FILTER)
        val memberSymbolClass = directives.singleOrZeroValue(Directives.MEMBER_SYMBOL_CLASS)
        if (memberName == null && memberSymbolClass == null && Directives.RESOLVE_PROPERTY_PART !in directives) {
            return symbol
        }

        val memberSymbol = when (symbol) {
            is CfirClassLikeSymbol<*> -> chooseClassLikeMember(symbol, resolutionFacade, memberName, memberSymbolClass)
            else -> symbol
        }

        val propertyPart = directives.singleOrZeroValue(Directives.RESOLVE_PROPERTY_PART) ?: return memberSymbol
        val propertySymbol = memberSymbol as? CfirPropertySymbol
            ?: error("`RESOLVE_PROPERTY_PART` 只能作用在 property symbol 上，当前是 `${memberSymbol::class.simpleName}`。")

        return when (propertyPart) {
            PropertyPart.GETTER -> propertySymbol.getterSymbol
            PropertyPart.SETTER -> propertySymbol.setterSymbol
        } ?: error("Property `${propertySymbol.callableId}` 不存在请求的 accessor：$propertyPart")
    }

    private fun chooseClassLikeMember(
        classLikeSymbol: CfirClassLikeSymbol<*>,
        resolutionFacade: LLResolutionFacade,
        memberName: String?,
        memberSymbolClass: String?,
    ): CfirBasedSymbol<*> {
        val classLikeDeclaration = classLikeSymbol.cfir as? CfirClassLikeDeclaration
            ?: return classLikeSymbol
        val scope = classLikeDeclaration.unsubstitutedScope(
            useSiteSession = resolutionFacade.useSiteCfirSession,
            scopeSession = resolutionFacade.getScopeSessionFor(resolutionFacade.useSiteCfirSession),
            withForcedTypeCalculator = false,
            memberRequiredPhase = CfirResolvePhase.STATUS,
        )

        val candidates = linkedSetOf<CfirCallableSymbol<*>>()
        scope.getCallableNames().sortedBy(Name::asString).forEach { name ->
            scope.processFunctionsByName(name) { function ->
                if (matches(function, memberName, memberSymbolClass)) {
                    candidates += function
                }
            }
            scope.processPropertiesByName(name) { property ->
                if (matches(property, memberName, memberSymbolClass)) {
                    candidates += property
                }
            }
        }
        scope.processDeclaredConstructors { constructor ->
            if (matches(constructor, memberName, memberSymbolClass)) {
                candidates += constructor
            }
        }

        return when (candidates.size) {
            0 -> error(
                buildString {
                    append("在 `${classLikeSymbol.classId.asString()}` 上没有找到匹配 member")
                    if (memberName != null) append(" name=`$memberName`")
                    if (memberSymbolClass != null) append(" symbolClass=`$memberSymbolClass`")
                },
            )
            1 -> candidates.single()
            else -> error(
                "member 选择结果不唯一：\n" + candidates.joinToString(separator = "\n") { candidate ->
                    "${candidate::class.simpleName}: ${candidate.callableId}"
                },
            )
        }
    }

    private fun matches(
        symbol: CfirCallableSymbol<*>,
        memberName: String?,
        memberSymbolClass: String?,
    ): Boolean {
        if (memberName != null && symbol.name.asString() != memberName) return false
        if (memberSymbolClass != null && symbol::class.simpleName != memberSymbolClass) return false
        return true
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    protected enum class LazyResolveMode {
        Regular,
        Recursive,
        WithCallableMembers,
    }

    protected enum class PropertyPart {
        GETTER,
        SETTER,
    }

    protected fun CfirElementWithResolveState.lazyResolveToPhaseByDirective(
        targetPhase: CfirResolvePhase,
        testServices: TestServices,
    ) {
        when (testServices.allDirectivesForAnalysisTest().singleOrZeroValue(Directives.LAZY_MODE)) {
            null, LazyResolveMode.Regular -> lazyResolveToPhase(targetPhase)
            LazyResolveMode.Recursive -> lazyResolveToPhaseRecursively(targetPhase)
            LazyResolveMode.WithCallableMembers -> {
                val cfirClass = this as? CfirClass
                    ?: error("`WITH_CALLABLE_MEMBERS` 只能作用在 class-like declaration 上，当前是 `${this::class.simpleName}`。")
                cfirClass.lazyResolveToPhaseWithCallableMembers(targetPhase)
            }
        }
    }

    protected object Directives : SimpleDirectivesContainer() {
        val MEMBER_NAME_FILTER by stringDirective("在 class-like declaration 内选择指定成员名。")
        val MEMBER_SYMBOL_CLASS by stringDirective("在 class-like declaration 内选择指定 symbol class simpleName。")
        val RESOLVE_PROPERTY_PART by enumDirective<PropertyPart>("将 property 目标切换到 getter / setter。")
        val RESOLVE_FILE by directive("直接对整个 CfirFile 执行 lazy resolve。")
        val LAZY_MODE by enumDirective<LazyResolveMode>("指定 lazy resolve 入口：regular / recursive / withCallableMembers。")
    }
}

private fun TestServices.allDirectivesForAnalysisTest() = cjTestModuleStructure.testModuleStructure.allDirectives

internal fun renderCfirWithResolvePhases(element: CfirElement): String {
    return CfirRenderer(
        resolvePhaseRenderer = CfirResolvePhaseRenderer(),
    ).renderElementAsString(element)
}

internal fun collectCallableSymbols(scope: CfirTypeScope): List<CfirCallableSymbol<*>> {
    val result = linkedSetOf<CfirCallableSymbol<*>>()
    scope.getCallableNames().sortedBy(Name::asString).forEach { name ->
        scope.processFunctionsByName(name) { result += it }
        scope.processPropertiesByName(name) { result += it }
    }
    scope.processDeclaredConstructors { result += it }
    return result.toList()
}

internal fun collectDirectOverriddenCallables(
    symbol: CfirCallableSymbol<*>,
    scope: CfirTypeScope,
): List<Pair<CfirCallableSymbol<*>, CfirTypeScope>> {
    val result = mutableListOf<Pair<CfirCallableSymbol<*>, CfirTypeScope>>()
    when (symbol) {
        is CfirNamedFunctionSymbol -> {
            scope.processDirectOverriddenFunctionsWithBaseScope(symbol) { overridden, baseScope ->
                result += overridden to baseScope
                ProcessorAction.NEXT
            }
        }
        is CfirPropertySymbol -> {
            scope.processDirectOverriddenPropertiesWithBaseScope(symbol) { overridden, baseScope ->
                result += overridden to baseScope
                ProcessorAction.NEXT
            }
        }
        // 仓颉 callable 族比 Kotlin 当前覆盖面更宽，只有 function / property 具备这组 override API。
        else -> {}
    }
    return result
}
