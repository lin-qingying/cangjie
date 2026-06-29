package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR public symbol 缓存键协议。
 *
 * 这里单独承载稳定身份定义，不再和 symbol 构造逻辑混放在同一个大文件里。
 * 构造由 `CaSymbolByCfirBuilder` 负责，缓存恢复则消费这些键。
 */
internal sealed interface CaCfirPublicSymbolCacheKey

/**
 * 包符号的稳定缓存键。
 */
internal data class CaCfirPackageSymbolCacheKey(
    /**
     * 包的完全限定名。
     */
    val fqName: FqName,
) : CaCfirPublicSymbolCacheKey

/**
 * 文件符号的稳定缓存键。
 */
internal data class CaCfirFileSymbolCacheKey(
    /**
     * 文件符号对应的仓颉 PSI 文件。
     */
    val file: CjFile,
) : CaCfirPublicSymbolCacheKey

/**
 * class-like 符号的稳定缓存键。
 */
internal data class CaCfirClassLikeSymbolCacheKey(
    /**
     * class、interface、struct、enum 或 typealias 的 classId。
     */
    val classId: ClassId,
) : CaCfirPublicSymbolCacheKey

/**
 * extend 符号的稳定缓存键。
 */
internal data class CaCfirExtendSymbolCacheKey(
    /** `extend` 的稳定语义身份，供缓存与 pointer 恢复使用。 */
    val identity: CaCfirExtendSymbolIdentity,
) : CaCfirPublicSymbolCacheKey

/**
 * 顶层 callable 符号的稳定缓存键。
 */
internal data class CaCfirCallableSymbolCacheKey(
    /**
     * callable 的包级或成员级 callableId。
     */
    val callableId: CallableId,
    /**
     * callable 的具体公开符号种类。
     */
    val kind: CaCfirCallableSymbolKind,
) : CaCfirPublicSymbolCacheKey

/**
 * extend 成员 callable 符号的稳定缓存键。
 */
internal data class CaCfirExtendMemberCallableSymbolCacheKey(
    /** owner extend 的稳定语义身份，而不是公开 `extendId` 文本。 */
    val extendIdentity: CaCfirExtendSymbolIdentity,
    /**
     * extend 成员 callable 的短名。
     */
    val callableName: Name,
    /**
     * extend 成员 callable 的公开符号种类。
     */
    val kind: CaCfirCallableSymbolKind,
) : CaCfirPublicSymbolCacheKey

/**
 * 属性访问器符号的稳定缓存键。
 */
internal data class CaCfirPropertyAccessorSymbolCacheKey(
    /**
     * 所属属性的稳定缓存键。
     */
    val ownerKey: CaCfirPublicSymbolCacheKey,
    /**
     * getter 或 setter 访问器种类。
     */
    val kind: CaCfirPropertyAccessorKind,
) : CaCfirPublicSymbolCacheKey

/**
 * 值参数符号的稳定缓存键。
 */
internal data class CaCfirValueParameterSymbolCacheKey(
    /**
     * 所属 callable 或构造器的稳定缓存键。
     */
    val ownerKey: CaCfirPublicSymbolCacheKey,
    /**
     * 参数在 owner 参数列表中的稳定下标。
     */
    val parameterIndex: Int,
    /**
     * 参数名称，用于恢复后的额外一致性校验。
     */
    val parameterName: Name,
) : CaCfirPublicSymbolCacheKey

/**
 * 类型参数符号的稳定缓存键。
 */
internal data class CaCfirTypeParameterSymbolCacheKey(
    /**
     * 所属声明的稳定缓存键。
     */
    val ownerKey: CaCfirPublicSymbolCacheKey,
    /**
     * 类型参数名称，用于恢复后的额外一致性校验。
     */
    val parameterName: Name,
    /**
     * 类型参数在 owner 类型参数列表中的稳定下标。
     */
    val parameterIndex: Int,
) : CaCfirPublicSymbolCacheKey

/**
 * 只能通过 PSI 恢复的局部符号缓存键。
 */
internal data class CaCfirPsiSymbolCacheKey(
    /**
     * 局部符号的源码 PSI。
     */
    val psi: PsiElement,
    /**
     * PSI 对应的公开局部符号种类。
     */
    val kind: CaCfirPsiSymbolKind,
) : CaCfirPublicSymbolCacheKey

/**
 * callable 符号缓存键中的 callable 种类。
 */
internal enum class CaCfirCallableSymbolKind {
    NAMED_FUNCTION,
    MAIN_FUNCTION,
    MACRO,
    FINALIZER,
    CONSTRUCTOR,
    PROPERTY,
    FIELD,
    PATTERN_VARIABLE,
    PATTERN_BINDING,
    ENUM_CONSTRUCTOR,
}

/**
 * 属性访问器缓存键中的访问器种类。
 */
internal enum class CaCfirPropertyAccessorKind {
    GETTER,
    SETTER,
}

/**
 * PSI-based 局部符号缓存键中的符号种类。
 */
internal enum class CaCfirPsiSymbolKind {
    SCRIPT,
    ANONYMOUS_FUNCTION,
    LOCAL_VARIABLE,
    PATTERN_VARIABLE,
    PATTERN_BINDING,
    TYPE_PARAMETER,
}

/**
 * 补全候选去重与可达性判定使用的符号 key 协议。
 */
internal sealed interface CaCfirCompletionSymbolKey

/**
 * 拥有稳定 public symbol key 的补全候选 key。
 */
internal data class CaCfirStableCompletionSymbolKey(
    /**
     * 可跨 session 恢复的 public symbol key。
     */
    val symbolKey: CaCfirPublicSymbolCacheKey,
) : CaCfirCompletionSymbolKey

/**
 * 仅在当前 session 内有效的补全候选 key。
 */
internal data class CaCfirEphemeralCompletionSymbolKey(
    /**
     * 无稳定 key 的公开符号对象。
     */
    val symbol: CaSymbol,
) : CaCfirCompletionSymbolKey
