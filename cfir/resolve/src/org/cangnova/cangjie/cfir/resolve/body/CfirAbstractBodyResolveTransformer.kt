package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CfirResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeCheckerContext
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirOverloadConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.resolve.CfirInferenceComponents
import org.cangnova.cangjie.cfir.resolve.CfirTypeRelations
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import kotlinx.collections.immutable.toPersistentList

/**
 * Body resolve transformer 的抽象基类。
 * 子类通过 `context` 与 `components` 协作，统一驱动 body resolve 流程。
 */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<CfirResolutionMode>(phase) {

    abstract val context: CfirBodyResolveContext

    abstract val components: BodyResolveTransformerComponents

    final override val session: CfirSession get() = components.session

    /**
     * 共享组件容器，集中持有 body resolve 所需的会话、上下文和服务。
     */
    open class BodyResolveTransformerComponents(
        override val session: CfirSession,
        override val scopeSession: ScopeSession,
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        val context: CfirBodyResolveContext,
    ) : BodyResolveComponents() {
        override val containingDeclarations: List<CfirDeclaration>
            get() = context.containers.toList()
        override val fileImportsScope: List<CfirScope>
            get() = createImportingScopes(context.file)
        override val towerDataElements: List<CfirTowerDataElement>
            get() = context.towerDataContext.towerDataElements

        override val towerDataContext get() = context.towerDataContext
        override val localScopes: CfirLocalScopes
            get() = context.towerDataContext.localScopes.toPersistentList()
        override val noExpectedType: CfirTypeRef
            get() = buildErrorTypeRef { reason = "No expected type" }

        val returnTypeCalculator: CfirReturnTypeCalculator get() = context.returnTypeCalculator

        override val symbolProvider get() = session.symbolProvider
        override val file: CfirFile
            get() = context.file
        override val container: CfirDeclaration
            get() = context.containerIfAny ?: context.file

        val resolutionStageRunner: CfirResolutionStageRunner = CfirResolutionStageRunner()

        private val typeCheckerContext: CfirTypeCheckerContext by lazy(LazyThreadSafetyMode.NONE) {
            CfirTypeCheckerContext(session)
        }

        val typeRelations: CfirTypeRelations by lazy(LazyThreadSafetyMode.NONE) {
            CfirTypeRelations(typeCheckerContext)
        }

        val conflictResolver: CfirCallConflictResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirOverloadConflictResolver(typeRelations)
        }

        val inferenceComponents: CfirInferenceComponents by lazy(LazyThreadSafetyMode.NONE) {
            CfirInferenceComponents(session, typeCheckerContext)
        }
        override val callCompleter: CfirCallCompleter by lazy(LazyThreadSafetyMode.NONE) { CfirCallCompleter(transformer, this) }
        override val inlineFunction: CfirFunction?
            get() = context.containers.lastOrNull() as? CfirFunction


        val resolutionContext: CfirResolutionContext? by lazy(LazyThreadSafetyMode.NONE) {
            createResolutionContext()
        }

        fun createResolutionContext(expectedType: org.cangnova.cangjie.cfir.types.ConeCangJieType? = null): CfirResolutionContext? {
            return try {
                CfirResolutionContext(
                    session = session,
                    bodyResolveContext = context,
                    typeRelations = typeRelations,
                    inferenceComponents = inferenceComponents,
                    expectedType = expectedType,
                    containingFilePath = context.file.sourceFile?.path,
                    containingPackageFqName = context.file.packageDirective.packageFqName,
                )
            } catch (e: Exception) {
                // 记录异常信息以便诊断，但不阻止编译继续
                System.err.println("[CFIR] createResolutionContext failed: ${e::class.simpleName}: ${e.message}")
                null
            }
        }

        val towerResolver: CfirTowerResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirTowerResolver(this, resolutionStageRunner)
        }

        override val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this).also { resolver ->
                resolver.conflictResolver = conflictResolver
            }
        }

        val extendProvider: CfirExtendProvider? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                session.extendProvider
            } catch (_: Exception) {
                null
            }
        }

        private fun createImportingScopes(file: CfirFile): List<CfirScope> {
            val imports = file.imports
            val defaultImportsProvider = session.defaultImportsProvider
            val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
                .filter { it.fqName !in defaultImportsProvider.excludedImports }
                .map { importPath ->
                    buildImport {
                        source = null
                        importedFqName = importPath.fqName
                        isAllUnder = importPath.isAllUnder
                        aliasName = importPath.alias
                        aliasSource = null
                    }
                }

            return buildList {
                add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
                add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
                add(CfirExplicitStarImportingScope(imports, symbolProvider))
                add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
            }
        }
    }
}

/**
 * Body resolve dispatcher 的抽象基类。
 * 具体 dispatcher 通过它把各种 `transformXxx` 分发给对应子 transformer。
 */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
    open val implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformer(phase) {

    abstract override val context: CfirBodyResolveContext

    abstract override val components: BodyResolveTransformerComponents

    abstract val expressionsTransformer: CfirExpressionsResolveTransformer

    abstract val declarationsTransformer: CfirDeclarationsResolveTransformer

    open fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: CfirResolutionMode,
    ): CfirDeclaration {
        return declaration.transform(this, data)
    }

    override fun <E : CfirElement> transformElement(element: E, data: CfirResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

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

    override fun transformBlock(block: CfirBlock, data: CfirResolutionMode): CfirExpression {
        return declarationsTransformer.transformBlock(block, data)
    }

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
