package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.buildSymbol
import org.cangnova.cangjie.analysis.api.components.CaImplicitReceiver
import org.cangnova.cangjie.analysis.api.components.CaScopeContext
import org.cangnova.cangjie.analysis.api.components.CaScopeKind
import org.cangnova.cangjie.analysis.api.components.CaScopeWithKind
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.ContextCollector
import org.cangnova.cangjie.cfir.calls.producesInapplicableCandidate
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.createFileLookupScopes
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.lookupOriginForAccessibility
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirBuiltinPrimitiveScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.importBindingStore
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 解析塔到公开 [CaScopeContext] 的投影。
 *
 * 投影只做两件事：按真实查找顺序暴露作用域，并标注每个作用域的结构性来源。
 * 可见性、适用性与导入需求都不是本投影的结论，必须由对应组件在使用点另行判断。
 *
 * @param session 当前 CFIR Analysis 会话。
 * @param file 上下文所属 PSI 文件。
 * @param position 投影所依据的位置；无法定位到解析塔时退化为文件级导入上下文。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirScopeContext(
    private val session: CaCfirSession,
    private val file: CjFile,
    private val position: CjElement,
) : CaScopeContext {

    /**
     * 上下文所属 CFIR 文件，按当前会话惰性构建。
     */
    private val cfirFile: CfirFile by lazy(LazyThreadSafetyMode.PUBLICATION) {
        file.getOrBuildCfirFile(session.resolutionFacade)
    }

    /**
     * 公开上下文跟随当前会话的生命周期令牌。
     */
    override val token: CaLifetimeToken get() = session.token

    /**
     * 按查找优先级从高到低排列的作用域条目。
     *
     * 解析塔按低到高优先级保存，公开契约要求从高到低，因此这里倒序投影。
     * 同一层的同名重载与导入别名全部保留，不做显示名去重。
     *
     * 解析塔快照只携带词法层与隐式接收者，文件级查找层不在其中；
     * 因此文件顶层声明、包与导入层由 [importingScopeContext] 提供并追加到低优先级端，
     * 否则函数体内的候选会漏掉全部顶层声明。
     */
    override val scopes: List<CaScopeWithKind> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        withValidityAssertion {
            val lexical = resolveTowerData()?.towerDataElements?.asReversed()
                ?.flatMap(::toScopeEntries)
                .orEmpty()
            val fileLevel = fileLevelEntries()
            (lexical + fileLevel).distinctBy { it.kind.indexInTower to it.scope }
        }
    }

    /**
     * 按塔层级从内到外排列的隐式接收者。
     *
     * 不可访问且不会产生可用候选的接收者被剔除；是否可见仍由可见性组件在使用点判断。
     */
    override val implicitReceivers: List<CaImplicitReceiver> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        withValidityAssertion {
            val towerData = resolveTowerData() ?: return@withValidityAssertion emptyList()
            val elements = towerData.towerDataElements
            elements.mapIndexedNotNull { index, element ->
                val receiver = element.implicitReceiver?.takeUnless { it.producesInapplicableCandidate() }
                    ?: return@mapIndexedNotNull null
                CaCfirImplicitReceiver(
                    token = session.token,
                    type = receiver.type.toPublicType(),
                    ownerSymbol = session.cfirSymbolBuilder.buildSymbol(receiver.boundSymbol),
                    scopeIndexInTower = elements.size - index,
                )
            }
        }
    }

    /**
     * 解析给定位置的 tower data；无法解析时返回 `null`，由调用方退化为导入上下文。
     */
    private fun resolveTowerData(): CfirTowerDataContext? =
        ContextCollector.process(session.resolutionFacade, cfirFile, position)?.towerDataContext

    /**
     * 把单个解析塔元素投影为作用域条目。
     */
    private fun toScopeEntries(element: CfirTowerDataElement): List<CaScopeWithKind> =
        element.getAvailableScopes().mapNotNull { cfirScope ->
            if (cfirScope === CfirTypeScope.Empty) return@mapNotNull null
            val publicScope = cfirScope.toPublicScopeOrNull(session.cfirSymbolBuilder) ?: return@mapNotNull null
            CaCfirScopeWithKind(
                token = session.token,
                scope = publicScope,
                kind = cfirScope.toScopeKind(isLocal = element.isLocal),
                ownerSymbol = element.staticScopeOwnerSymbol?.let { session.cfirSymbolBuilder.buildSymbol(it) },
            )
        }

    /**
 * 取得文件级作用域条目：本文件顶层声明、同包成员，以及 IMPORTS 阶段已记录的显式/默认导入。
     *
     * 导入层直接复用 CFIR 的文件查找布局；没有导入绑定的文件（无 import 声明）
     * 只暴露本文件顶层声明与包成员，不回退到 provider 现场重放导入，
     * 以免与编译器的 IMPORTS 阶段形成第二套导入语义。
     */
    private fun fileLevelEntries(): List<CaScopeWithKind> =
        session.fileLevelScopeEntries(cfirFile)

    /**
     * 构造隐式接收者的公开类型，保留接收者的实际类型实参。
     */
    private fun org.cangnova.cangjie.cfir.types.ConeCangJieType.toPublicType(): CaType =
        session.cfirSymbolBuilder.typeBuilder.buildType(this)
}

