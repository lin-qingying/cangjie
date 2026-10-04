package org.cangnova.cangjie.analysis.api.cfir.symbols.pointers

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession

import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.CfirCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin `KaFirMemberSymbolPointer` 的 member symbol pointer 基座。
 *
 * 当前仓颉主干不区分 JVM static/member scope，也没有 Java interop，
 * 因此这里只保留 owner pointer + member 选择这一条主恢复链。
 */
internal abstract class CaCfirMemberSymbolPointer<S : CaSymbol>(
    /**
     * 成员 owner 声明容器的 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaDeclarationContainerSymbol>,
) : CaCfirSymbolPointerBase<S>() {
    @CaImplementationDetail
    /**
     * 先恢复 owner，再由具体子类在 owner 作用域中选择成员符号。
     */
    final override fun restoreSymbol(session: CaSession): S? {
        val cfirSession = restoreSession(session) ?: return null
        val ownerSymbol = ownerPointer.restoreSymbol(session) ?: return null
        return cfirSession.chooseCandidateAndCreateSymbol(ownerSymbol)
    }

    /**
     * 从 owner 声明容器中选择目标成员并构造公开符号。
     */
    protected abstract fun CaCfirSession.chooseCandidateAndCreateSymbol(ownerSymbol: CaDeclarationContainerSymbol): S?
}

/**
 * 成员函数符号 pointer。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirMemberFunctionSymbolPointer(
    /**
     * 成员函数所属声明容器的 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaDeclarationContainerSymbol>,
    /**
     * 成员函数短名。
     */
    private val name: Name,
    /**
     * 区分重载的 CFIR callable 签名。
     */
    private val signature: CfirCallableSignature,
) : CaCfirMemberSymbolPointer<CaNamedFunctionSymbol>(ownerPointer) {
    @OptIn(CaImplementationDetail::class)
    /**
     * 从 owner 的 declared member scope 中按名称和签名恢复成员函数。
     */
    override fun CaCfirSession.chooseCandidateAndCreateSymbol(ownerSymbol: CaDeclarationContainerSymbol): CaNamedFunctionSymbol? {
        val candidates = when (ownerSymbol) {
            is org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol -> with(this) {
                ownerSymbol.declaredMemberScope.callables(name).toList()
            }

            is org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol -> with(this) {
                ownerSymbol.declaredMemberScope.callables(name).toList()
            }

            else -> emptyList()
        }

        return candidates
            .filterIsInstance<CaNamedFunctionSymbol>()
            .singleOrNull { candidate ->
                val backingSymbol = (candidate as? CaCfirSymbol<*>)?.cfirSymbol
                signature.hasTheSameSignature(
                    backingSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*> ?: return@singleOrNull false
                )
            }
    }
}

/**
 * 字段符号 pointer。
 *
 * 字段此前完全没有稳定指针，导致成员作用域一旦真的暴露字段就无法被渲染或恢复。
 * 补全的候选快照需要「分析结束后按指针重新进入分析」，没有指针就无法满足该契约。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirMemberFieldSymbolPointer(
    /**
     * 字段所属声明容器的 pointer。
     */
    private val ownerPointer: CaSymbolPointer<CaDeclarationContainerSymbol>,
    /**
     * 字段短名。
     */
    private val name: Name,
) : CaCfirMemberSymbolPointer<CaFieldSymbol>(ownerPointer) {
    /**
     * 从 owner 的声明成员作用域中按名恢复字段。
     *
     * 字段在成员作用域中唯一（同一容器内不允许同名字段），因此按名取单个即可；
     * 找不到时返回 `null`，由调用方决定失效表现，不伪造符号。
     */
    override fun CaCfirSession.chooseCandidateAndCreateSymbol(
        ownerSymbol: CaDeclarationContainerSymbol,
    ): CaFieldSymbol? = with(this) {
        when (ownerSymbol) {
            is org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol -> ownerSymbol.declaredMemberScope.callables(name).toList()
            is org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol -> ownerSymbol.declaredMemberScope.callables(name).toList()
            else -> emptyList()
        }.filterIsInstance<CaFieldSymbol>().singleOrNull()
    }
}
