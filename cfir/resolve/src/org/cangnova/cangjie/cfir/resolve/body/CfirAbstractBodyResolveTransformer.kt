package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractPhaseTransformer
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.util.PrivateForInline

/**
 * Body resolve transformer 的抽象基类。
 * 子类通过 `context` 与 `components` 协作，统一驱动 body resolve 流程。
 */
abstract class CfirAbstractBodyResolveTransformer(
    phase: CfirResolvePhase,
) : CfirAbstractPhaseTransformer<ResolutionMode>(phase) {

    abstract val context: BodyResolveContext

    abstract val resolutionContext: ResolutionContext

    abstract val components: BodyResolveTransformerComponents

    inline val dataFlowAnalyzer: CfirDataFlowAnalyzer
        get() = components.dataFlowAnalyzer

    @set:PrivateForInline
    abstract var implicitTypeOnly: Boolean
        internal set

    final override val session: CfirSession get() = components.session
    @OptIn(PrivateForInline::class)
    internal inline fun <T> withFullBodyResolve(crossinline l: () -> T): T {
        val shouldSwitchMode = implicitTypeOnly
        if (shouldSwitchMode) {
            implicitTypeOnly = false
        }
        return try {
            l()
        } finally {
            if (shouldSwitchMode) {
                implicitTypeOnly = true
            }
        }
    }
    /**
     * 共享组件容器，集中持有 body resolve 所需的会话、上下文和服务。
     */
    open class BodyResolveTransformerComponents(
        override val session: CfirSession,
        override val scopeSession: ScopeSession,
        val transformer: CfirAbstractBodyResolveTransformerDispatcher,
        val context: BodyResolveContext,
        expandTypeAliases: Boolean,

    ) : BodyResolveComponents() {
        override val containingDeclarations: List<CfirDeclaration>
            get() = context.containers.toList()
        override val fileImportsScope: List<CfirScope>
            get() = context.fileImportsScope.ifEmpty { createImportingScopes(context.file) }
        override val towerDataElements: List<CfirTowerDataElement>
            get() = context.towerDataContext.towerDataElements

        override val towerDataContext get() = context.towerDataContext
        override val localScopes: CfirLocalScopes
            get() = context.towerDataContext.localScopes.toPersistentList()
        override val noExpectedType: CfirTypeRef
            get() = buildErrorTypeRef { diagnostic = org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("No expected type") }

        override val returnTypeCalculator: ReturnTypeCalculator
            get() = context.returnTypeCalculator
        override val implicitValueStorage
            get() = context.implicitValueStorage

        override val symbolProvider get() = session.symbolProvider
        override val samResolver: CfirSamResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirSamResolver(session, scopeSession)
        }
        override val file: CfirFile
            get() = context.file
        override val container: CfirDeclaration
            get() = context.containerIfAny ?: context.file

        override val resolutionStageRunner: ResolutionStageRunner = ResolutionStageRunner()







        override val dataFlowAnalyzer: CfirDataFlowAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
            CfirDataFlowAnalyzer(this, context)
        }

        override val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
            by lazy(LazyThreadSafetyMode.NONE) {
                IntegerLiteralAndOperatorApproximationTransformer(session, scopeSession)
            }

        override val callCompleter: CfirCallCompleter by lazy(LazyThreadSafetyMode.NONE) { CfirCallCompleter(transformer, this) }
        override val inlineFunction: CfirFunction?
            get() = context.containers.lastOrNull() as? CfirFunction







        override val callResolver: CfirCallResolver by lazy(LazyThreadSafetyMode.NONE) {
            CfirCallResolver(this)
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
                // 当前文件顶层声明必须先于包级/导入级 scope，避免默认导入抢占本地声明。
                add(CfirFileDeclaredTopLevelScope(file))
                add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
                add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
                add(CfirExplicitStarImportingScope(imports, symbolProvider))
                add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
            }
        }

        fun createResolutionContext(@Suppress("UNUSED_PARAMETER") expectedType: org.cangnova.cangjie.cfir.types.ConeCangJieType? = null): ResolutionContext {
            return ResolutionContext(session, this, context)
        }
    }
}

/**
 * Body resolve dispatcher 的抽象基类。
 * 具体 dispatcher 通过它把各种 `transformXxx` 分发给对应子 transformer。
 */