/**
 * 取得文件级作用域条目：本文件顶层声明、同包成员，以及 IMPORTS 阶段已记录的显式/默认导入。
 *
 * 导入层直接复用 CFIR 的文件查找布局；没有导入绑定的文件（无 import 声明）
 * 只暴露本文件顶层声明、包成员与内建类型，不回退到 provider 现场重放导入，
 * 以免与编译器的 IMPORTS 阶段形成第二套导入语义。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal fun CaCfirSession.fileLevelScopeEntries(cfirFile: CfirFile): List<CaScopeWithKind> {
    val cfirScopes = if (cfirSession.importBindingStore.getBindings(cfirFile) != null) {
        cfirSession.createFileLookupScopes(cfirFile, getScopeSessionFor(cfirSession)).typeResolutionScopes
    } else {
        listOf(
            CfirFileDeclaredTopLevelScope(cfirFile),
            CfirPackageMemberScope(cfirFile.packageDirective.packageFqName, cfirSession),
            CfirBuiltinPrimitiveScope(cfirSession),
        )
    }
    return cfirScopes.mapNotNull { cfirScope ->
        val publicScope = cfirScope.toPublicScopeOrNull(cfirSymbolBuilder) ?: return@mapNotNull null
        CaCfirScopeWithKind(
            token = token,
            scope = publicScope,
            kind = cfirScope.toScopeKind(isLocal = false),
            ownerSymbol = null,
        )
    }
}

/**
 * 按底层 scope 的真实类型与查找来源判定结构性来源，不按解析塔下标猜测。
 */
private fun CfirScope.toScopeKind(isLocal: Boolean): CaScopeKind = when {
    // 片段作用域必须在 `isLocal` 之前判定：它在查找行为上是局部作用域，
    // 但公开层要单独成类——片段顶层声明与片段内 `let` 局部是两层，不是同一层。
    this is CfirLocalScope && isCodeFragmentMemberScope -> CaScopeKind.CODE_FRAGMENT_MEMBER_SCOPE
    isLocal -> CaScopeKind.LOCAL_SCOPE
    this is CfirTypeParameterScopeImpl -> CaScopeKind.TYPE_PARAMETER_SCOPE
    this is CfirClassStaticScope -> CaScopeKind.STATIC_MEMBER_SCOPE
    this is CfirFileDeclaredTopLevelScope -> CaScopeKind.FILE_SCOPE
    this is CfirPackageMemberScope -> CaScopeKind.PACKAGE_MEMBER_SCOPE
    this is CfirBuiltinPrimitiveScope -> CaScopeKind.BUILTIN_SCOPE
    lookupOriginForAccessibility() == CfirLookupOrigin.EXPLICIT_IMPORT ->
        if (this is CfirExplicitStarImportingScope) {
            CaScopeKind.EXPLICIT_STAR_IMPORTING_SCOPE
        } else {
            CaScopeKind.EXPLICIT_SIMPLE_IMPORTING_SCOPE
        }

    lookupOriginForAccessibility() == CfirLookupOrigin.DEFAULT_IMPORT ->
        if (this is CfirExplicitStarImportingScope) {
            CaScopeKind.DEFAULT_STAR_IMPORTING_SCOPE
        } else {
            CaScopeKind.DEFAULT_SIMPLE_IMPORTING_SCOPE
        }

    this is CfirTypeScope -> CaScopeKind.TYPE_SCOPE
    this is CfirLocalScope -> CaScopeKind.LOCAL_SCOPE
    else -> CaScopeKind.OTHER
}

/**
 * 把底层 CFIR 作用域包装为公开作用域；当前只映射 names-aware 作用域，
 * 未映射的类型返回 `null`，避免伪造不完整的成员集合。
 */
private fun CfirScope.toPublicScopeOrNull(builder: CaSymbolByCfirBuilder): CaScope? =
    (this as? CfirContainingNamesAwareScope)?.let { CaCfirDelegatingNamesAwareScope(it, builder) }

/**
 * 单个作用域条目的实现。
 *
 * [ownerSymbol] 仅在底层解析塔提供 owner 元数据时给出；文件、包与内建作用域没有声明者，返回 `null`。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirScopeWithKind(
    override val token: CaLifetimeToken,
    override val scope: CaScope,
    override val kind: CaScopeKind,
    override val ownerSymbol: CaSymbol?,
) : CaScopeWithKind {

    override fun toString(): String = "CaScopeWithKind(kind=$kind, scope=$scope, owner=$ownerSymbol)"
}

/**
 * 隐式接收者条目的实现。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirImplicitReceiver(
    override val token: CaLifetimeToken,
    override val type: CaType,
    override val ownerSymbol: CaSymbol,
    override val scopeIndexInTower: Int,
) : CaImplicitReceiver {

    override fun toString(): String = "CaImplicitReceiver(type=$type, owner=$ownerSymbol, index=$scopeIndexInTower)"
}