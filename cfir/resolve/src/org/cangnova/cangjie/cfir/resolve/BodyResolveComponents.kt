package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirLocalScopes
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSyntheticCallGenerator
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/** 只持有 session 与 scope session 的轻量实现。 */
data class SessionHolderImpl(
    /** 当前 CFIR session。 */
    override val session: CfirSession,
    /** 当前解析使用的 scope session。 */
    override val scopeSession: ScopeSession,
) : SessionAndScopeSessionHolder {
    companion object {
        /** 使用新的空 [ScopeSession] 为指定 session 创建 holder。 */
        fun createWithEmptyScopeSession(session: CfirSession): SessionHolderImpl = SessionHolderImpl(session, ScopeSession())
    }
}

/**
 * body resolve 阶段共享组件集合。
 *
 * transformer、call resolver、type resolver 与 data-flow analyzer 都从这里读取当前文件、容器、
 * tower data、局部作用域和候选检查组件，确保同一次 body resolve 使用一致上下文。
 */
abstract class BodyResolveComponents : SessionAndScopeSessionHolder {
    abstract val returnTypeCalculator: ReturnTypeCalculator
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
    abstract val resolutionStageRunner: ResolutionStageRunner
    abstract val samResolver: CfirSamResolver
    abstract val callResolver: CfirCallResolver
    abstract val callCompleter: CfirCallCompleter
//    abstract val doubleColonExpressionResolver: CfirDoubleColonExpressionResolver
    abstract val syntheticCallGenerator: CfirSyntheticCallGenerator
    abstract val dataFlowAnalyzer: CfirDataFlowAnalyzer
//    abstract val outerClassManager: CfirOuterClassManager
    abstract val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
    abstract val inlineFunction: CfirFunction?
}

/**
 * 对齐 Kotlin `BodyResolveComponents.createCurrentScopeList()`：
 * body resolve 内部的类型解析统一从当前 tower-data 可见 scope 线性视图取上下文。
 */
fun BodyResolveComponents.createCurrentScopeList(): List<CfirScope> =
    towerDataElements.asReversed().mapNotNull { it.scope }
