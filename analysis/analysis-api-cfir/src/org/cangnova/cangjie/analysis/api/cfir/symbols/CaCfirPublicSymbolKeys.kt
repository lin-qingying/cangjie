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

internal data class CaCfirPackageSymbolCacheKey(
    val fqName: FqName,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirFileSymbolCacheKey(
    val file: CjFile,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirClassLikeSymbolCacheKey(
    val classId: ClassId,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirExtendSymbolCacheKey(
    /** `extend` 的稳定语义身份，供缓存与 pointer 恢复使用。 */
    val identity: CaCfirExtendSymbolIdentity,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirCallableSymbolCacheKey(
    val callableId: CallableId,
    val kind: CaCfirCallableSymbolKind,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirExtendMemberCallableSymbolCacheKey(
    /** owner extend 的稳定语义身份，而不是公开 `extendId` 文本。 */
    val extendIdentity: CaCfirExtendSymbolIdentity,
    val callableName: Name,
    val kind: CaCfirCallableSymbolKind,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirPropertyAccessorSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val kind: CaCfirPropertyAccessorKind,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirValueParameterSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val parameterIndex: Int,
    val parameterName: Name,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirTypeParameterSymbolCacheKey(
    val ownerKey: CaCfirPublicSymbolCacheKey,
    val parameterName: Name,
    val parameterIndex: Int,
) : CaCfirPublicSymbolCacheKey

internal data class CaCfirPsiSymbolCacheKey(
    val psi: PsiElement,
    val kind: CaCfirPsiSymbolKind,
) : CaCfirPublicSymbolCacheKey

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

internal enum class CaCfirPropertyAccessorKind {
    GETTER,
    SETTER,
}

internal enum class CaCfirPsiSymbolKind {
    SCRIPT,
    ANONYMOUS_FUNCTION,
    LOCAL_VARIABLE,
    PATTERN_VARIABLE,
    PATTERN_BINDING,
    TYPE_PARAMETER,
}

internal sealed interface CaCfirCompletionSymbolKey

internal data class CaCfirStableCompletionSymbolKey(
    val symbolKey: CaCfirPublicSymbolCacheKey,
) : CaCfirCompletionSymbolKey

internal data class CaCfirEphemeralCompletionSymbolKey(
    val symbol: CaSymbol,
) : CaCfirCompletionSymbolKey
