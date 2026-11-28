package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.name.Name

/** 将已解析 star-import namespaces 投影为 Analysis API 名称视图。 */
internal class CaCfirStarImportingScope(
    cfirScope: CfirExplicitStarImportingScope,
    builder: CaSymbolByCfirBuilder,
    private val callableNames: Set<Name>,
    private val classifierNames: Set<Name>,
) : CaCfirBasedScope<CfirExplicitStarImportingScope>(cfirScope, builder) {
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion { callableNames }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion { classifierNames }

    override fun getAllPossibleNames(): Set<Name> = withValidityAssertion { callableNames + classifierNames }

    override fun mayContainName(name: Name): Boolean = withValidityAssertion {
        name in callableNames || name in classifierNames
    }
}
