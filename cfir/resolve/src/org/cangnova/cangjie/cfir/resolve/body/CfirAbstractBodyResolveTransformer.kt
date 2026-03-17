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
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * Body 瑙ｆ瀽 transformer 鎶借薄鍩虹被銆? *
 * 瀹氫箟 body resolve 闃舵鐨勪笁涓牳蹇冩娊璞″睘鎬э細
 * - [context]锛欱ody 瑙ｆ瀽涓婁笅鏂囷紙scope 濉斻€佹枃浠躲€佸鍣ㄦ爤锛? * - [components]锛氬叡浜粍浠跺鍣紙session銆乧all resolver銆乼ower resolver 绛夛級
 *
 * session 缁熶竴浠?components 鑾峰彇锛岄伩鍏嶅悇瀛愮粍浠剁洿鎺ヤ簰鐩告寔鏈夊紩鐢ㄣ€? *
 * 鍙傝€?K2 FirAbstractBodyResolveTransformer銆? */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<CfirResolutionMode>(phase) {

    abstract val context: CfirBodyResolveContext

    abstract val components: BodyResolveTransformerComponents

    final override val session: CfirSession get() = components.session

    /**
     * 鍏变韩缁勪欢瀹瑰櫒锛屾墍鏈?body resolve 瀛愮粍浠堕€氳繃姝ゅ鍣ㄥ崗浣溿€?     *
     * 鎸佹湁 session銆乻copeSession銆乧ontext 寮曠敤锛?     * 浠ュ強鎳掑垵濮嬪寲鐨?callResolver銆乼owerResolver 绛夈€?     *
     * 鍙傝€?K2 FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents銆?     */
    open class BodyResolveTransformerComponents(
        override val session: CfirSession,
        val scopeSession: CfirScopeSession,
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        val context: CfirBodyResolveContext,
    ) : CfirSessionHolder {

        /** scope 濉斾笂涓嬫枃 鈥?濮旀墭鍒?context */
        val towerDataContext get() = context.towerDataContext

        /** 杩斿洖绫诲瀷璁＄畻鍣?鈥?濮旀墭鍒?context */
        val returnTypeCalculator: CfirReturnTypeCalculator get() = context.returnTypeCalculator

        /** 绗﹀彿鎻愪緵鍣?鈥?濮旀墭鍒?session */
        val symbolProvider get() = session.symbolProvider

        /** 鍊欓€夐獙璇佺绾挎墽琛屽櫒 鈥?鍗虫椂鍒濆鍖栵紙鏃犵姸鎬侊紝杞婚噺锛?*/
        val resolutionStageRunner: CfirResolutionStageRunner = CfirResolutionStageRunner()

        /** 瀛愮被鍨嬫鏌ュ櫒 鈥?鎳掑垵濮嬪寲 */
        val subtypeChecker: ConeSubtypeChecker by lazy(LazyThreadSafetyMode.NONE) {
            ConeSubtypeChecker(CfirTypeCheckerContext(session))
        }

        /** 閲嶈浇鍐茬獊瑙ｆ瀽鍣?鈥?鎳掑垵濮嬪寲 */
        val conflictResolver: CfirCallConflictResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirOverloadConflictResolver(subtypeChecker)
        }

        /** 鎺ㄦ柇缁勪欢 鈥?鎳掑垵濮嬪寲锛圥hase 4 娉涘瀷鎺ㄦ柇锛?*/
        val inferenceComponents: CfirInferenceComponents by lazy(LazyThreadSafetyMode.NONE) {
            CfirInferenceComponents(subtypeChecker, session.inferenceLogger)
        }

        /** 瑙ｆ瀽涓婁笅鏂?鈥?鎳掑垵濮嬪寲锛堢敤浜?Phase 3 楠岃瘉闃舵绠＄嚎锛?*/
        val resolutionContext: CfirResolutionContext? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                CfirResolutionContext(session, context, subtypeChecker, inferenceComponents)
            } catch (_: Exception) {
                // 濡傛灉 typeContext 涓嶅彲鐢紝鍥為€€鍒版棫鐗堣В鏋
                null
            }
        }

        /** Tower 瑙ｆ瀽鍣?鈥?鎳掑垵濮嬪寲 */
        val towerResolver: CfirTowerResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirTowerResolver(this, resolutionStageRunner)
        }

        /** 璋冪敤瑙ｆ瀽鍣?鈥?鎳掑垵濮嬪寲 */
        val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this).also { resolver ->
                resolver.conflictResolver = conflictResolver
            }
        }

        /** Extend 澹版槑鎻愪緵鍣?鈥?鎳掑垵濮嬪寲锛圥hase 4 extend 鎴愬憳鏌ユ壘锛?*/
        val extendProvider: CfirExtendProvider? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                session.extendProvider
            } catch (_: Exception) {
                // session 涓湭娉ㄥ唽 extendProvider 鏃跺洖閫€
                null
            }
        }
    }
}

