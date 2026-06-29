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
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.calls.visibility.getOwnerClassId
import org.cangnova.cangjie.cfir.resolve.calls.visibility.moduleVisibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.canAccessPackageInternalDeclaration
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjDeclaration
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
        val useSiteFile = analysisSession.cfirSession.cfirProvider.getContainingFile(classDeclaration.symbol) ?: return false

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
 * 按 CFIR 声明可见性和 use-site 上下文执行最终访问判定。
 */
private fun CaCfirSession.isDeclarationVisibleAtUseSite(
    candidateDeclaration: CfirMemberDeclaration,
    useSiteFile: CfirFile,
    positionModule: CaModule,
    containingDeclarations: List<CfirDeclaration>,
    position: PsiElement,
): Boolean {
    val targetSession = getTargetSession(positionModule, candidateDeclaration)

    return when (candidateDeclaration.status.visibility) {
        Visibilities.Public -> true
        Visibilities.Internal -> canSeePackageInternalDeclaration(useSiteFile, candidateDeclaration, targetSession)
        Visibilities.Private -> {
            val ownerClassId = candidateDeclaration.symbol.getOwnerClassId(targetSession.cfirProvider)
            when (ownerClassId) {
                null -> canSeePrivateTopLevelDeclarationFromFile(useSiteFile, candidateDeclaration, targetSession)
                else -> canSeeMemberOf(ownerClassId, containingDeclarations)
            }
        }
        Visibilities.Protected -> {
            candidateDeclaration.llCfirModuleData.caModule == targetSession.caModule ||
                targetSession.moduleVisibilityChecker?.isInFriendModule(candidateDeclaration) == true ||
                candidateDeclaration.symbol.getOwnerClassId(targetSession.cfirProvider)?.let { ownerClassId ->
                    canSeeMemberOf(ownerClassId, containingDeclarations)
                } == true
        }
        Visibilities.Local -> isLocalDeclarationVisible(candidateDeclaration, position)
        else -> true
    }
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
 * 判断使用点包是否允许访问目标 package-internal 声明。
 */
private fun canSeePackageInternalDeclaration(
    useSiteFile: CfirFile,
    declaration: CfirMemberDeclaration,
    session: org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession,
): Boolean {
    val declarationContainingFile = session.cfirProvider.getContainingFile(declaration.symbol) ?: return false
    val declarationPackage = declarationContainingFile.packageDirective.packageFqName
    val useSitePackage = useSiteFile.packageDirective.packageFqName
    return canAccessPackageInternalDeclaration(useSitePackage, declarationPackage)
}

/**
 * 判断 private 顶层声明是否与使用点处于同一个 CFIR 文件。
 */
private fun canSeePrivateTopLevelDeclarationFromFile(
    useSiteFile: CfirFile,
    declaration: CfirMemberDeclaration,
    session: org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession,
): Boolean {
    val declarationContainingFile = session.cfirProvider.getContainingFile(declaration.symbol) ?: return false
    return useSiteFile == declarationContainingFile
}

/**
 * 判断 use-site 声明链是否位于指定 owner class 内部。
 */
private fun canSeeMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId }
}

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