abstract class CfirAbstractBodyResolveTransformerDispatcher(
    phase: CfirResolvePhase,
    override var implicitTypeOnly: Boolean = false,
) : CfirAbstractBodyResolveTransformer(phase) {

    abstract override val context: BodyResolveContext

    final override val resolutionContext: ResolutionContext
        get() = components.createResolutionContext()

    abstract override val components: BodyResolveTransformerComponents

    abstract val expressionsTransformer: CfirExpressionsResolveTransformer

    abstract val declarationsTransformer: CfirDeclarationsResolveTransformer

    open fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        return declaration.transform(this, data)
    }

    override fun <E : CfirElement> transformElement(element: E, data: ResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile {
        checkSessionConsistency(file)
        return CfirAccessibilityFileScope.with(file) {
            declarationsTransformer.transformFile(file, data)
        }
    }

    override fun transformClass(klass: CfirClass, data: ResolutionMode): CfirClass {
        return declarationsTransformer.transformClass(klass, data)
    }

    override fun transformInterface(interfaceDeclaration: CfirInterface, data: ResolutionMode): CfirInterface {
        return declarationsTransformer.transformInterface(interfaceDeclaration, data)
    }

    override fun transformStruct(struct: CfirStruct, data: ResolutionMode): CfirStruct {
        return declarationsTransformer.transformStruct(struct, data)
    }

    override fun transformEnum(enum: CfirEnum, data: ResolutionMode): CfirEnum {
        return declarationsTransformer.transformEnum(enum, data)
    }

    override fun transformFunction(function: CfirFunction, data: ResolutionMode): CfirFunction {
        return declarationsTransformer.transformFunction(function, data)
    }

    override fun transformConstructor(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor {
        return declarationsTransformer.transformConstructor(constructor, data)
    }

    override fun transformEnumConstructor(enumConstructor: CfirEnumConstructor, data: ResolutionMode): CfirEnumConstructor {
        return declarationsTransformer.transformEnumConstructor(enumConstructor, data)
    }

    override fun transformNamedFunction(namedFunction: CfirNamedFunction, data: ResolutionMode): CfirNamedFunction {
        return declarationsTransformer.transformNamedFunction(namedFunction, data)
    }

    override fun transformMainFunction(mainFunction: CfirMainFunction, data: ResolutionMode): CfirMainFunction {
        return declarationsTransformer.transformMainFunction(mainFunction, data)
    }

    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: ResolutionMode): CfirFieldVariable {
        return declarationsTransformer.transformFieldVariable(fieldVariable, data)
    }

    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable {
        return declarationsTransformer.transformPatternBindingVariable(patternBindingVariable, data)
    }

    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable {
        return declarationsTransformer.transformVariable(variable, data)
    }

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable {
        return declarationsTransformer.transformPatternVariable(patternVariable, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: ResolutionMode): CfirDeclaration {
        return declarationsTransformer.transformDeclaration(declaration, data)
    }

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        return declarationsTransformer.transformBlock(block, data)
    }

    override fun transformExpression(expression: CfirExpression, data: ResolutionMode): CfirExpression {
        return expressionsTransformer.transformExpression(expression, data) as CfirExpression
    }

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLiteralExpression(literalExpression, data)
    }

    override fun transformNamedAccessExpression(
        namedAccess: CfirNamedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformNamedAccessExpression(namedAccess, data)
    }

    override fun transformSuperReceiverExpression(
        superReceiverExpression: CfirSuperReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSuperReceiverExpression(superReceiverExpression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccess: CfirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformQualifiedAccessExpression(qualifiedAccess, data)
    }

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformFunctionCall(functionCall, data)
    }

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIfExpression(ifExpression, data)
    }

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformReturnExpression(returnExpression, data)
    }

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformAssignment(assignment, data)
    }

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTupleLiteral(tupleLiteral, data)
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformArrayLiteral(arrayLiteral, data)
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformStringInterpolation(stringInterpolation, data)
    }

    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformMatchExpression(matchExpression, data)
    }

    override fun transformErrorExpression(
        errorExpression: CfirErrorExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformErrorExpression(errorExpression, data)
    }

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformComparisonExpression(comparisonExpression, data)
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformBinaryOp(binaryOp, data)
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTypeOperator(typeOperator, data)
    }

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformForInExpression(forInExpression, data)
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLoopExpression(loopExpression, data)
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformThrowExpression(throwExpression, data)
    }

    override fun transformPerformExpression(
        performExpression: CfirPerformExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformPerformExpression(performExpression, data)
    }

    override fun transformResumeExpression(
        resumeExpression: CfirResumeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformResumeExpression(resumeExpression, data)
    }

    override fun transformHandleClause(
        handleClause: CfirHandleClause,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformHandleClause(handleClause, data)
    }

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTryExpression(tryExpression, data)
    }

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSubscriptExpression(subscriptExpression, data)
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformAnonymousFunctionExpression(anonymousFunctionExpression, data)
    }

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformRangeExpression(rangeExpression, data)
    }

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformSpawnExpression(spawnExpression, data)
    }
}
