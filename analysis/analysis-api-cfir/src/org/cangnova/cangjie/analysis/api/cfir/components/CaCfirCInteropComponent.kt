package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.backingPsiIfApplicable
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.interop.CaInteropBackend
import org.cangnova.cangjie.analysis.api.interop.CaInteropCallingConvention
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CallingConvention
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.CjBuiltInAnnotation
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.psiUtil.isPlain
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

/**
 * C / FFI 互操作信息组件。
 *
 * 对齐 Kotlin 的组件落位方式，不再额外引入 `Protocol` 层。
 * 互操作语义直接由组件文件内的私有 helper 承载，并统一复用 session 缓存。
 */
internal class CaCfirCInteropComponent(
    /**
     * 延迟取得当前 CFIR Analysis session，互操作信息查询复用其中的缓存和 token。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCInteropComponent {
    /**
     * 返回源码元素归属声明的 C/FFI 互操作信息。
     */
    override fun CjElement.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }

    /**
     * 返回符号底层源码声明的 C/FFI 互操作信息。
     */
    override fun CaSymbol.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }
}

/**
 * CFIR 后端的互操作公开快照实现。
 *
 * 互操作信息来源于源码声明上的修饰符与内建 FFI 注解，
 * 因此这里统一落在 session 缓存里，而不是让不同组件重复扫描 PSI。
 */
internal class CaCfirInteropInfoImpl(
    /**
     * 声明显式标记的互操作后端集合。
     */
    override val backends: List<CaInteropBackend>,
    /**
     * 声明是否带有 foreign 修饰符。
     */
    override val isForeignDeclaration: Boolean,
    /**
     * 声明是否带有 fast native 互操作标记。
     */
    override val isFastNative: Boolean,
    /**
     * 通过 ForeignName 等注解指定的外部符号名。
     */
    override val externalName: String?,
    /**
     * 互操作调用约定。
     */
    override val callingConvention: CaInteropCallingConvention?,
    /**
     * 参与互操作判定的 FFI 注解短名集合。
     */
    override val ffiAnnotationNames: List<String>,
    /**
     * 约束互操作快照生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaInteropInfo

/**
 * 从 PSI 元素查询其所属声明的互操作信息。
 */
internal fun CaCfirSession.getInteropInfo(element: CjElement): CaInteropInfo? {
    return resolveInteropOwner(element)?.let(::buildInteropInfo)
}

/**
 * 从公开符号查询底层源码声明的互操作信息。
 */
internal fun CaCfirSession.getInteropInfo(symbol: CaSymbol): CaInteropInfo? {
    val sourcePsi = when (symbol) {
        is CaCfirSymbol<*> -> symbol.cfirSymbol.backingPsiIfApplicable
        else -> null
    } ?: return null
    val owner = resolveInteropOwner(sourcePsi as? CjElement ?: return null)
        ?: return null
    return buildInteropInfo(owner)
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
    val allAnnotations = owner.collectInteropAnnotations()
    val ffiAnnotations = allAnnotations
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
    val isFastNative = allAnnotations.any { annotation ->
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

/**
 * 互操作注解必须绑定到声明边界本身，而不是依赖某一类 PSI 对注解容器的转发细节。
 *
 * 当前仓库里不同声明的注解既可能通过 `annotationEntries` 暴露，
 * 也可能以 `CjAnnotations` / `modifierList` 形式直接挂在声明前缀上。
 * 这里统一把这几条稳定入口合并，避免 `@C struct` 和 `@ForeignName foreign func`
 * 因为 PSI 落位差异而被分裂处理。
 */
private fun CjModifierListOwner.collectInteropAnnotations(): List<CjAnnotation> {
    val forwardedAnnotations = (this as? CjAnnotated)?.annotationEntries.orEmpty()
    val directContainerAnnotations = children
        .filterIsInstance<CjAnnotations>()
        .flatMap(CjAnnotations::entries)
    val directAnnotations = children.filterIsInstance<CjAnnotation>()
    val modifierListAnnotations = modifierList?.let { modifierList ->
        PsiTreeUtil.findChildrenOfType(modifierList, CjAnnotation::class.java).toList()
    }.orEmpty()
    val detachedSiblingAnnotations = generateSequence(prevSibling) { sibling -> sibling.prevSibling }
        .takeWhile { sibling ->
            sibling is PsiWhiteSpace || sibling is PsiComment || sibling is CjAnnotations || sibling is CjModifierList
        }
        .filterIsInstance<CjAnnotations>()
        .flatMap(CjAnnotations::entries)
        .toList()
        .asReversed()

    return buildList {
        // 对齐 raw CFIR builder 的 detached annotation 恢复：部分注解会落在声明前缀 sibling 上。
        addAll(detachedSiblingAnnotations)
        addAll(forwardedAnnotations)
        addAll(directContainerAnnotations)
        addAll(directAnnotations)
        addAll(modifierListAnnotations)
    }.distinctBy { annotation ->
        annotation.textRange to annotation.text
    }
}

/**
 * 从 ForeignName 风格注解中提取外部符号名。
 */
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

/**
 * 将 PSI 层调用约定转换为 Analysis API 调用约定枚举。
 */
private fun CallingConvention.asPublicCallingConvention(): CaInteropCallingConvention = when (this) {
    CallingConvention.CDECL -> CaInteropCallingConvention.CDECL
    CallingConvention.STDCALL -> CaInteropCallingConvention.STDCALL
}
