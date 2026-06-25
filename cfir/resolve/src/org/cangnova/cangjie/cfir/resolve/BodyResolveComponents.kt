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
    /** 当前 body resolve 使用的返回类型计算器。 */
    abstract val returnTypeCalculator: ReturnTypeCalculator
    /** 隐式接收者和值的存储。 */
    abstract val implicitValueStorage: ImplicitValueStorage
    /** 当前声明栈，从文件/容器到正在解析的声明。 */
    abstract val containingDeclarations: List<CfirDeclaration>
    /** 当前文件 imports 构造出的可见 scope 列表。 */
    abstract val fileImportsScope: List<CfirScope>
    /** 当前 tower data 元素栈。 */
    abstract val towerDataElements: List<CfirTowerDataElement>
    /** 当前 tower data 上下文。 */
    abstract val towerDataContext: CfirTowerDataContext
    /** 当前局部作用域集合。 */
    abstract val localScopes: CfirLocalScopes
    /** 表示无期望类型的共享 type ref。 */
    abstract val noExpectedType: CfirTypeRef
    /** 当前 session 的符号提供器。 */
    abstract val symbolProvider: CfirSymbolProvider
    /** 正在解析的文件。 */
    abstract val file: CfirFile
    /** 当前 body resolve 容器声明。 */
    abstract val container: CfirDeclaration
    /** 调用解析阶段 runner。 */
    abstract val resolutionStageRunner: ResolutionStageRunner
    /** SAM 转换解析器。 */
    abstract val samResolver: CfirSamResolver
    /** 普通调用解析器。 */
    abstract val callResolver: CfirCallResolver
    /** 调用完成器，负责约束系统 completion。 */
    abstract val callCompleter: CfirCallCompleter
//    abstract val doubleColonExpressionResolver: CfirDoubleColonExpressionResolver
    /** 合成调用生成器。 */
    abstract val syntheticCallGenerator: CfirSyntheticCallGenerator
    /** body resolve 使用的数据流分析器。 */
    abstract val dataFlowAnalyzer: CfirDataFlowAnalyzer
//    abstract val outerClassManager: CfirOuterClassManager
    /** 整数字面量与 primitive operator 近似 transformer。 */
    abstract val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
    /** 当前 inline 函数上下文；非 inline 路径为 null。 */
    abstract val inlineFunction: CfirFunction?
}

/**
 * 对齐 Kotlin `BodyResolveComponents.createCurrentScopeList()`：
 * body resolve 内部的类型解析统一从当前 tower-data 可见 scope 线性视图取上下文。
 */
fun BodyResolveComponents.createCurrentScopeList(): List<CfirScope> =
    towerDataElements.asReversed().mapNotNull { it.scope }
