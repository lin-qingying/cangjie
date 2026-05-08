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
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
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

    override fun isPublicApi(symbol: CaDeclarationSymbol): Boolean = withValidityAssertion {
        symbol.visibility == CaSymbolVisibility.PUBLIC
    }
}

private class CaCfirUseSiteVisibilityChecker(
    private val position: PsiElement,
    private val positionModule: CaModule,
    private val containingDeclarations: List<CfirDeclaration>,
    private val useSiteFile: CaCfirFileSymbol,
    private val analysisSession: CaCfirSession,
    override val token: CaLifetimeToken,
) : CaUseSiteVisibilityChecker {
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

private fun CaCfirSession.getTargetSession(
    positionModule: CaModule,
    candidateDeclaration: CfirMemberDeclaration,
) = resolutionFacade.getSessionFor(
    when {
        positionModule is CaDanglingFileModule && candidateDeclaration.llCfirModuleData.caModule != positionModule -> positionModule.contextModule
        else -> positionModule
    }
)

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

private fun canSeePrivateTopLevelDeclarationFromFile(
    useSiteFile: CfirFile,
    declaration: CfirMemberDeclaration,
    session: org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession,
): Boolean {
    val declarationContainingFile = session.cfirProvider.getContainingFile(declaration.symbol) ?: return false
    return useSiteFile == declarationContainingFile
}

private fun canSeeMemberOf(
    ownerClassId: ClassId,
    containingDeclarations: List<CfirDeclaration>,
): Boolean {
    return containingDeclarations.asSequence()
        .filterIsInstance<CfirClassLikeDeclaration>()
        .map { it.symbol.classId }
        .any { it == ownerClassId }
}

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
