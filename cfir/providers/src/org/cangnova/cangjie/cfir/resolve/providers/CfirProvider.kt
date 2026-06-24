package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.containingClassLookupTag
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.resolve.toClassSymbol
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
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
    /**
     * 与当前 provider 同源的符号查询入口。
     *
     * 声明级 provider 负责文件、宿主与归属信息；实际 symbol 查找必须委托给该属性，
     * 以保证 source、library、builtin 与 composite provider 的查询边界一致。
     */
    abstract val symbolProvider: CfirSymbolProvider

    /**
     * 当前 provider 是否允许在不同 resolve phase 下返回尚未完全解析的 CFIR。
     *
     * 默认关闭，只有能维护 phased declaration 生命周期的 provider 才应开启。
     */
    open val isPhasedCfirAllowed: Boolean
        get() = false

    /**
     * 按 [classId] 查询 class-like 声明。
     *
     * 返回 `null` 表示该 provider 没有该声明，而不是声明解析失败。
     */
    abstract fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration?

    /**
     * 返回 [fqName] 对应 classifier 的容器文件。
     *
     * 调用方已确认声明存在时使用；找不到容器文件代表 provider 索引不完整。
     */
    abstract fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile

    /**
     * 尝试返回 [fqName] 对应 classifier 的容器文件。
     *
     * 与 [getCfirClassifierContainerFile] 相同但以 `null` 表达未命中，用于 composite provider 继续查询后续来源。
     */
    abstract fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile?

    /**
     * 返回 [symbol] 对应 classifier 的容器文件。
     *
     * 默认通过 symbol 的 [ClassId] 路由到 [getCfirClassifierContainerFile]。
     */
    open fun getCfirClassifierContainerFile(symbol: CfirClassLikeSymbol<*>): CfirFile =
        getCfirClassifierContainerFile(symbol.classId)

    /**
     * 尝试返回 [symbol] 对应 classifier 的容器文件。
     *
     * 默认通过 symbol 的 [ClassId] 路由到 [getCfirClassifierContainerFileIfAny]。
     */
    open fun getCfirClassifierContainerFileIfAny(symbol: CfirClassLikeSymbol<*>): CfirFile? =
        getCfirClassifierContainerFileIfAny(symbol.classId)

    /**
     * 返回 callable symbol 的容器文件。
     *
     * 对合成 callable、builtin callable 或跨来源 callable，可返回 `null` 表示没有稳定文件归属。
     */
    abstract fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile?

    /**
     * 返回 pattern binding 所属的外层 pattern variable。
     *
     * 仓颉的名字解析暴露的是 `let (a, b) = ...` 中的 binding symbol，
     * 但隐式类型推断入口属于携带 initializer 的外层 pattern variable。
     */
    open fun getCfirPatternVariableForBinding(symbol: CfirPatternBindingSymbol): CfirPatternVariable? = null

    /**
     * 返回指定包下由该 provider 管理的 CFIR 文件。
     */
    abstract fun getCfirFilesByPackage(fqName: FqName): List<CfirFile>

    /**
     * 返回指定包下已知的 class-like 短名集合。
     *
     * 该集合用于 scope 快速过滤，必须不能漏报当前 provider 确实拥有的 class-like 声明。
     */
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
