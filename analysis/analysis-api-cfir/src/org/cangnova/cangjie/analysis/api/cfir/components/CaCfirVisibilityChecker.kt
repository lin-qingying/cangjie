package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaUseSiteVisibilityChecker
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.collectUseSiteContainers
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.calls.visibility.moduleVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.scopes.CfirCallableWithLookupProvenance
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.psi.CjExpression

/**
 * CFIR use-site 可见性判定实现。
 *
 * 这里不再把“当前 session 能否恢复为同一 public symbol”误当成可见性，
 * 而是收回到 Kotlin 同层的 use-site file / position / receiver 形状，
 * 再复用仓颉编译器现有的可见性规则。
 */
@OptIn(CaExperimentalApi::class)
internal class CaCfirVisibilityChecker(
    /**
     * 延迟取得当前 CFIR Analysis session，所有可见性判断都通过该 session 访问 LL FIR/CFIR 服务。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
    /**
     * 为指定 use-site 构造可复用的声明可见性检查器。
     */
    override fun createUseSiteVisibilityChecker(
        useSiteFile: org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol,
        receiverExpression: CjExpression?,
        position: PsiElement,
    ): CaUseSiteVisibilityChecker = withPsiValidityAssertion(receiverExpression, position) {
        require(useSiteFile is CaCfirFileSymbol)

        val positionModule = resolutionFacade.moduleProvider.getModule(position)
        val containingDeclarations = collectUseSiteContainers(position, resolutionFacade).orEmpty()

        CaCfirUseSiteVisibilityChecker(
            position = position,
            positionModule = positionModule,
            containingDeclarations = containingDeclarations,
            useSiteFile = useSiteFile,
            analysisSession = analysisSession,
            token = token,
        )
    }

    /**
     * 判断 callable 成员是否在目标类内部可见。
     */
    override fun CaCallableSymbol.isVisibleInClass(classSymbol: CaClassSymbol): Boolean = withValidityAssertion {
        require(this@isVisibleInClass is CaCfirSymbol<*>)
        require(classSymbol is CaCfirSymbol<*>)

        val candidateDeclaration = cfirSymbol.cfir as? CfirMemberDeclaration ?: return false
        val classDeclaration = classSymbol.cfirSymbol.cfir as? CfirClassLikeDeclaration ?: return false
        val useSiteFile = classDeclaration.symbol.getContainingFile() ?: return false

        analysisSession.isDeclarationVisibleAtUseSite(
            candidateDeclaration = candidateDeclaration,
            useSiteFile = useSiteFile,
            positionModule = classSymbol.containingModule,
            containingDeclarations = listOf(classDeclaration),
            position = classDeclaration.psi ?: return false,
        )
    }

    /**
     * 判断声明是否属于公开 API 表面。
     */
    override fun isPublicApi(symbol: CaDeclarationSymbol): Boolean = withValidityAssertion {
        symbol.visibility == CaSymbolVisibility.PUBLIC
    }
}

/**
 * 固定 use-site 上下文后可重复使用的 CFIR 可见性检查器。
 */
private class CaCfirUseSiteVisibilityChecker(
    /**
     * 使用点 PSI，决定局部声明、private 成员和 protected 成员的可见边界。
     */
    private val position: PsiElement,
    /**
     * 使用点所属模块。
     */
    private val positionModule: CaModule,
    /**
     * 使用点外层 CFIR 声明链。
     */
    private val containingDeclarations: List<CfirDeclaration>,
    /**
     * 使用点所在文件的公开 CFIR 文件符号。
     */
    private val useSiteFile: CaCfirFileSymbol,
    /**
     * 执行可见性判断所需的 Analysis session。
     */
    private val analysisSession: CaCfirSession,
    /**
     * 约束检查器生命周期的会话 token。
     */
    override val token: CaLifetimeToken,
) : CaUseSiteVisibilityChecker {
    /**
     * 判断候选声明符号在当前 use-site 是否可访问。
     */
    override fun isVisible(candidateSymbol: CaDeclarationSymbol): Boolean = withValidityAssertion {
        require(candidateSymbol is CaCfirSymbol<*>)

        val candidateDeclaration = candidateSymbol.cfirSymbol.cfir as? CfirMemberDeclaration ?: return true
        analysisSession.isDeclarationVisibleAtUseSite(
            candidateDeclaration = candidateDeclaration,
            useSiteFile = useSiteFile.file.getOrBuildCfirFile(analysisSession.resolutionFacade),
            positionModule = positionModule,
            containingDeclarations = containingDeclarations,
            position = position,
        )
    }
}

/**
 * 为 type-qualified CDoc 扩展成员查找保留 CFIR lookup provenance 的可见性检查器。
 *
 * 普通 [CaUseSiteVisibilityChecker] 接收最终公开符号，适用于不需要区分来源的通用声明检查。
 * 扩展成员的跨包导出面依赖其来源 extend 与 receiver 实例，必须在公开符号投影丢失 lookup
 * provenance 前把 CFIR 保留的完整来源交给编译器访问检查器。
 */
