package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 聚合 classifier 的结构身份去重，供 eager、LL 与组织导入命名空间共用。
 *
 * provider 保留全部 common/specific 声明；类型名优先级由已确定 scope 的类型查找负责。
 * 仅按真实 ClassId 消除 builtin 的真实/合成重复，保留不同 re-export 身份及输入次序。
 */
fun selectCompositeClassifierCandidates(
    candidates: List<CfirClassLikeSymbol<*>>,
): List<CfirClassLikeSymbol<*>> {
    val materializedBuiltinIds = candidates.filter { symbol ->
        symbol.cfir is CfirBuiltInDeclaration && symbol.cfir.origin !is CfirDeclarationOrigin.Synthetic
    }.mapTo(hashSetOf()) { it.classId }
    val syntheticBuiltinIds = hashSetOf<org.cangnova.cangjie.name.ClassId>()
    return candidates.filter { symbol ->
        val isSyntheticBuiltin = symbol.cfir is CfirBuiltInDeclaration &&
                symbol.cfir.origin is CfirDeclarationOrigin.Synthetic
        when {
            !isSyntheticBuiltin -> true
            symbol.classId in materializedBuiltinIds -> false
            else -> syntheticBuiltinIds.add(symbol.classId)
        }
    }.distinct()
}
