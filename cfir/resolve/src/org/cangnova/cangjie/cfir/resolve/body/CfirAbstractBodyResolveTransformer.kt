package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeCheckerContext
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirOverloadConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceComponents
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * Body 解析 transformer 抽象基类。
 *
 * 定义 body resolve 阶段的三个核心抽象属性：
 * - [context]：Body 解析上下文（scope 塔、文件、容器栈）
 * - [components]：共享组件容器（session、call resolver、tower resolver 等）
 *
 * session 统一从 components 获取，避免各子组件直接互相持有引用。
 *
 * 参考 K2 FirAbstractBodyResolveTransformer。
 */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<CfirResolutionMode>(phase) {

    abstract val context: CfirBodyResolveContext

    abstract val components: BodyResolveTransformerComponents

    final override val session: CfirSession get() = components.session

    /**
     * 共享组件容器，所有 body resolve 子组件通过此容器协作。
     *
     * 持有 session、scopeSession、context 引用，
     * 以及懒初始化的 callResolver、towerResolver 等。
     *
     * 参考 K2 FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents。
     */
    open class BodyResolveTransformerComponents(
        override val session: CfirSession,
        val scopeSession: CfirScopeSession,
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        val context: CfirBodyResolveContext,
    ) : CfirSessionHolder {

        /** scope 塔上下文 — 委托到 context */
        val towerDataContext get() = context.towerDataContext

        /** 返回类型计算器 — 委托到 context */
        val returnTypeCalculator: CfirReturnTypeCalculator get() = context.returnTypeCalculator

        /** 符号提供器 — 委托到 session */
        val symbolProvider get() = session.symbolProvider

        /** 候选验证管线执行器 — 即时初始化（无状态，轻量） */
        val resolutionStageRunner: CfirResolutionStageRunner = CfirResolutionStageRunner()

        /** 子类型检查器 — 懒初始化 */
        val subtypeChecker: ConeSubtypeChecker by lazy(LazyThreadSafetyMode.NONE) {
            ConeSubtypeChecker(CfirTypeCheckerContext(session))
        }

        /** 重载冲突解析器 — 懒初始化 */
        val conflictResolver: CfirCallConflictResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirOverloadConflictResolver(subtypeChecker)
        }

        /** 推断组件 — 懒初始化（Phase 4 泛型推断） */
        val inferenceComponents: CfirInferenceComponents by lazy(LazyThreadSafetyMode.NONE) {
            CfirInferenceComponents(subtypeChecker)
        }

        /** 解析上下文 — 懒初始化（用于 Phase 3 验证阶段管线） */
        val resolutionContext: CfirResolutionContext? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                CfirResolutionContext(session, context, subtypeChecker, inferenceComponents)
            } catch (_: Exception) {
                null // 如果 typeContext 不可用，回退到旧版解析
            }
        }

        /** Tower 解析器 — 懒初始化 */
        val towerResolver: CfirTowerResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirTowerResolver(this, resolutionStageRunner)
        }

        /** 调用解析器 — 懒初始化 */
        val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this).also { resolver ->
                resolver.conflictResolver = conflictResolver
            }
        }

        /** Extend 声明提供器 — 懒初始化（Phase 4 extend 成员查找） */
        val extendProvider: CfirExtendProvider? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                session.extendProvider
            } catch (_: Exception) {
                null // session 中未注册 extendProvider 时回退
            }
        }
    }
}

/**
 * Body 解析 dispatcher 抽象基类。
 *
 * 作为具体 dispatcher（如 [CfirBodyResolveTransformer]）的基类，
 * 持有 context 和 components 的所有权。
 * 所有 transformXxx 方法委托到对应的子 transformer。
 *
 * 参考 K2 FirAbstractBodyResolveTransformerDispatcher。
 */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
    /** 仅推断隐式类型（true = IMPLICIT_TYPES 阶段，false = BODY_RESOLVE 阶段） */
    open val implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformer(phase) {

    abstract override val context: CfirBodyResolveContext

    abstract override val components: BodyResolveTransformerComponents

    /** 表达式子 transformer */
    abstract val expressionsTransformer: CfirExpressionsResolveTransformer

    /** 声明子 transformer */
    abstract val declarationsTransformer: CfirDeclarationsResolveTransformer

    /**
     * 声明内容变换钩子。
     *
     * 默认直接委托到 [declarationsTransformer]，
     * 子类（如 [CfirDesignatedBodyResolveTransformer]）可覆写以实现指定路径遍历。
     *
     * 参考 K2 FirAbstractBodyResolveTransformerDispatcher.transformDeclarationContent。
     */
    open fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: CfirResolutionMode,
    ): CfirDeclaration {
        return declaration.transform(this, data)
    }

    // ---- 默认 transformElement ----

    override fun <E : CfirElement> transformElement(element: E, data: CfirResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

    // ---- 声明委托到 declarationsTransformer ----

    override fun transformFile(file: CfirFile, data: CfirResolutionMode): CfirFile {
        checkSessionConsistency(file)
        return declarationsTransformer.transformFile(file, data)
    }

    override fun transformClass(klass: CfirClass, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformClass(klass, data)
    }

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformVariable(variable, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    // ---- block 委托到 declarationsTransformer（需要 scope 管理） ----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        return declarationsTransformer.transformBlock(block, data)
    }

    // ---- 表达式委托到 expressionsTransformer ----

    override fun transformExpression(expression: CfirExpression, data: CfirResolutionMode): CfirExpression {
        return expressionsTransformer.transformExpression(expression, data) as CfirExpression
    }

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLiteralExpression(literalExpression, data)
    }

    override fun transformPropertyAccess(
        propertyAccess: CfirPropertyAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformPropertyAccess(propertyAccess, data)
    }

    override fun transformQualifiedAccess(
        qualifiedAccess: CfirQualifiedAccess,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformQualifiedAccess(qualifiedAccess, data)
    }

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIfExpression(ifExpression, data)
    }

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformReturnExpression(returnExpression, data)
    }

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformAssignment(assignment, data)
    }

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTupleLiteral(tupleLiteral, data)
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformArrayLiteral(arrayLiteral, data)
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformStringInterpolation(stringInterpolation, data)
    }

    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformMatchExpression(matchExpression, data)
    }

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformErrorExpression(errorExpression, data)
    }
}
