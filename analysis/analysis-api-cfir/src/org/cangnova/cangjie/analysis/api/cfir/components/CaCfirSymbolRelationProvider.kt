package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirValueParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.isTopLevel
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.originalDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjModifierList
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import org.cangnova.cangjie.psi.psiUtil.parents
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 符号关系组件。
 */
internal class CaCfirSymbolRelationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolRelationProvider {
    override val CaSymbol.containingDeclaration: CaDeclarationSymbol?
        get() = withValidityAssertion {
            if (!hasParentSymbol(this@containingDeclaration)) {
                return@withValidityAssertion null
            }

            getContainingDeclarationForDependentDeclaration(this@containingDeclaration)?.let { return@withValidityAssertion it }

            val cfirSymbol = (this@containingDeclaration as? CaCfirSymbol<*>)?.cfirSymbol ?: return@withValidityAssertion null
            val symbolModule = cfirSymbol.llCfirModuleData.caModule

            if (symbolModule is CaDanglingFileModule && symbolModule.resolutionMode == CaDanglingFileResolutionMode.IGNORE_SELF) {
                if (hasParentPsi(this@containingDeclaration)) {
                    return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
                }
            }

            when (this@containingDeclaration) {
                is CaLocalVariableSymbol,
                is CaAnonymousFunctionSymbol,
                    -> {
                    return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
                }

                is CaCallableSymbol -> {
                    cfirSymbol.getContainingClassSymbol()?.let { outerClass ->
                        return@withValidityAssertion analysisSession.cfirSymbolBuilder.buildSymbol(outerClass) as? CaDeclarationSymbol
                    }
                }

                is CaClassLikeSymbol -> {
                    cfirSymbol.getContainingClassSymbol()?.let { outerClass ->
                        return@withValidityAssertion analysisSession.cfirSymbolBuilder.buildSymbol(outerClass) as? CaDeclarationSymbol
                    }
                }
            }

            return@withValidityAssertion getContainingDeclarationByPsi(this@containingDeclaration)
        }

    private fun getContainingDeclarationForDependentDeclaration(symbol: CaSymbol): CaDeclarationSymbol? {
        return when (symbol) {
            is CaPropertyAccessorSymbol -> symbol.owningProperty

            is CaCfirValueParameterSymbol ->
                analysisSession.cfirSymbolBuilder.buildSymbol(symbol.cfirSymbol.containingDeclarationSymbol) as? CaDeclarationSymbol

            is CaCfirTypeParameterSymbol ->
                analysisSession.cfirSymbolBuilder.buildSymbol(symbol.cfirSymbol.containingDeclarationSymbol) as? CaDeclarationSymbol

            else -> null
        }
    }

    private fun hasParentSymbol(symbol: CaSymbol): Boolean {
        return when (symbol) {
            is CaPackageSymbol,
            is CaFileSymbol,
                -> false

            !is CaDeclarationSymbol -> false
            else -> !symbol.isTopLevel
        }
    }

    private fun getContainingDeclarationByPsi(symbol: CaSymbol): CaDeclarationSymbol? {
        val containingDeclaration = getContainingPsi(symbol) ?: return null
        return with(analysisSession) { containingDeclaration.symbol }
    }

    private fun getContainingPsi(symbol: CaSymbol): CjDeclaration? {
        val source = (symbol as? CaCfirSymbol<*>)?.cfirSymbol?.cfir?.source
            ?: errorWithAttachment("PSI should present for declaration built by CangJie code") {
                withPsiEntry("symbolPsi", (symbol as? CaDeclarationSymbol)?.psi)
            }

        return getContainingPsi(symbol, source)
    }

