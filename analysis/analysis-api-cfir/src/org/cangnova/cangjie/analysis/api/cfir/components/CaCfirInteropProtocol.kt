package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.interop.CaInteropBackend
import org.cangnova.cangjie.analysis.api.interop.CaInteropCallingConvention
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CallingConvention
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjBuiltInAnnotation
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.psiUtil.collectAnnotationEntriesFromStubOrPsi
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.isPlain

/**
 * CFIR 后端的互操作公开快照实现。
 *
 * 互操作信息来源于源码声明上的修饰符与内建 FFI 注解，
 * 因此这里统一落在 session 缓存里，而不是让不同组件重复扫描 PSI。
 */
internal class CaCfirInteropInfoImpl(
    override val backends: List<CaInteropBackend>,
    override val isForeignDeclaration: Boolean,
    override val isFastNative: Boolean,
    override val externalName: String?,
    override val callingConvention: CaInteropCallingConvention?,
    override val ffiAnnotationNames: List<String>,
    override val token: CaLifetimeToken,
) : CaInteropInfo

internal fun CaCfirSession.getInteropInfo(element: CjElement): CaInteropInfo? {
    return getOrCreateInteropInfo(element) {
        resolveInteropOwner(element)?.let(::buildInteropInfo)
    }
}

internal fun CaCfirSession.getInteropInfo(symbol: CaSymbol): CaInteropInfo? {
    val key = symbol.publicSymbolCacheKeyOrNull() ?: return null
    return getOrCreateSymbolInteropInfo(key) {
        val sourcePsi = when (symbol) {
            is CaCfirBackedSymbol<*> -> lookupSourcePsi(symbol.backingSymbol)
            else -> null
        } ?: return@getOrCreateSymbolInteropInfo null
        val owner = resolveInteropOwner(sourcePsi as? CjElement ?: return@getOrCreateSymbolInteropInfo null)
            ?: return@getOrCreateSymbolInteropInfo null
        buildInteropInfo(owner)
    }
}

/**
 * 统一确定某个源码元素归属的互操作声明边界。
 *
 * 互操作信息只对带修饰符/注解的声明边界稳定成立，因此这里总是回收到最近的
 * [CjModifierListOwner]，并要求它同时具备注解容器语义。
 */
private fun resolveInteropOwner(element: CjElement): CjModifierListOwner? {
    return when (element) {
        is CjModifierListOwner -> element
        else -> element.getStrictParentOfType<CjModifierListOwner>()
    }
}

/**
 * 从源码声明构建稳定的互操作快照。
 */
private fun CaCfirSession.buildInteropInfo(owner: CjModifierListOwner): CaInteropInfo? {
    val annotated = owner as? CjAnnotated ?: return null
    val ffiAnnotations = annotated.collectAnnotationEntriesFromStubOrPsi()
        .filter(CjAnnotation::isFFIAnnotation)

    val backends = ffiAnnotations.mapNotNull { annotation ->
        when (annotation.builtInAnnotation) {
            CjBuiltInAnnotation.C -> CaInteropBackend.C
            CjBuiltInAnnotation.JAVA -> CaInteropBackend.JAVA
            CjBuiltInAnnotation.JAVA_MIRROR -> CaInteropBackend.JAVA_MIRROR
            CjBuiltInAnnotation.JAVA_IMPL -> CaInteropBackend.JAVA_IMPL
            CjBuiltInAnnotation.OBJ_C_MIRROR -> CaInteropBackend.OBJC_MIRROR
            CjBuiltInAnnotation.OBJ_C_IMPL -> CaInteropBackend.OBJC_IMPL
            else -> null
        }
    }.distinct()

    val isForeignDeclaration = owner.hasModifier(CjTokens.FOREIGN_KEYWORD)
    val isFastNative = ffiAnnotations.any { annotation ->
        annotation.builtInAnnotation == CjBuiltInAnnotation.FAST_NATIVE
    } || annotated.collectAnnotationEntriesFromStubOrPsi().any { annotation ->
        annotation.builtInAnnotation == CjBuiltInAnnotation.FAST_NATIVE
    }
    val externalName = ffiAnnotations
        .firstOrNull { annotation -> annotation.builtInAnnotation == CjBuiltInAnnotation.FOREIGN_NAME }
        ?.extractForeignName()
    val callingConvention = ffiAnnotations
        .firstNotNullOfOrNull { annotation -> annotation.callingConvention?.asPublicCallingConvention() }
    val ffiAnnotationNames = ffiAnnotations.mapNotNull { annotation ->
        annotation.shortName?.asString()
    }.distinct()

    if (!isForeignDeclaration && !isFastNative && backends.isEmpty() && externalName == null && callingConvention == null) {
        return null
    }

    return CaCfirInteropInfoImpl(
        backends = backends,
        isForeignDeclaration = isForeignDeclaration,
        isFastNative = isFastNative,
        externalName = externalName,
        callingConvention = callingConvention,
        ffiAnnotationNames = ffiAnnotationNames,
        token = token,
    )
}

private fun CjAnnotation.extractForeignName(): String? {
    val rawExpression = valueArguments
        .firstOrNull { argument -> argument.getArgumentName()?.asName?.asString() == "name" }
        ?.getArgumentExpression()
        ?: valueArguments.firstOrNull()?.getArgumentExpression()
        ?: return null

    return when (rawExpression) {
        is CjStringTemplateExpression -> {
            if (!rawExpression.isPlain()) return null
            rawExpression.stringContent
        }

        else -> rawExpression.text.trim().trim('"', '\'').ifBlank { null }
    }
}

private fun CallingConvention.asPublicCallingConvention(): CaInteropCallingConvention = when (this) {
    CallingConvention.CDECL -> CaInteropCallingConvention.CDECL
    CallingConvention.STDCALL -> CaInteropCallingConvention.STDCALL
}
