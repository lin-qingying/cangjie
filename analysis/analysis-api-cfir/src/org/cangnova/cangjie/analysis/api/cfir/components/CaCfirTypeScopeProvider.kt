package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDelegatingNamesAwareScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.getCallableSignatures
import org.cangnova.cangjie.analysis.api.cfir.scopes.getClassifierSymbols
import org.cangnova.cangjie.analysis.api.cfir.scopes.getConstructors
import org.cangnova.cangjie.analysis.api.components.CaTypeScopeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.scopes.CaTypeScope
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.calls.typeToScope
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toOwningClassLikeSymbol
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.name.Name

/**
 * 具体类型的类型作用域实现。
 *
 * 成员集合来自接收者查找共用的 `typeToScope` 接缝：它按**完整 Cone 类型**（而非 class-like 声明）
 * 构造成员 scope，因此同一泛型声明的不同实例得到各自的成员签名，并统一处理类型变量上界、
 * 交叉类型、ideal primitive extend 与继承链替换。
 *
 * @property type 进入本作用域的公开类型，仅用于取声明视图（[declarationScope]）。
 * @property cfirScope 替换后的 CFIR 成员作用域。
 * @property builder 公开符号构建器，同时提供生命周期令牌。
 */
@OptIn(CaExperimentalApi::class, CfirImplementationDetail::class)
internal class CaCfirTypeScope(
    private val type: CaType,
    private val cfirScope: CfirTypeScope,
    private val builder: CaSymbolByCfirBuilder,
    /**
     * 取本类型对应**声明**视图（未替换）的入口，由 provider 提供，避免本类再走一遍类型→声明的还原。
     */
    private val declarationScopeProvider: (CaType) -> CaScope?,
) : CaTypeScope {
    /**
     * 生命周期与构建器一致。
     */
    override val token: CaLifetimeToken get() = builder.token

    /**
     * 本作用域可能包含的全部名字。
     */
    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion {
        getPossibleCallableNames() + getPossibleClassifierNames()
    }

    /**
     * 本作用域可能包含的 callable 名字。
     */
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { cfirScope.getCallableNames() }

    /**
     * 本作用域可能包含的 classifier 名字。
     */
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { cfirScope.getClassifierNames() }

    /**
     * 按名字过滤器查询替换后的 callable 签名。
     */
    override fun callableSignatures(nameFilter: (Name) -> Boolean): Sequence<CaCallableSignature<*>> =
        withValidityAssertion {
            cfirScope.getCallableSignatures(getPossibleCallableNames().filter(nameFilter), builder)
        }

    /**
     * 按名字集合查询替换后的 callable 签名。
     */
    override fun callableSignatures(names: Collection<Name>): Sequence<CaCallableSignature<*>> =
        withValidityAssertion { cfirScope.getCallableSignatures(names, builder) }

    /**
     * 按名字过滤器查询 classifier。
     */
    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<CaClassifierSymbol> = withValidityAssertion {
        cfirScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    /**
     * 按名字集合查询 classifier。
     */
    override fun classifiers(names: Collection<Name>): Sequence<CaClassifierSymbol> =
        withValidityAssertion { cfirScope.getClassifierSymbols(names, builder) }

    /**
     * 本作用域的构造器。
     */
    override val constructors: Sequence<CaConstructorSymbol>
        get() = withValidityAssertion { cfirScope.getConstructors(builder) }

    /**
     * 未替换的声明视图；由 [CaCfirTypeScopeProvider.declarationScope] 转发。
     *
     * 取不到说明类型本身无法还原成 class-like 声明（错误类型等），属于调用方前提被破坏，
     * 直接失败并带上类型，避免返回一个必然误导的空作用域。
     */
    val declarationScope: CaScope
        get() = withValidityAssertion {
            declarationScopeProvider(type)
                ?: error("类型作用域缺少声明视图（类型无法还原成 class-like 声明）：$type")
        }
}

/**
 * 具体类型作用域组件。
 */
@OptIn(CaPlatformInterface::class, CaExperimentalApi::class, CfirImplementationDetail::class)
internal class CaCfirTypeScopeProvider(
    /**
     * 延迟取得当前 CFIR Analysis session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeScopeProvider, CaCfirSessionComponent {
    /**
     * 具体类型的替换后成员作用域。
     *
     * 类型无法构造成员视图（错误类型、非 class-like 且非原始类型、无上界的类型变量）时返回 `null`。
     */
    override val CaType.typeScope: CaTypeScope?
        get() = withValidityAssertion {
            val coneType = this.requireCfirConeType("类型作用域查询")
            val cfirScope = analysisSession.cacheStorage.getOrCreateTypeScope(coneType) {
                typeToScope(
                    session = analysisSession.cfirSession,
                    scopeSession = analysisSession.getScopeSessionFor(analysisSession.cfirSession),
                    type = coneType,
                    scopeKind = CfirClassMemberScopeKind.USE_SITE,
                )
            } ?: return@withValidityAssertion null
            CaCfirTypeScope(this, cfirScope, analysisSession.cfirSymbolBuilder, ::declarationScopeOf)
        }

    /**
     * 该类型作用域对应的声明视图（未替换类型参数）。
     */
    override val CaTypeScope.declarationScope: CaScope
        get() = withValidityAssertion { (this as CaCfirTypeScope).declarationScope }

    /**
     * 取声明视图：把类型还原成声明侧 owner 后取 unsubstituted scope。
     *
     * 与 use-site 视图的分工：这里保留**未替换**的声明签名（`Box<Int64>` 的成员
     * 仍显示 `T`），作为 `TypeScopeExecutionTest` 断言 ② 的对照视图。
     *
     * owner 还原走 [toOwningClassLikeSymbol]：基本类型按合成 [ClassId] 取
     * `CfirPrimitiveTypeSymbol`（`ConePrimitiveType` 不是 lookup-tag 类型，
     * `toClassLikeSymbol` 对它返回 `null`，会撞上本类 `declarationScope` 的
     * `error("类型作用域缺少声明视图")` 兜底）；typealias 先按语义展开再取 owner，
     * 与 extend 索引的同一展开纪律对齐。
     *
     * `CPointer` / `CString` 没有 nominal 声明视图，其成员表面全部来自 extend；
     * 此时对照视图退化为 use-site 视图本身——对照的目的是“未替换 vs 替换”，
     * 无声明可对照时返回替换后结果比直接失败更接近调用方预期。
     */
    private fun declarationScopeOf(type: CaType): CaScope? {
        val expandedConeType = type.requireCfirConeType("类型作用域声明视图")
            .fullyExpandedType(analysisSession.cfirSession)
        val owner = expandedConeType.toOwningClassLikeSymbol(analysisSession.cfirSession)
        if (owner == null) {
            // 无 nominal 声明可对照（CPointer / CString 等内建目标）：成员表面全部
            // 来自 extend，对照视图退化为 use-site 视图本身。
            val useSiteScope = analysisSession.cacheStorage.getOrCreateTypeScope(expandedConeType) {
                typeToScope(
                    session = analysisSession.cfirSession,
                    scopeSession = analysisSession.getScopeSessionFor(analysisSession.cfirSession),
                    type = expandedConeType,
                    scopeKind = CfirClassMemberScopeKind.USE_SITE,
                )
            } ?: return null
            return CaCfirDelegatingNamesAwareScope(useSiteScope, analysisSession.cfirSymbolBuilder)
        }
        return owner.cfir
            .unsubstitutedScope(
                analysisSession.cfirSession,
                analysisSession.getScopeSessionFor(analysisSession.cfirSession),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            ?.let { CaCfirDelegatingNamesAwareScope(it, analysisSession.cfirSymbolBuilder) }
    }
}
