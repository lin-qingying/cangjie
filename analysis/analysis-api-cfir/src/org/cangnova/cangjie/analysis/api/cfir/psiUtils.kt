package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CJ_DIAGNOSTIC_CONVERTER
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnostic
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getParentOfTypes2
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal fun CjPsiDiagnostic.asCaDiagnostic(analysisSession: CaCfirSession): CaDiagnosticWithPsi<*> {
    return CJ_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as CjDiagnostic)
}

/**
 * 直接从 PSI 修饰符读取声明可见性。
 *
 * 该 helper 对齐 Kotlin `visibilityByModifiers`，只表达显式修饰符；
 * 仓颉默认可见性仍由调用方按 raw CFIR builder 的容器规则补齐。
 */
internal val CjDeclaration.visibilityByModifiers: Visibility?
    get() = when {
        hasModifier(CjTokens.PRIVATE_KEYWORD) -> Visibilities.Private
        hasModifier(CjTokens.INTERNAL_KEYWORD) -> Visibilities.Internal
        hasModifier(CjTokens.PROTECTED_KEYWORD) -> Visibilities.Protected
        hasModifier(CjTokens.PUBLIC_KEYWORD) -> Visibilities.Public
        else -> null
    }

/**
 * 直接从 PSI 修饰符读取声明模态。
 *
 * 只返回源码中显式声明的模态，避免 source renderer 为判断隐式修饰符强制恢复 CFIR。
 */
internal val CjDeclaration.caSymbolModalityByModifiers: CaSymbolModality?
    get() = when {
        hasModifier(CjTokens.ABSTRACT_KEYWORD) -> CaSymbolModality.ABSTRACT
        hasModifier(CjTokens.OPEN_KEYWORD) -> CaSymbolModality.OPEN
        this is CjTypeStatement && hasModifier(CjTokens.SEALED_KEYWORD) -> CaSymbolModality.SEALED
        else -> null
    }

internal val CjDeclaration.location: CaSymbolLocation
    get() {
        // Note: a declaration can be nested inside a modifier list (for example, in the case of dangling annotations or context parameters)
        val parent = getParentOfTypes2<CjDeclaration, CjModifierList>()

        if (this is CjTypeParameter) {
            return if (parent is CjTypeStatement) CaSymbolLocation.CLASS else CaSymbolLocation.LOCAL
        }

        return when (parent) {
            null -> CaSymbolLocation.TOP_LEVEL

            is CjTypeStatement -> CaSymbolLocation.CLASS

            is CjDeclarationWithBody,
            is CjDeclarationWithInitializer,
            is CjModifierList,
            is CjParameter,
                -> CaSymbolLocation.LOCAL

            else -> errorWithAttachment("Unexpected parent declaration: ${parent::class.simpleName}") {
                withPsiEntry("parentDeclaration", parent)
                withPsiEntry("psi", this@location)
            }
        }
    }

/**
 * 按 raw CFIR builder 的规则从 PSI 推导 callable 可见性。
 *
 * Kotlin FIR 中该职责位于 `KaFirPsiSymbol.psiBasedVisibility`；仓颉默认可见性
 * 与 Kotlin 不同：局部声明为 local，接口成员默认为 public，其余默认为 internal。
 */
internal fun CjCallableDeclaration.psiBasedVisibility(isOverride: () -> Boolean): Visibility? = when (this) {
    is CjNamedFunction if isLocal -> Visibilities.Local
    is CjProperty if isLocal -> Visibilities.Local
    else -> visibilityByModifiers ?: when {
        containingTypeStatement?.isInterface() == true -> Visibilities.Public
        else -> Visibilities.Internal
    }
}

/**
 * 按 PSI 结构推导 callable 的默认模态。
 *
 * 这里保持 Kotlin `psiBasedDefaultKaModality` 的层级：顶层/局部为 final，
 * 接口成员按是否有实现区分 abstract/open，override 成员保留 open 语义。
 */
internal fun CjCallableDeclaration.psiBasedDefaultCaModality(
    isOverride: () -> Boolean,
): CaSymbolModality {
    val containingTypeStatement = containingTypeStatement
    return when {
        containingTypeStatement == null -> CaSymbolModality.FINAL
        containingTypeStatement.isInterface() -> when {
            hasModifier(CjTokens.PRIVATE_KEYWORD) -> CaSymbolModality.FINAL
            this is CjNamedFunction && !hasBody() -> CaSymbolModality.ABSTRACT
            this is CjProperty && !hasBody() -> CaSymbolModality.ABSTRACT
            else -> CaSymbolModality.OPEN
        }

        isOverride() -> CaSymbolModality.OPEN
        else -> CaSymbolModality.FINAL
    }
}

context(callable: CjCallableDeclaration)
internal val CaSymbolModality.isOpenFromInterface: Boolean
    get() = this == CaSymbolModality.OPEN && callable.containingTypeStatement?.isInterface() == true

internal fun CaCfirSymbol<*>.findPsi(): PsiElement? {
    return cfirSymbol.findPsi(analysisSession.analysisScope)
}
fun CfirBasedSymbol<*>.findPsi(scope: GlobalSearchScope): PsiElement? {
    val declaration = if (this is CfirCallableSymbol<*>) {
        cfir.unwrapFakeOverridesOrDelegated()
    } else {
        cfir
    }

    return declaration.realPsi?.takeIf { psi -> scope.contains(psi.containingFile.virtualFile) }
}


/**
 * Finds [PsiElement] which will be used as go-to referenced element for [KtPsiReference]
 * For data classes & enums generated members like `copy` `componentN`, `values` it will return corresponding enum/data class
 * Otherwise, behaves the same way as [findPsi] returns exact PSI declaration corresponding to passed [CfirDeclaration]
 */
internal fun CfirDeclaration.findReferencePsi(scope: GlobalSearchScope): PsiElement? {
    return if (
        this is CfirCallableDeclaration /*&&
        !this.symbol.isTypeAliasedConstructor*/ // typealiased constructors should not be unwrapped
    ) {
        unwrapFakeOverridesOrDelegated().psi
    } else {
        psi
    } /*?: CfirSyntheticFunctionInterfaceSourceProvider.findPsi(this, scope)*/
}
