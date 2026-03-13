package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol

data class CfirSuperTypeGraphEdge(
    val renderedType: String,
    val resolvedClassSymbol: CfirClassSymbol?,
)

data class CfirSuperTypeGraphNode(
    val owner: CfirClassSymbol,
    val superTypes: List<CfirSuperTypeGraphEdge>,
)

class CfirSuperTypeGraphStore : CfirSessionComponent {
    private val nodesByOwner = mutableMapOf<CfirClassSymbol, CfirSuperTypeGraphNode>()

    fun record(owner: CfirClass, superTypes: List<CfirSuperTypeGraphEdge>) {
        nodesByOwner[owner.symbol] = CfirSuperTypeGraphNode(
            owner = owner.symbol,
            superTypes = superTypes,
        )
    }

    fun getNode(owner: CfirClass): CfirSuperTypeGraphNode? = nodesByOwner[owner.symbol]
}
