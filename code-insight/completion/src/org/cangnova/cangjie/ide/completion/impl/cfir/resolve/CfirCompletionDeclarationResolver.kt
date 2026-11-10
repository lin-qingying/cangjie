package org.cangnova.cangjie.ide.completion.impl.cfir.resolve

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.cfir.sections.qualifiedNameText
import org.cangnova.cangjie.ide.completion.impl.shared.resolve.CompletionDeclarationResolver

/**
 * 按候选身份恢复声明（`impl-cfir` 实现）。
 *
 * **必须放在生成候选的这一层**：快照里的 `qualifiedName` 由快照工厂用本层的算法算出
 * （callable 走 `qualifiedNameText()`、分类器走 `ClassId` 的完全限定名），
 * 恢复端只有用同一套算法才能精确对上；放到消费端就要复制这份算法。
 *
 * 匹配策略：名称前缀召回（索引接口按前缀给名字，长前缀退化为子串匹配）→
 * **短名精确相等** → **限定名相等**。同名重载会有多个同限定名的候选，
 * 取首个即可：CDoc 挂在声明组上，各重载共享同一份文档。
 */
class CfirCompletionDeclarationResolver : CompletionDeclarationResolver {
    override fun <T> resolveDeclaration(
        session: CaSession,
        snapshot: CompletionSnapshot,
        use: (CaDeclarationSymbol) -> T,
    ): T? {
        val name = snapshot.identity.name ?: return null
        val qualifiedName = snapshot.identity.qualifiedName
        return session.getDeclarationsByNamePrefix(name, isRepeatedInvocation = true)
            .filterIsInstance<CaDeclarationSymbol>()
            .filter { (it as? CaNamedSymbol)?.name?.asString() == name }
            .firstOrNull { it.qualifiedNameForResolve() == qualifiedName }
            ?.let(use)
    }

    /**
     * 恢复时使用的限定名：与快照工厂的算法一致（否则永远匹配不上）。
     */
    private fun CaDeclarationSymbol.qualifiedNameForResolve(): String? = when (this) {
        is CaClassLikeSymbol -> classId?.asSingleFqName()?.asString()
        is CaCallableSymbol -> qualifiedNameText()
        else -> (this as? CaNamedSymbol)?.name?.asString()
    }
}
