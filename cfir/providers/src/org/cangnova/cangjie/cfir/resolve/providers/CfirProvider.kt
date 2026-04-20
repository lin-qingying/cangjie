package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin FIR `FirProvider` 的声明归属 provider。
 *
 * 该层只承载：
 * - classifier 声明查询
 * - container file 查询
 * - containing class 查询
 * - phased provider 能力表达
 *
 * symbol lookup 统一由 [symbolProvider] 承担。
 */
abstract class CfirProvider : CfirSessionComponent {
    abstract val symbolProvider: CfirSymbolProvider

    open val isPhasedCfirAllowed: Boolean
        get() = false

    abstract fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration?

    abstract fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile

    abstract fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile?

    open fun getCfirClassifierContainerFile(symbol: CfirClassLikeSymbol<*>): CfirFile =
        getCfirClassifierContainerFile(symbol.classId)

    open fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? =
        getCfirClassifierContainerFileIfAny(symbol.classId)

    abstract fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile?

    abstract fun getCfirFilesByPackage(fqName: FqName): List<CfirFile>

    abstract fun getClassNamesInPackage(fqName: FqName): Set<Name>

    /**
     * 返回声明所属的外层 class-like 符号。
     *
     * 仓颉当前公开 `ClassId` 只覆盖顶层 class-like，因此默认实现只处理 callable owner。
     * source/IDE provider 可在需要时覆写更精确的宿主判定。
     */
    open fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        return when (symbol) {
            is CfirCallableSymbol<*> -> {
                val session = symbol.cfir.moduleData.session
                symbol.callableId.classId?.let(session.symbolProvider::getClassLikeSymbolByClassId)
                    ?: (symbol.cfir as? CfirCallableDeclaration)?.containingClassLookupTag()?.toClassSymbol(session)
            }

            is CfirClassLikeSymbol<*> -> null
            else -> null
        }
    }
}