    private fun getContainingPsi(symbol: CaSymbol, source: CjSourceElement): CjDeclaration? {
        getContainingPsiForFakeSource(source)?.let { return it }

        val psi = source.psi
            ?: errorWithAttachment("PSI not found for source kind '${source.kind}'") {}

        if (source.kind != CjRealSourceElementKind) {
            errorWithAttachment("Cannot compute containing PSI for unknown source kind '${source.kind}' (${psi::class.simpleName})") {
                withPsiEntry("psi", psi)
            }
        }

        if (isSyntheticSymbolWithParentSource(symbol)) {
            return psi as CjDeclaration
        }

        if (isOrdinarySymbolWithSource(symbol)) {
            val result = psi.getContainingPsiDeclaration()
            if (result == null) {
                val containingFile = psi.containingFile
                if (containingFile is CjCodeFragment) {
                    return null
                }

                if (psi.parentOfType<CjModifierList>() != null) {
                    return null
                }

                errorWithAttachment("Containing declaration should present for nested declaration ${psi::class}") {
                    withPsiEntry("psi", psi)
                }
            }

            return result
        }

        errorWithAttachment("Unsupported declaration origin ${symbol.origin} ${psi::class}") {
            withPsiEntry("psi", psi)
        }
    }

    private fun hasParentPsi(symbol: CaSymbol): Boolean {
        val source = (symbol as? CaCfirSymbol<*>)?.cfirSymbol?.cfir?.source?.takeIf { it.psi is CjElement } ?: return false

        return getContainingPsiForFakeSource(source) != null ||
            isSyntheticSymbolWithParentSource(symbol) ||
            isOrdinarySymbolWithSource(symbol)
    }

    private fun isSyntheticSymbolWithParentSource(symbol: CaSymbol): Boolean {
        return when (symbol.origin) {
            else -> false
        }
    }

    private fun isOrdinarySymbolWithSource(symbol: CaSymbol): Boolean {
        return symbol.origin == CaSymbolOrigin.SOURCE
    }

    private fun getContainingPsiForFakeSource(source: CjSourceElement): CjDeclaration? {
        return when (source.kind) {
            CjFakeSourceElementKind.ImplicitConstructor -> source.psi as CjDeclaration
            CjFakeSourceElementKind.PropertyFromParameter -> (source.psi as CjParameter).ownerFunction as? CjPrimaryConstructor
            CjFakeSourceElementKind.EnumGeneratedDeclaration -> source.psi as CjDeclaration
            CjFakeSourceElementKind.DataClassGeneratedMembers -> when (val psi = source.psi) {
                is CjTypeStatement -> psi
                is CjParameter -> (psi.ownerFunction as? CjPrimaryConstructor)?.getContainingTypeStatement()
                is CjPrimaryConstructor -> psi.getContainingTypeStatement()
                else -> null
            }
            else -> null
        }
    }

    private fun PsiElement.getContainingPsiDeclaration(): CjDeclaration? {
        for (parent in parents) {
            if (parent is CjDeclaration) {
                return parent.originalDeclaration ?: parent
            }
        }

        return null
    }

    override fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean = withValidityAssertion {
        this@isEquivalentTo === other ||
            (this@isEquivalentTo.publicSymbolCacheKeyOrNull() != null &&
                this@isEquivalentTo.publicSymbolCacheKeyOrNull() == other.publicSymbolCacheKeyOrNull())
    }

    override val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val backingSymbol = (this@directlyOverriddenSymbols as? CaCfirSymbol<*>)
                ?.cfirSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>
                ?: return@withValidityAssertion emptySequence()

