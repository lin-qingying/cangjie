package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin FIR 的 composite provider。
 *
 * symbol lookup 由 [CfirCompositeSymbolProvider] 聚合，ownership/container 只在 provider 层聚合。
 */
class CfirCompositeProvider(
    session: CfirSession,
    /**
     * 按优先级排列的声明 provider。
     *
     * classifier 与 container 查询按顺序返回第一个命中结果，文件列表与名称集合则聚合所有 provider。
     */
    private val providers: List<CfirProvider>,
) : CfirProvider() {
    /**
     * 聚合后的符号查询入口。
     */
    override val symbolProvider: CfirSymbolProvider =
        CfirCompositeSymbolProvider(session, providers.map { it.symbolProvider })

    /**
     * 任意子 provider 支持 phased CFIR 时，组合 provider 也暴露该能力。
     */
    override val isPhasedCfirAllowed: Boolean
        get() = providers.any(CfirProvider::isPhasedCfirAllowed)

    /**
     * 按 provider 顺序查找第一个匹配的 classifier 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? {
        for (provider in providers) {
            provider.getCfirClassifierByFqName(classId)?.let { return it }
        }
        return null
    }

    /**
     * 返回 classifier 的容器文件；所有子 provider 均未命中时抛出结构性错误。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        getCfirClassifierContainerFileIfAny(fqName)
            ?: error("No containing file found for classifier $fqName")

    /**
     * 按 provider 顺序查找 classifier 的容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? {
        for (provider in providers) {
            provider.getCfirClassifierContainerFileIfAny(fqName)?.let { return it }
        }
        return null
    }

    /**
     * 按 provider 顺序查找 callable 的容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? {
        for (provider in providers) {
            provider.getCfirCallableContainerFile(symbol)?.let { return it }
        }
        return null
    }

    /**
     * 按 provider 顺序查找 pattern binding 所属的外层 pattern variable。
     */
    override fun getCfirPatternVariableForBinding(symbol: CfirPatternBindingSymbol): CfirPatternVariable? {
        for (provider in providers) {
            provider.getCfirPatternVariableForBinding(symbol)?.let { return it }
        }
        return null
    }

    /**
     * 聚合指定包下所有 provider 暴露的 CFIR 文件。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        providers.flatMap { it.getCfirFilesByPackage(fqName) }

    /**
     * 聚合指定包下所有 class-like 短名。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> {
        return buildSet {
            for (provider in providers) {
                addAll(provider.getClassNamesInPackage(fqName))
            }
        }
    }

    /**
     * 按 provider 顺序查找 symbol 的外层 class-like 宿主。
     */
    override fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        for (provider in providers) {
            provider.getContainingClass(symbol)?.let { return it }
        }
        return null
    }
}
