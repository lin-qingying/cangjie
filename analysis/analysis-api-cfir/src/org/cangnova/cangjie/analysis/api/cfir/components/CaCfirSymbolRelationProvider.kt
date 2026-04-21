package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.publicSymbolCacheKeyOrNull
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirBackedSymbol
import org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol

/**
 * 符号关系组件。
 */
internal class CaCfirSymbolRelationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolRelationProvider {
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

            val backingSymbol = (this@directlyOverriddenSymbols as? CaCfirBackedSymbol<*>)
                ?.backingSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>
                ?: return@withValidityAssertion emptySequence()

            analysisSession.collectDirectlyOverriddenCallableSymbols(backingSymbol)
                .map(analysisSession::getPublicSymbol)
                .filterIsInstance<CaCallableSymbol>()
                .distinctStableCallables()
                .asSequence()
        }

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
        val memberScope = scopeQueries.queryMemberScope(ownerClassId) ?: return emptyList()

        return when (backingSymbol) {
            is CfirFunctionSymbol<*> -> buildList {
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
