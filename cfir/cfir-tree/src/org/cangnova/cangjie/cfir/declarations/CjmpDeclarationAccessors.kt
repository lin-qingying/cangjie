package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.session.CfirCjmpDeclarationRole
import org.cangnova.cangjie.cfir.session.CfirCjmpMatchingMode
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpMatchingCandidate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.requiresDeferredCjmpFinalMatching
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides

/**
 * specific 声明的唯一正向读取入口。直接推进当前声明；LL designation collector
 * 会将访问器与 pattern binding 归一到原生阶段根，批处理模式由整树阶段驱动。
 * 返回声明自身冻结的 counterpart 与外层类型参数映射。
 */
fun CfirBasedSymbol<*>.cjmpMatchResultForSpecific(): CfirCjmpDeclarationMatchResult? {
    val declaration = (cfir as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides() ?: cfir
    if (declaration is CfirCallableDeclaration && declaration.isLocal) return null
    if (!declaration.moduleData.session.isCjmpSpecificCompilationEnabled()) return null
    val roleDeclaration = when (declaration) {
        is CfirPropertyAccessor -> declaration.propertySymbol.cfir
        else -> declaration
    }
    if (!roleDeclaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return null

    declaration.lazyResolveToPhase(CfirResolvePhase.CJMP_MATCHING)

    val result = declaration.cjmpMatchResult ?: return null
    check(result.specificSymbol === declaration.symbol) {
        "A declaration must read only its own CJMP result"
    }
    return result
}

/**
 * 候选过滤读取声明级已发布关系；alpha.66 在 IMPLICIT_TYPES 前尚无最终关系时，
 * 读取单独缓存的查询期 family 快照。该临时结果绝不写入声明属性，也不会污染最终匹配快照。
 */
fun CfirBasedSymbol<*>.cjmpMatchResultForActualization(): CfirCjmpDeclarationMatchResult? {
    cjmpMatchResultForSpecific()?.let { return it }

    val declaration = (cfir as? CfirCallableDeclaration)?.unwrapSubstitutionOverrides() ?: cfir
    if (declaration is CfirCallableDeclaration && declaration.isLocal) return null
    val session = declaration.moduleData.session
    if (!session.requiresDeferredCjmpFinalMatching() ||
        declaration.resolvePhase >= CfirResolvePhase.CJMP_FINAL_MATCHING
    ) return null

    val roleDeclaration = if (declaration is CfirPropertyAccessor) declaration.propertySymbol.cfir else declaration
    if (!roleDeclaration.isCjmpMatchingCandidate(CfirCjmpDeclarationRole.SPECIFIC)) return null

    return session.cjmpFamilyAggregate.matchingGroupFor(
        roleDeclaration,
        matchingMode = CfirCjmpMatchingMode.ACTUALIZATION_LOOKUP,
    ).declarationMatchResultFor(declaration)
}
