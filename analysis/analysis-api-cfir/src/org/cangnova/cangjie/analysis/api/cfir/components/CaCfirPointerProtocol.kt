package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 符号指针恢复键。
 *
 * 各类公开 symbol pointer 统一收敛为“恢复键 + session 内部恢复协议”。
 */
internal sealed interface CaCfirSymbolRestoreKey {
    fun restore(session: CaSession): CaSymbol?
}

internal data class CaCfirFileSymbolRestoreKey(
    val file: CjFile,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.createFileSymbol(file)
}

internal data class CaCfirPackageSymbolRestoreKey(
    val fqName: FqName,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.getPackagePublicSymbol(fqName)
}

internal data class CaCfirClassLikeSymbolRestoreKey(
    val classId: ClassId,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.getClassLikePublicSymbol(classId)
}

internal data class CaCfirCallableSymbolRestoreKey(
    val callableId: CallableId,
) : CaCfirSymbolRestoreKey {
    override fun restore(session: CaSession): CaSymbol? =
        (session as? CaCfirSession)?.restoreCallablePublicSymbol(callableId)
}

internal fun CaSymbol.createRestoreKey(): CaCfirSymbolRestoreKey = when (this) {
    is CaCfirFileSymbolImpl -> CaCfirFileSymbolRestoreKey(file)
    is CaCfirPackageSymbolImpl -> CaCfirPackageSymbolRestoreKey(fqName)
    is CaCfirClassLikeSymbolImpl -> CaCfirClassLikeSymbolRestoreKey(classId)
    is CaCfirCallableSymbolImpl -> {
        val resolvedCallableId = callableId
            ?: error("匿名或局部 callable 不属于公开符号恢复协议")
        CaCfirCallableSymbolRestoreKey(resolvedCallableId)
    }

    else -> error("不支持为 ${this::class.simpleName} 创建符号恢复键")
}

/**
 * 只有进入公开恢复协议的符号才允许生成稳定公开缓存键。
 */
internal fun CaSymbol.publicSymbolCacheKeyOrNull(): CaCfirPublicSymbolCacheKey? = when (this) {
    is CaCfirFileSymbolImpl -> CaCfirFileSymbolCacheKey(file)
    is CaCfirPackageSymbolImpl -> CaCfirPackageSymbolCacheKey(fqName)
    is CaCfirClassLikeSymbolImpl -> CaCfirClassLikeSymbolCacheKey(classId)
    is CaCfirCallableSymbolImpl -> callableId?.let(::CaCfirCallableSymbolCacheKey)
    else -> null
}

/**
 * 统一的 CFIR symbol pointer 实现。
 */
internal class CaCfirSymbolPointerDelegate<out S : CaSymbol>(
    private val restoreKey: CaCfirSymbolRestoreKey,
) : CaSymbolPointer<S> {
    @Suppress("UNCHECKED_CAST")
    override fun restoreSymbol(session: CaSession): S? =
        restoreKey.restore(session) as? S
}
