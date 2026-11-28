package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.Collections

/** peer 准备的真实类型域；接口已完成投影，alias callback 仅在准备期间读取并登记依赖。 */
class CfirExtendMemberPeerPreparationInput(
    val sources: CfirTypeAwareSupertypeSources,
    val structuralProvider: CfirTypeAwareSupertypeProvider,
    val ownerTargetType: ConeCangJieType?,
    val peerTargetType: ConeCangJieType?,
    ownerInterfaces: List<ConeCangJieType>,
    peerInterfaces: List<ConeCangJieType>,
    ownerParameters: List<CfirTypeParameterSymbol>,
    ownerBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>>,
    val aliasExpansion: (CfirTypeAlias) -> ConeCangJieType?,
) {
    val ownerInterfaces: List<ConeCangJieType> = Collections.unmodifiableList(ArrayList(ownerInterfaces))
    val peerInterfaces: List<ConeCangJieType> = Collections.unmodifiableList(ArrayList(peerInterfaces))
    val ownerParameters: List<CfirTypeParameterSymbol> = Collections.unmodifiableList(ArrayList(ownerParameters))
    val ownerBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>> = Collections.unmodifiableMap(ownerBounds.mapValues {
        Collections.unmodifiableList(ArrayList(it.value))
    })
    init { require(ownerParameters.toSet() == ownerBounds.keys) { "Peer normalization requires the original ordered owner bounds" } }
}
