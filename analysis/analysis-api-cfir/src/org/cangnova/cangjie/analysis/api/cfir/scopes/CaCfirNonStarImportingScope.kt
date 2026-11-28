package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.cached
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.name.Name

/** 将已解析 simple import bindings 投影为 Analysis API 名称视图。 */
internal class CaCfirNonStarImportingScope(
    cfirScope: CfirExplicitSimpleImportingScope,
    builder: CaSymbolByCfirBuilder,
) : CaCfirBasedScope<CfirExplicitSimpleImportingScope>(cfirScope, builder) {
    private val imports by cached { cfirScope.resolvedImports }

    private val callableNames by cached {
        imports.asSequence()
            .filter { binding -> binding.targets.any { it is CfirResolvedImportTarget.Callable } }
            .mapTo(linkedSetOf()) { it.effectiveName }
    }

    private val classifierNames by cached {
        imports.asSequence()
            .filter { binding -> binding.targets.any { it is CfirResolvedImportTarget.ClassLike } }
            .mapTo(linkedSetOf()) { it.effectiveName }
    }

    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { callableNames }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { classifierNames }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { callableNames + classifierNames }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in callableNames || name in classifierNames
    }
}
