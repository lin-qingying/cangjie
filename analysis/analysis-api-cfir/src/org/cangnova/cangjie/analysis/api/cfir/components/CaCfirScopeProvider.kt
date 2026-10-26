package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirCompositeScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDelegatingNamesAwareScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirFileScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirPackageScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirScopeContext
import org.cangnova.cangjie.analysis.api.cfir.scopes.CfirSingleExtendDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaMemberScopeQuery
import org.cangnova.cangjie.analysis.api.components.CaMemberScopeQueryData
import org.cangnova.cangjie.analysis.api.components.CaScopeContext
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeAccessKind
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.calls.typeToScope
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjBlockCodeFragment
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjFile

/**
 * 作用域组件。
 *
 * 所有 low-level scope 查询都统一通过 session 协议映射为公开 `CaScope`，
 * 不再保留额外的 snapshot 包装协议层。
 */
@OptIn(CaPlatformInterface::class, CaExperimentalApi::class)
internal class CaCfirScopeProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，作用域查询需要复用其中的 scope session 与符号构建器。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaScopeProvider, CaCfirSessionComponent {
    /**
     * 取得当前 CFIR session 对应的作用域缓存会话。
     */
    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.cfirSession)
    }

    /**
     * 将公开声明容器符号还原为可用于底层作用域查询的 CFIR class-like 声明。
     */
    private fun CaDeclarationContainerSymbol.getCfirForScope(): CfirClassLikeDeclaration = when (this) {
        is CaCfirClassSymbol -> cfirSymbol.cfir
        else -> error(
            "`${this::class.qualifiedName}` needs to be specially handled by the scope provider or is an unknown" +
                    " ${CaDeclarationContainerSymbol::class.simpleName} implementation."
        )
    }

    /**
     * 返回声明容器的可见成员作用域。
     */
    override val CaDeclarationContainerSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val cfirScope = getCfirForScope().unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    /**
     * 返回文件级作用域，包含文件导入和包内短名解析视图。
     */
    override fun CjFile.getFileScope(): CaScope = withValidityAssertion {
        CaCfirFileScope(CaCfirFileSymbol(this@getFileScope, analysisSession), analysisSession.cfirSymbolBuilder)
    }

    /**
     * 按包名返回包级作用域，包不存在时返回 null。
     */
    override fun getPackageScope(packageFqName: FqName): CaScope? = withValidityAssertion {
        if (!analysisSession.useSitePackageProvider.doesPackageExist(packageFqName)) return@withValidityAssertion null
        CaCfirPackageScope(packageFqName, analysisSession)
    }

    /**
     * 返回包符号自身对应的包级作用域。
     */
    override val CaPackageSymbol.packageScope: CaScope
        get() = withValidityAssertion {
            val packageSymbol = this@packageScope as? CaCfirPackageSymbol
                ?: error("仅 CFIR 包符号支持包级作用域查询：${this@packageScope::class.simpleName}")
            CaCfirPackageScope(packageSymbol.fqName, analysisSession)
        }

    /**
     * 返回声明容器显式声明成员的组合视图。
     */
    override val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope
        get() = withValidityAssertion {
            when (this@combinedDeclaredMemberScope) {
                is CaClassLikeSymbol -> {
                    val classSymbol = requireClassLikeSymbol(this@combinedDeclaredMemberScope)
                    CaCfirDeclaredMemberScope(
                        CfirClassDeclaredMemberScope(classSymbol.cfirSymbol),
                        analysisSession.cfirSymbolBuilder,
                    )
                }
                else -> error("当前仅 class-like 声明容器支持 combinedDeclaredMemberScope：${this@combinedDeclaredMemberScope::class.simpleName}")
            }
        }

    /**
     * 返回 class-like 符号直接声明的成员作用域。
     */
    override val CaClassLikeSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@declaredMemberScope)
            CaCfirDeclaredMemberScope(
                CfirClassDeclaredMemberScope(classSymbol.cfirSymbol),
                analysisSession.cfirSymbolBuilder,
            )
        }

    /**
     * 返回 extend 声明直接贡献的成员作用域。
     */
    override val org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val extendSymbol = this@declaredMemberScope as? CaCfirExtendSymbol
                ?: error("Only CFIR extend symbols can expose declared-member scope: ${this@declaredMemberScope::class.simpleName}")
            CaCfirDeclaredMemberScope(
                CfirSingleExtendDeclaredMemberScope(extendSymbol.cfirSymbol.cfir),
                analysisSession.cfirSymbolBuilder,
            )
        }

    /**
     * 返回 class-like 符号包含继承成员在内的成员作用域。
     */
    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            val cfirScope = classSymbol.cfirSymbol.cfir.unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    /**
     * 返回类型对应 class-like 声明的成员作用域。
     */
    override val CaType.scope: CaScope?
        get() = withValidityAssertion { useSiteMemberScopeOf(this@scope) }

    /**
     * 返回类型对应 class-like 声明的**实例**成员作用域。
     *
     * 与 [CaType.scope] 走同一条 use-site 作用域构造路径：
     * 仓颉的 use-site 成员作用域已经把继承链与适用 extend 一并纳入，
     * 单独再包一层「仅实例」过滤会丢掉 extend 注入，代价大于收益。
     * 真正的静态/实例区分由 [memberScopeQueries] 按使用点形态完成。
     */
    override val CaType.instanceMemberScope: CaScope?
        get() = withValidityAssertion { useSiteMemberScopeOf(this@instanceMemberScope) }

    /**
     * 返回类型对应 class-like 声明的**静态**成员作用域。
     *
     * 用 `CfirClassStaticScope(...).withoutInstanceMemberDiagnostics()` 得到纯静态视图：
     * 该视图在同名静态成员为空时**不会**回退到实例成员，
     * 因此「`Type.member` 补出实例成员」这类错误候选不会从这里泄漏出去。
     * 静态通道完全不可用时返回 `null`，让调用方能区分「没有静态成员」与「有但需要接收者」。
     */
    override val CaType.staticMemberScope: CaScope?
        get() = withValidityAssertion {
            val base = useSiteTypeScopeOf(this@staticMemberScope) ?: return@withValidityAssertion null
            if (base.hasDefinitelyNoStaticMembers) return@withValidityAssertion null
            CaCfirDelegatingNamesAwareScope(
                CfirClassStaticScope(base).withoutInstanceMemberDiagnostics(),
                analysisSession.cfirSymbolBuilder,
            )
        }

    /**
     * 按使用点形态给出成员作用域查询列表。
     *
     * `STATIC_THROUGH_TYPE_NAME` 只给静态通道；其余三种形态给实例通道。
     * 返回顺序即查询优先级：调用方按序取第一个非空作用域，
     * 因此这里不做过滤——过滤责任在可见性与适用性组件。
     */
    override fun CaType.memberScopeQueries(accessKind: CaTypeAccessKind): List<CaMemberScopeQuery> =
        withValidityAssertion {
            if (accessKind == CaTypeAccessKind.STATIC_THROUGH_TYPE_NAME) {
                listOf(
                    CaMemberScopeQueryData(
                        token = analysisSession.token,
                        scope = staticMemberScope,
                        kind = CaScopeKind.STATIC_MEMBER_SCOPE,
                        accessedThroughTypeName = true,
                        requiresReceiver = false,
                    ),
                )
            } else {
                listOf(
                    CaMemberScopeQueryData(
                        token = analysisSession.token,
                        scope = instanceMemberScope,
                        kind = CaScopeKind.TYPE_SCOPE,
                        accessedThroughTypeName = false,
                        requiresReceiver = true,
                    ),
                )
            }
        }

    /**
     * 构造类型的 use-site 成员作用域（CFIR 层）。
     *
     * 与 `CaTypeScopeProvider.typeScope` 走**同一条** `typeToScope` 接缝：该接缝是
     * 接收者查找与 Analysis 类型作用域共用的唯一入口（任务 4.2），统一处理类型变量
     * 上界、交叉类型、ideal literal、builtin extend、typealias 展开与继承替换。
     * 成员通道若在此各自复制“类型 → 作用域 owner”的还原逻辑，非 nominal 接收者
     * （`ConePrimitiveType` 等）会整体退化成 `null`——`extend Int64` 注入的成员
     * 随之在 IDE 补全与成员查询中不可见，而调用解析路径却一切正常。
     *
     * 成员签名按接收者实际类型实参替换；需要声明侧（未替换）对照视图的消费方走
     * `CaTypeScopeProvider.typeScope.declarationScope`。
     */
    @OptIn(CfirImplementationDetail::class)
    private fun useSiteTypeScopeOf(type: CaType): CfirTypeScope? {
        val coneType = type.requireCfirConeType("成员作用域查询")
        return analysisSession.cacheStorage.getOrCreateTypeScope(coneType) {
            typeToScope(
                analysisSession.cfirSession,
                getScopeSession(),
                coneType,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
            )
        }
    }

    /**
     * 构造类型的 use-site 成员作用域（公开层）。
     */
    private fun useSiteMemberScopeOf(type: CaType): CaScope? {
        val cfirScope = useSiteTypeScopeOf(type) ?: return null
        return CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
    }

    /**
     * 取得 [position] 处的词法作用域上下文。
     *
     * 位置在解析塔内时按真实查找顺序投影；否则退化为文件级导入上下文。
     */
    override fun CjFile.scopeContext(position: CjElement): CaScopeContext = withValidityAssertion {
        val stamp = modificationStamp
        val cached = lastScopeContext
        if (cached != null && lastScopeContextPosition === position && lastScopeContextStamp == stamp) {
            return@withValidityAssertion cached
        }
        return@withValidityAssertion CaCfirScopeContext(analysisSession, this@scopeContext, position).also {
            lastScopeContextPosition = position
            lastScopeContextStamp = stamp
            lastScopeContext = it
        }
    }

    /**
     * 最近一次 [scopeContext] 的位置身份、文件修改戳与结果。
     *
     * 一次补全请求会对**同一位置**反复判定候选（索引段、导入规划、缩短各自遍历），
     * 而位置作用域投影每次都要跑一遍 ContextCollector——按候选重算会把补全主路径
     * 拖成 O(候选数 × 上下文收集)。调用方天然是「同一位置连判多个候选」的形状，
     * 所以只缓存最近一条：命中即省掉一次上下文收集，也不会累积 PSI 引用。
     *
     * 文件修改戳参与键，副本被改动后同一 PSI 位置不会复用旧快照。
     * 会话按 AA 契约单线程使用，因此这几个字段不加同步。
     */
    private var lastScopeContextPosition: CjElement? = null

    /**
     * [lastScopeContext] 对应文件的修改戳。
     */
    private var lastScopeContextStamp: Long = Long.MIN_VALUE

    /**
     * [lastScopeContextPosition] 处的作用域上下文快照。
     */
    private var lastScopeContext: CaScopeContext? = null

    /**
     * 取得该文件的导入/包/顶层声明上下文。
     */
    override val CjFile.importingScopeContext: CaScopeContext
        get() = withValidityAssertion { CaCfirScopeContext(analysisSession, this@importingScopeContext, this@importingScopeContext) }

    /**
     * 取得代码片段自身的顶层声明作用域；非块片段返回 `null`。
     *
     * 取片段内容区里最后一个引用作为查询位置——该位置必然落在片段自身作用域内，
     * 因此不需要调用方再指定位置。与 [CaCfirScopeContext] 共用同一套投影逻辑，
     * 两者不得各写一份。
     */
    override val CjCodeFragment.fragmentMemberScope: CaScope?
        get() = withValidityAssertion {
            val fragment = this@fragmentMemberScope
            if (fragment !is CjBlockCodeFragment) return@withValidityAssertion null
            // 取片段内最后一个引用作为查询位置：它必然落在片段自身作用域内，
            // 调用方因此不必再指定位置。找不到引用时退回片段根，仍能取到该作用域。
            val position = PsiTreeUtil.findChildrenOfType(fragment, CjSimpleNameExpression::class.java)
                .lastOrNull() ?: fragment
            CaCfirScopeContext(analysisSession, fragment, position)
                .scopes
                .firstOrNull { it.kind == CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE }
                ?.scope
        }

    /**
     * 把多个作用域组合为单一名称查询视图，保持顺序与符号身份。
     */
    override fun List<CaScope>.asCompositeScope(filter: (CaScope) -> Boolean): CaScope = withValidityAssertion {
        val delegates = filter { filter(it) }
        check(delegates.isNotEmpty()) { "Composite scope requires at least one delegate scope" }
        if (delegates.size == 1) delegates.single() else CaCfirCompositeScope(delegates, { first, second -> with(analysisSession) { first.isEquivalentTo(second) } })
    }

    /**
     * 按来源过滤合成单一作用域视图，保持上下文优先级顺序。
     */
    override fun CaScopeContext.compositeScope(filter: (CaScopeKind) -> Boolean): CaScope = withValidityAssertion {
        val delegates = scopes.map { it.scope }.filter { scope ->
            scopes.firstOrNull { it.scope === scope }?.let { filter(it.kind) } ?: true
        }
        when {
            delegates.isEmpty() -> error("Composite scope requires at least one scope matching the filter")
            delegates.size == 1 -> delegates.single()
            else -> CaCfirCompositeScope(delegates, { first, second -> with(analysisSession) { first.isEquivalentTo(second) } })
        }
    }

    /**
     * 校验公开 class-like 符号确实由 CFIR 符号承载。
     */
    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirSymbol<CfirClassLikeSymbol<*>> {
        return symbol as? CaCfirSymbol<CfirClassLikeSymbol<*>>
            ?: error("仅 CFIR class-like 符号支持成员作用域查询：${symbol::class.simpleName}")
    }

}

/**
 * 返回 class、interface、struct 或 enum 声明成员及显式继承成员，不纳入 extend 注入的成员。
 */
internal fun CaCfirSession.declarationSiteMemberScope(symbol: CaClassSymbol): CaScope = withValidityAssertion {
    val classSymbol = symbol as? CaCfirClassSymbol
        ?: error("仅 CFIR class symbol 支持声明站点成员作用域：${symbol::class.simpleName}")
    val lowLevelSession = cfirSession
    val cfirScope = lowLevelSession.cangjieScopeProvider.getDeclarationSiteMemberScope(classSymbol.cfirSymbol, lowLevelSession)
    CaCfirDelegatingNamesAwareScope(cfirScope, cfirSymbolBuilder)
}
