package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.cjmpMatchResultForSpecific

fun CfirSession.cjmpCommonCounterpartOf(declaration: CfirDeclaration): CfirDeclaration? {
    val counterparts = cjmpCommonCounterpartsOf(declaration)
    check(counterparts.size <= 1) { "A single-counterpart query cannot read a multiple-parent declaration" }
    return counterparts.singleOrNull()
}

/** 返回 specific 声明通过阶段访问器发布的全部 common parent 对应物。 */
fun CfirSession.cjmpCommonCounterpartsOf(declaration: CfirDeclaration): List<CfirDeclaration> =
    declaration.symbol.cjmpMatchResultForSpecific()?.counterparts?.map { it.commonSymbol.cfir }.orEmpty()

/** specific 模式下按声明取得其规范组；未启用 CJMP 时不启动候选发现或惰性解析。 */
fun CfirSession.cjmpFamilySnapshotFor(declaration: CfirDeclaration): CfirCjmpFamilySnapshot? =
    if (isCjmpSpecificCompilationEnabled()) cjmpFamilyAggregate.matchingGroupFor(declaration) else null
