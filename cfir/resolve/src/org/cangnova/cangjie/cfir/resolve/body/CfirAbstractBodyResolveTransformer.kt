/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.createCurrentScopeList
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSyntheticCallGenerator
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
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
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
        override val syntheticCallGenerator: CfirSyntheticCallGenerator by lazy(LazyThreadSafetyMode.NONE) {
            CfirSyntheticCallGenerator(this)
        }
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
            val resolvedImports = session.importBindingStoreOrNull?.getBindings(file)?.imports
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
                // Scope 列表按低优先级到高优先级排列，tower 反向遍历时会先命中当前文件声明。
                add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
                add(CfirExplicitStarImportingScope(imports, symbolProvider, resolvedImports))
                add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
                add(CfirFileDeclaredTopLevelScope(file))
                add(CfirExplicitSimpleImportingScope(imports, symbolProvider, resolvedImports))
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
    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.typeResolverTransformer`。
     *
     * `prepareSignatureForBodyResolve(...)` 等路径会直接把 declaration 的 typeRef 交给 dispatcher，
     * 因而 dispatcher 必须像 Kotlin 一样承担 body resolve 内的显式类型解析职责。
     */
    protected val typeResolverTransformer: CfirSpecificTypeResolverTransformer by lazy(LazyThreadSafetyMode.NONE) {
        CfirSpecificTypeResolverTransformer(session)
    }

    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.preserveCFGForClasses`。
     * 主干 body resolve 默认保留 class CFG，low-level resolver 可覆写关闭。
     */
    open val preserveCFGForClasses: Boolean
        get() = !implicitTypeOnly

    /**
     * 对齐 Kotlin `FirAbstractBodyResolveTransformerDispatcher.buildCfgForFiles`。
     * 主干 body resolve 默认构建 file CFG，low-level resolver 可覆写关闭，
     * 再由 LL resolver 在 designated 路径上单独计算 file CFG。
     */
    open val buildCfgForFiles: Boolean
        get() = !implicitTypeOnly

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
        // 对齐 Kotlin K2：declaration-content 钩子只负责继续向下遍历当前声明的 children，
        // 让 designated body resolve 可以在“容器已选定”的前提下接管后续子树，而不是重新走一遍具体 transformXxx 分发。
        @Suppress("UNCHECKED_CAST")
        return transformElement(declaration, data) as CfirDeclaration
    }

    override fun <E : CfirElement> transformElement(element: E, data: ResolutionMode): E {
        @Suppress("UNCHECKED_CAST")
        element.transformChildren(this, data)
        return element
    }

    override fun transformTypeRef(typeRef: CfirTypeRef, data: ResolutionMode): CfirResolvedTypeRef {
        val resolvedTypeRef = if (typeRef is CfirResolvedTypeRef) {
            if (typeRef is CfirErrorTypeRef) {
                typeRef.transformPartiallyResolvedTypeRef(this, data)
            }
            typeRef
        } else {
            typeResolverTransformer.transformTypeRef(
                typeRef,
                CfirTypeResolutionConfiguration(
                    scopes = components.createCurrentScopeList(),
                    containingClassDeclarations = context.containingClassDeclarations.toList(),
                    useSiteFile = context.file,
                    topContainer = context.containerIfAny,
                ).withAdditionalTypeParameters(context.containers.flatMap(::extractTypeParameters)),
            ) as CfirResolvedTypeRef
        }

        return resolvedTypeRef.transformAnnotations(this, data) as CfirResolvedTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: ResolutionMode): CfirTypeRef {
        if (data !is ResolutionMode.UpdateImplicitTypeRef) {
            return implicitTypeRef
        }

        return data.newTypeRef.transformSingle(this, data)
    }

    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile {
        checkSessionConsistency(file)
        return CfirAccessibilityFileScope.with(file) {
            declarationsTransformer.transformFile(file, data)
        }
    }

    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: ResolutionMode): CfirCodeFragment {
        return declarationsTransformer.transformCodeFragment(codeFragment, data)
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

    override fun transformExtend(extend: CfirExtend, data: ResolutionMode): CfirExtend {
        return declarationsTransformer.transformExtend(extend, data)
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

    override fun transformMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: ResolutionMode): CfirMacroDeclaration {
        return declarationsTransformer.transformMacroDeclaration(macroDeclaration, data)
    }

    override fun transformFinalizer(finalizer: CfirFinalizer, data: ResolutionMode): CfirFinalizer {
        return declarationsTransformer.transformFinalizer(finalizer, data)
    }

    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty {
        return declarationsTransformer.transformProperty(property, data)
    }

    override fun transformPropertyAccessor(propertyAccessor: CfirPropertyAccessor, data: ResolutionMode): CfirPropertyAccessor {
        return declarationsTransformer.transformPropertyAccessor(propertyAccessor, data)
    }

    override fun transformFieldVariable(fieldVariable: CfirFieldVariable, data: ResolutionMode): CfirFieldVariable {
        return declarationsTransformer.transformFieldVariable(fieldVariable, data)
    }

    /**
     * Body resolve 内部的显式类型引用可能出现在函数体、局部声明、默认参数等位置。
     * 这些位置必须继承当前 container 链上所有可见类型参数，保持与 Kotlin
     * `FirMemberTypeParameterScope` 相同的类型解析上下文。
     */
    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> {
        return when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirInterface -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
            is CfirConstructor -> declaration.typeParameters
            is CfirProperty -> declaration.typeParameters
            is CfirFieldVariable -> declaration.typeParameters
            is CfirValueParameter -> declaration.typeParameters
            is CfirExtend -> declaration.typeParameters
            is CfirTypeAlias -> declaration.typeParameters
            is CfirPatternVariable -> declaration.typeParameters
            is CfirMacroDeclaration -> declaration.typeParameters
            is CfirMainFunction -> declaration.typeParameters
            is CfirFinalizer -> declaration.typeParameters
            is CfirFunction -> declaration.typeParameters
            is CfirCodeFragment -> emptyList()
            is CfirEnumConstructor -> declaration.typeParameters
            else -> emptyList()
        }
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

    override fun transformWrappedExpression(
        wrappedExpression: CfirWrappedExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformWrappedExpression(wrappedExpression, data)
    }

    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformOptionalExpression(optionalExpression, data)
    }

    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformOptionalChainExpression(optionalChainExpression, data)
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

    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: CfirIncrementDecrementExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIncrementDecrementExpression(incrementDecrementExpression, data)
    }

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformIfExpression(ifExpression, data)
    }

    override fun transformLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLetPatternExpression(letPatternExpression, data)
    }

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformReturnExpression(returnExpression, data)
    }

    override fun transformLoopJump(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformLoopJump(jumpExpression, data)
    }

    override fun transformBreakExpression(
        breakExpression: CfirBreakExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformBreakExpression(breakExpression, data)
    }

    override fun transformContinueExpression(
        continueExpression: CfirContinueExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformContinueExpression(continueExpression, data)
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

    override fun transformTypeConversion(
        typeConversion: CfirTypeConversion,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformTypeConversion(typeConversion, data)
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

    override fun transformCatch(
        catch: CfirCatch,
        data: ResolutionMode,
    ): CfirExpression {
        return expressionsTransformer.transformCatch(catch, data)
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