            analysisSession.collectDirectlyOverriddenCallableSymbols(backingSymbol)
                .map { symbol -> analysisSession.cfirSymbolBuilder.buildSymbol(symbol) }
                .filterIsInstance<CaCallableSymbol>()
                .distinctStableCallables()
                .asSequence()
        }

    override val CaCallableSymbol.intersectionOverriddenSymbols: List<CaCallableSymbol>
        get() = withValidityAssertion { emptyList() }

    override val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val visited = linkedSetOf<String>()
            val result = mutableListOf<CaCallableSymbol>()
            val queue = ArrayDeque(this@allOverriddenSymbols.directlyOverriddenSymbols.toList())

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val key = current.stableCallableIdentity() ?: continue
                if (!visited.add(key)) continue
                result += current
                queue.addAll(current.directlyOverriddenSymbols)
            }

            result.asSequence()
        }

    override fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = true)
    }

    override fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = false)
    }

    override fun CaDeclarationSymbol.getExpectsForActual(): List<CaDeclarationSymbol> = withValidityAssertion {
        emptyList()
    }

    private fun CaCallableSymbol.mayHaveOverriddenSymbols(): Boolean {
        return this is org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol ||
            this is org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
    }

    private fun List<CaCallableSymbol>.distinctStableCallables(): List<CaCallableSymbol> {
        return distinctBy { callable ->
            callable.stableCallableIdentity() ?: "${callable::class.qualifiedName}@${System.identityHashCode(callable)}"
        }
    }

    private fun CaCallableSymbol.stableCallableIdentity(): String? {
        return publicSymbolCacheKeyOrNull()?.toString() ?: callableId?.toString()
    }

    private fun CaClassSymbol.isSubclassOf(
        superClass: CaClassSymbol,
        allowIndirect: Boolean,
    ): Boolean {
        if (isSameClassAs(superClass)) {
            return false
        }

        val directSuperSymbols = superTypes.mapNotNull { type ->
            with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
        }
        if (directSuperSymbols.any { symbol -> symbol.isSameClassAs(superClass) }) {
            return true
        }
        if (!allowIndirect) {
            return false
        }

        val visited = linkedSetOf<String>()
        val queue = ArrayDeque(directSuperSymbols)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current.classRelationIdentity())) continue
            if (current.isSameClassAs(superClass)) {
                return true
            }
            queue.addAll(current.superTypes.mapNotNull { type ->
                with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
            })
        }
        return false
    }

    /**
     * subclass relation 既要覆盖带 `ClassId` 的稳定声明，也要覆盖当前 session 中仅由源码承载的局部类。
     *
     * 因此这里统一按“ClassId 优先，其次源码 PSI 身份”来比较两个 class symbol，
     * 避免把 local class 关系硬退化成一律 `false`。
     */
    private fun CaClassSymbol.isSameClassAs(other: CaClassSymbol): Boolean {
        val thisClassId = classId
        val otherClassId = other.classId
        if (thisClassId != null && otherClassId != null) {
            return thisClassId == otherClassId
        }

        val thisPsi = psi
        val otherPsi = other.psi
        return thisPsi != null && otherPsi != null && thisPsi == otherPsi
    }

    private fun CaClassSymbol.classRelationIdentity(): String {
        classId?.let { return "classId:${it.asString()}" }

        val declarationPsi = psi
        if (declarationPsi != null) {
            val filePath = declarationPsi.containingFile?.virtualFile?.path ?: declarationPsi.containingFile?.name.orEmpty()
            return "psi:$filePath:${declarationPsi.textOffset}"
        }

        return "${this::class.qualifiedName}@${System.identityHashCode(this)}"
    }

    private fun CaCfirSession.collectDirectlyOverriddenCallableSymbols(
        backingSymbol: org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>,
    ): List<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>> {
        val ownerClassId = backingSymbol.callableId.classId
            ?: cfirSession.cfirProvider.getContainingClass(backingSymbol)?.classId
            ?: return emptyList()
        val ownerClass = cfirSession.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirClass
            ?: return emptyList()
        val memberScope = cfirSession.cangjieScopeProvider.getUseSiteMemberScope(
            ownerClass,
            cfirSession,
            getScopeSessionFor(cfirSession),
        )

        return when (backingSymbol) {
            is CfirNamedFunctionSymbol -> buildList {
                memberScope.processDirectOverriddenFunctionsWithBaseScope(backingSymbol) { overridden, _ ->
                    add(overridden)
                    ProcessorAction.NEXT
                }
            }

            is CfirPropertySymbol -> buildList {
                memberScope.processDirectOverriddenPropertiesWithBaseScope(backingSymbol) { overridden, _ ->
                    add(overridden)
                    ProcessorAction.NEXT
                }
            }

            else -> emptyList()
        }
    }
}