/**
 * Body 瑙ｆ瀽 dispatcher 鎶借薄鍩虹被銆? *
 * 浣滀负鍏蜂綋 dispatcher锛堝 [CfirBodyResolveTransformer]锛夌殑鍩虹被锛? * 鎸佹湁 context 鍜?components 鐨勬墍鏈夋潈銆? * 鎵€鏈?transformXxx 鏂规硶濮旀墭鍒板搴旂殑瀛?transformer銆? *
 * 鍙傝€?K2 FirAbstractBodyResolveTransformerDispatcher銆? */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
    /** 浠呮帹鏂殣寮忕被鍨嬶紙true = IMPLICIT_TYPES 闃舵锛宖alse = BODY_RESOLVE 闃舵锛?*/
    open val implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformer(phase) {

    abstract override val context: CfirBodyResolveContext

    abstract override val components: BodyResolveTransformerComponents

    /** 琛ㄨ揪寮忓瓙 transformer */
    abstract val expressionsTransformer: CfirExpressionsResolveTransformer

    /** 澹版槑瀛?transformer */
    abstract val declarationsTransformer: CfirDeclarationsResolveTransformer

    /**
     * 澹版槑鍐呭鍙樻崲閽╁瓙銆?     *
     * 榛樿鐩存帴濮旀墭鍒?[declarationsTransformer]锛?     * 瀛愮被锛堝 [CfirDesignatedBodyResolveTransformer]锛夊彲瑕嗗啓浠ュ疄鐜版寚瀹氳矾寰勯亶鍘嗐€?     *
     * 鍙傝€?K2 FirAbstractBodyResolveTransformerDispatcher.transformDeclarationContent銆?     */
    open fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: CfirResolutionMode,
    ): CfirDeclaration {
        return declaration.transform(this, data)
    }

    // ---- 榛樿 transformElement ----

    override fun <E : CfirElement> transformElement(element: E, data: CfirResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

    // ---- 澹版槑濮旀墭鍒 declarationsTransformer ----

    override fun transformFile(file: CfirFile, data: CfirResolutionMode): CfirFile {
        checkSessionConsistency(file)
        return declarationsTransformer.transformFile(file, data)
    }

    override fun transformClass(klass: CfirClass, data: CfirResolutionMode): CfirClass {
        return declarationsTransformer.transformClass(klass, data)
    }

    override fun transformFunction(function: CfirFunction, data: CfirResolutionMode): CfirFunction {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformProperty(property: CfirProperty, data: CfirResolutionMode): CfirProperty {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformVariable(variable: CfirVariable, data: CfirResolutionMode): CfirVariable {
        return declarationsTransformer.transformVariable(variable, data)
    }

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: CfirResolutionMode,
    ): CfirPatternVariable {
        return declarationsTransformer.transformPatternVariable(patternVariable, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: CfirResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    // ---- block 濮旀墭鍒?declarationsTransformer锛堥渶瑕?scope 绠＄悊锛?----

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        return declarationsTransformer.transformBlock(block, data)
    }

    // ---- 琛ㄨ揪寮忓鎵樺埌 expressionsTransformer ----

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

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformComparisonExpression(comparisonExpression, data)
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformBinaryOp(binaryOp, data)
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTypeOperator(typeOperator, data)
    }

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformForInExpression(forInExpression, data)
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLoopExpression(loopExpression, data)
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformThrowExpression(throwExpression, data)
    }

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTryExpression(tryExpression, data)
    }

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSubscriptExpression(subscriptExpression, data)
    }

    override fun transformLambdaExpression(
        lambdaExpression: CfirLambdaExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLambdaExpression(lambdaExpression, data)
    }

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformRangeExpression(rangeExpression, data)
    }

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: CfirResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSpawnExpression(spawnExpression, data)
    }
}