internal class CaCfirUseSiteMemberCallableVisibilityChecker(
    private val analysisSession: CaCfirSession,
    private val position: PsiElement,
    private val positionModule: CaModule,
    private val containingDeclarations: List<CfirDeclaration>,
    private val useSiteFile: CfirFile,
    private val receiverType: ConeCangJieType,
    override val token: CaLifetimeToken,
) : CaLifetimeOwner {
    /**
     * 用候选自身的 extend 来源和 receiver 上下文执行 CFIR 唯一的访问检查。
     */
    fun isVisible(candidate: CfirCallableWithLookupProvenance): Boolean = withValidityAssertion {
        val declaration = checkNotNull(candidate.symbol.cfir as? CfirMemberDeclaration) {
            "CFIR extend callable candidates must be backed by member declarations: ${candidate.symbol}"
        }
        if (declaration is CfirCallableDeclaration && declaration.isLocal) {
            return@withValidityAssertion isLocalDeclarationVisible(declaration, position)
        }

        val accessContext = CfirAccessContext(
            useSiteFile = useSiteFile,
            containingDeclarations = containingDeclarations,
            receiverType = receiverType,
            qualifierSymbol = null,
            lookupOrigin = CfirLookupOrigin.MEMBER,
            kind = CfirAccessKind.CALLABLE,
        )
        val targetSession = analysisSession.getTargetSession(positionModule, declaration)
        targetSession.accessibilityChecker.checkCallable(
            symbol = candidate.symbol,
            context = accessContext,
            provenance = candidate.provenance,
        ) is CfirAccessibilityResult.Accessible
    }
}

/**
 * 为 type-qualified 成员查找建立与普通 use-site visibility checker 相同的文件、模块和声明链上下文，
 * 但将 receiver 类型留给扩展成员的 provenance-aware 检查路径。
 */
internal fun CaCfirSession.createUseSiteMemberCallableVisibilityChecker(
    useSiteFile: CfirFile,
    receiverType: ConeCangJieType,
    position: PsiElement,
): CaCfirUseSiteMemberCallableVisibilityChecker {
    val positionModule = resolutionFacade.moduleProvider.getModule(position)
    val containingDeclarations = collectUseSiteContainers(position, resolutionFacade).orEmpty()
    return CaCfirUseSiteMemberCallableVisibilityChecker(
        analysisSession = this,
        position = position,
        positionModule = positionModule,
        containingDeclarations = containingDeclarations,
        useSiteFile = useSiteFile,
        receiverType = receiverType,
        token = token,
    )
}

/**
 * 按 CFIR 声明可见性和 use-site 上下文执行最终访问判定。
 *
 * Public/internal/private/protected 一律交给编译器唯一的可访问性服务，
 * Analysis 层不再维护第二套访问分支：两套规则迟早会漂移，而且成员可见性差异会被
 * 补全候选静默放大。这里只传入服务需要的完整 use-site 事实——文件、外围声明链、
 * 接收者类型和查找来源。
 *
 * 唯一保留在本层的规则是 [Visibilities.Local]：它是词法可见时机问题，
 * 编译器可访问性服务按 `else -> false` 处理，不表达局部声明的时间边界。
 */
private fun CaCfirSession.isDeclarationVisibleAtUseSite(
    candidateDeclaration: CfirMemberDeclaration,
    useSiteFile: CfirFile,
    positionModule: CaModule,
    containingDeclarations: List<CfirDeclaration>,
    position: PsiElement,
): Boolean {
    if (candidateDeclaration.status.visibility == Visibilities.Local) {
        return isLocalDeclarationVisible(candidateDeclaration, position)
    }

    val targetSession = getTargetSession(positionModule, candidateDeclaration)
    val symbol = candidateDeclaration.symbol
    val context = CfirAccessContext(
        useSiteFile = useSiteFile,
        containingDeclarations = containingDeclarations,
        receiverType = null,
        qualifierSymbol = null,
        lookupOrigin = CfirLookupOrigin.LEXICAL,
        kind = candidateDeclaration.accessKind(),
    )
    val result = when (symbol) {
        is CfirCallableSymbol<*> -> targetSession.accessibilityChecker.checkCallable(symbol, context)
        is CfirClassLikeSymbol<*> -> targetSession.accessibilityChecker.checkClassLike(symbol, context)
        else -> return true
    }
    return result is CfirAccessibilityResult.Accessible
}

/**
 * 按声明种类选择可访问性判定使用的语义入口。
 */
private fun CfirMemberDeclaration.accessKind(): CfirAccessKind = when (this) {
    is CfirClassLikeDeclaration -> CfirAccessKind.TYPE
    is CfirExtend -> CfirAccessKind.EXTEND
    is CfirFieldVariable -> CfirAccessKind.NAMED_VALUE
    else -> CfirAccessKind.CALLABLE
}

/**
 * 为候选声明选择执行可见性规则时应使用的 LL CFIR session。
 */
private fun CaCfirSession.getTargetSession(
    positionModule: CaModule,
    candidateDeclaration: CfirMemberDeclaration,
) = resolutionFacade.getSessionFor(
    when {
        positionModule is CaDanglingFileModule && candidateDeclaration.llCfirModuleData.caModule != positionModule -> positionModule.contextModule
        else -> positionModule
    }
)

/**
 * 判断局部声明在当前 PSI 位置是否仍处于可见 lexical 范围内。
 */
private fun isLocalDeclarationVisible(
    declaration: CfirMemberDeclaration,
    position: PsiElement,
): Boolean {
    val declarationPsi = declaration.psi ?: return false
    if (declarationPsi.containingFile != position.containingFile) return false

    val sharedLocalOwner = PsiTreeUtil.findFirstParent(position) { candidate ->
        candidate is CjDeclaration && PsiTreeUtil.isAncestor(candidate, declarationPsi, true)
    } as? CjDeclaration ?: return false

    return PsiTreeUtil.isAncestor(sharedLocalOwner, position, false)
}
