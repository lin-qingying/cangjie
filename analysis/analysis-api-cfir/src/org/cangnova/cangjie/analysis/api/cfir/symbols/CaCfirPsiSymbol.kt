package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.impl.base.symbols.pointers.CaBasePsiSymbolPointer
import org.cangnova.cangjie.analysis.api.impl.base.util.lazyPub
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirOfType
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.reflect.KClass

/**
 * A [CaCfirSymbol] that is possibly backed by some [PsiElement] and builds [cfirSymbol] lazily (by convention),
 * allowing some variables to be calculated without the need to build a [cfirSymbol].
 */
internal interface CaCfirPsiSymbol<out P : PsiElement, out S : CfirBasedSymbol<*>> : CaCfirSymbol<S> {
    /**
     * The [PsiElement] which can be used as a source of truth for some other property implementations.
     *
     * It can be as an element from a source file, or an element from a library.
     */
    val backingPsi: P?

    /**
     * The lazy implementation of [CfirBasedSymbol].
     *
     * The implementation is either built on top of [backingPsi] or provided during creation.
     *
     * @see cfirSymbol
     */
    val lazyCfirSymbol: Lazy<S>

    /**
     * The origin should be provided without using [cfirSymbol], if possible.
     */
    abstract override val origin: CaSymbolOrigin

    override val cfirSymbol: S get() = lazyCfirSymbol.value
}
@OptIn(ExperimentalContracts::class)
internal inline fun <R> CaCfirPsiSymbol<*, *>.ifNotLibrarySource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (analysisSession.useSiteModule is CaLibrarySourceModule) null else action()
}

@OptIn(ExperimentalContracts::class)
internal inline fun <R> CaCfirPsiSymbol<*, *>.ifSource(action: () -> R): R? {
    contract {
        callsInPlace(action, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
    }

    return if (origin == CaSymbolOrigin.SOURCE) action() else null
}
internal fun CaCfirCjBasedSymbol<CjCallableDeclaration, *>.createCaValueParameters(): List<CaValueParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.valueParameters?.map { it.symbol as CaValueParameterSymbol }
        }
    }
internal fun CaCfirCjBasedSymbol<CjTypeParameterListOwner, *>.createCaTypeParameters(): List<CaTypeParameterSymbol>? =
    ifNotLibrarySource {
        with(analysisSession) {
            backingPsi?.typeParameters?.map { it.symbol }
        }
    }
internal interface CaCfirCjBasedSymbol<out P : CjElement, out S : CfirBasedSymbol<*>> : CaCfirPsiSymbol<P, S> {
    override val origin: CaSymbolOrigin get() = withValidityAssertion { psiOrSymbolOrigin() }
}

internal inline fun <reified S : CfirBasedSymbol<*>> lazyCfirSymbol(
    declaration: CjDeclaration,
    session: CaCfirSession,
): Lazy<S> = lazyPub {
    declaration.resolveToCfirSymbolOfType<S>(session.resolutionFacade)
}

internal inline fun <reified E : CfirElement, reified S : CfirBasedSymbol<*>> lazyCfirSymbol(
    element: CjElement,
    session: CaCfirSession,
    crossinline symbol: (E) -> S,
): Lazy<S> = lazyPub {
    symbol(element.getOrBuildCfirOfType<E>(session.resolutionFacade))
}

internal fun CaCfirPsiSymbol<*, *>.psiOrSymbolHashCode(): Int = backingPsi?.hashCode() ?: cfirSymbol.hashCode()

internal fun CaCfirPsiSymbol<*, *>.psiOrSymbolEquals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other::class != this::class) return false

    val backingPsi = backingPsi
    val otherBackingPsi = (other as CaCfirPsiSymbol<*, *>).backingPsi
    return when {
        backingPsi == null && otherBackingPsi == null -> cfirSymbol == other.cfirSymbol
        backingPsi !== otherBackingPsi -> false
        backingPsi !is CjElement -> cfirSymbol == other.cfirSymbol
        !backingPsi.cameFromCangJieLibrary -> true
        else -> cfirSymbol == other.cfirSymbol
    }
}
/**
 * Currently, the compiled file can represent both library and non-library origin depending on the `preferBinary`
 * parameter from [org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLCfirSessionCache.getSession].
 *
 * So, depending on it, we may represent one decompiled file as [CaSymbolOrigin.SOURCE] and as [CaSymbolOrigin.LIBRARY]
 * at the same time.
 */
internal fun <P : CjElement> CaCfirPsiSymbol<P, *>.psiOrSymbolOrigin(): CaSymbolOrigin {
    val backingPsi = backingPsi
    return when {
        backingPsi == null -> symbolOrigin()
        backingPsi.cameFromCangJieLibrary -> symbolOrigin()
        else -> CaSymbolOrigin.SOURCE
    }
}
internal fun CaCfirCjBasedSymbol<CjAnnotated, *>.psiOrSymbolAnnotationList(): CaAnnotationList {
    return CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder)
}
internal val CjElement.cameFromCangJieLibrary: Boolean get() = containingCjFile.isCompiled
internal val CfirBasedSymbol<*>.backingPsiIfApplicable: PsiElement?
    get() {
        if (origin == CfirDeclarationOrigin.Synthetic.TypeAliasConstructor) return null

        return cfir.realPsi
    }


internal fun CaCfirCjBasedSymbol<CjDeclarationWithBody, CfirCallableSymbol<*>>.createReturnType(): CaType {
    val backingPsi = backingPsi
    if (backingPsi?.hasBlockBody() == true && !backingPsi.hasDeclaredReturnType()) {
        return builder.typeBuilder.buildType(analysisSession.cfirSession.builtinTypes.unitType)
    }

    return cfirSymbol.returnType(builder)
}

/**
 * callable 的 override 标记在源码 PSI 可直接判定。
 *
 * 对齐 Kotlin `isOverrideWithWorkaround` 的职责边界：source PSI 不为普通状态位
 * 强制恢复 CFIR；没有 source PSI 时才读取 CFIR 状态。
 */
internal val CaCfirCjBasedSymbol<CjCallableDeclaration, CfirCallableSymbol<*>>.isOverrideWithWorkaround: Boolean
    get() {
        val sourcePsi = ifSource { backingPsi }
        return sourcePsi?.hasModifier(CjTokens.OVERRIDE_KEYWORD) ?: cfirSymbol.rawStatus.isOverride
    }

internal inline fun <reified S : CaSymbol> CaCfirPsiSymbol<out CjElement, *>.psiBasedSymbolPointerOfTypeIfSource(
    noinline restoreSymbolByPsi: org.cangnova.cangjie.analysis.api.CaSession.(CjElement) -> CaSymbol?,
): CaSymbolPointer<S>? {
    return psiBasedSymbolPointerOfTypeIfSource(S::class, restoreSymbolByPsi)
}

@OptIn(CaImplementationDetail::class)
internal fun <S : CaSymbol> CaCfirPsiSymbol<out CjElement, *>.psiBasedSymbolPointerOfTypeIfSource(
    expectedClass: KClass<S>,
    restoreSymbolByPsi: org.cangnova.cangjie.analysis.api.CaSession.(CjElement) -> CaSymbol?,
): CaSymbolPointer<S>? {
    val symbol = this as? S ?: return null
    return ifSource {
        CaBasePsiSymbolPointer.createForSymbolFromSource(
            symbol = symbol,
            expectedClass = expectedClass,
            restoreSymbolByPsi = restoreSymbolByPsi,
        )
    }
}
