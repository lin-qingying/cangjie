package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirLocalScopes
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.body.CfirReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.types.CfirTypeRef

abstract class BodyResolveComponents : SessionAndScopeSessionHolder {
    abstract val returnTypeCalculator: CfirReturnTypeCalculator
    abstract val implicitValueStorage: ImplicitValueStorage
    abstract val containingDeclarations: List<CfirDeclaration>
    abstract val fileImportsScope: List<CfirScope>
    abstract val towerDataElements: List<CfirTowerDataElement>
    abstract val towerDataContext: CfirTowerDataContext
    abstract val localScopes: CfirLocalScopes
    abstract val noExpectedType: CfirTypeRef
    abstract val symbolProvider: CfirSymbolProvider
    abstract val file: CfirFile
    abstract val container: CfirDeclaration
//    abstract val resolutionStageRunner: ResolutionStageRunner
    abstract val samResolver: CfirSamResolver
    abstract val callResolver: CfirCallResolver
    abstract val callCompleter: CfirCallCompleter
//    abstract val doubleColonExpressionResolver: CfirDoubleColonExpressionResolver
//    abstract val syntheticCallGenerator: CfirSyntheticCallGenerator
    abstract val dataFlowAnalyzer: CfirDataFlowAnalyzer
//    abstract val outerClassManager: CfirOuterClassManager
    abstract val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
    abstract val inlineFunction: CfirFunction?
}
