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

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.createCurrentScopeList
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.impl.ResolvedImplicitTypeRef
import org.cangnova.cangjie.cfir.whileAnalysing
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * Declaration-level resolve transformer.
 *
 * 结构对齐 Kotlin K2 `CfirDeclarationsResolveTransformer`：
 *  - 每个 `transformXxx(...)` 用 `whileAnalysing(session, xxx)` 包外壳，负责早退、进入特殊模式等前置决策；
 *  - 真正干活的逻辑挪到 `transformXxxContent(...)`（`protected open`），供低层 body resolver 覆写；
 *  - scope / DFA 推入推出封装在 `withFile`、`forClassLikeBody`、`forConstructorBody` 等辅助函数里，便于替换。
 *
 * 职责：
 *  - 管理文件 / 类 / 函数 / 代码块的作用域栈；
 *  - 解析声明的显式/隐式返回类型，推断 initializer 类型；
 *  - 把局部声明注册到当前 local scope。
 */
open class CfirDeclarationsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {
    /** 显式类型引用解析器，用于 body resolve 中补解析局部声明和恢复错误类型引用。 */
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)

    /**
     * 对齐 Kotlin FIR 的 declaration-content 入口。
     * low-level body resolver 需要覆写这一层，而不是把局部逻辑硬塞回 transformFunction/transformConstructor。
     */
    protected fun transformDeclarationContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        return transformer.transformDeclarationContent(declaration, data)
    }

    // ── File ───────────────────────────────────────────────────────────────

    /**
     * 解析文件级声明内容，并用 `whileAnalysing` 保护重复进入和阶段状态。
     */
    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile =
        whileAnalysing(session, file) {
            doTransformFile(file, data)
        }

    /**
     * 在文件 scope / CFG 生命周期内解析文件内容。
     */
    private fun doTransformFile(file: CfirFile, data: ResolutionMode): CfirFile =
        withFile(file) {
            transformFileContent(file, data)
        }

    /**
     * 包装 file 级别的 scope / DFA 生命周期。对齐 K2 `CfirDeclarationsResolveTransformer.withFile`：
     * 进入 `context.withFile`、装配 imports、开启 file CFG，最后把 CFG 写回 file。
     */
    open fun withFile(file: CfirFile, action: () -> CfirFile): CfirFile {
        val savedContext = context.towerDataContext
        try {
            return context.withFile(file) {
                val importScopes = createImportingScopes(file)
                context.addNonLocalScopes(importScopes)

                dataFlowAnalyzer.enterFile(file, buildGraph = transformer.buildCfgForFiles)
                val result = action()
                dataFlowAnalyzer.exitFile()?.let { graph ->
                    file.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
                }
                result
            }
        } finally {
            context.replaceTowerDataContext(savedContext)
        }
    }

    /**
     * 文件声明内容解析入口，允许 low-level resolver 覆写。
     */
    protected open fun transformFileContent(file: CfirFile, data: ResolutionMode): CfirFile =
        transformDeclarationContent(file, data) as CfirFile

    /**
     * 解析代码片段并维护代码片段级 DFA 生命周期。
     */
    override fun transformCodeFragment(codeFragment: CfirCodeFragment, data: ResolutionMode): CfirCodeFragment {
        dataFlowAnalyzer.enterCodeFragment(codeFragment)
        context.withCodeFragment(codeFragment, components) {
            transformBlock(codeFragment.block, data)
        }
        dataFlowAnalyzer.exitCodeFragment(codeFragment)
        return codeFragment
    }

    // ── Class-like declarations ───────────────────────────────────────────

    /**
     * 解析 class body resolve 内容。
     */
    override fun transformClass(klass: CfirClass, data: ResolutionMode): CfirClass =
        whileAnalysing(session, klass) {
            transformClassContent(klass, data)
        }

    /**
     * 解析 interface body resolve 内容。
     */
    override fun transformInterface(`interface`: CfirInterface, data: ResolutionMode): CfirInterface =
        whileAnalysing(session, `interface`) {
            transformInterfaceContent(`interface`, data)
        }

    /**
     * 解析 struct body resolve 内容。
     */
    override fun transformStruct(struct: CfirStruct, data: ResolutionMode): CfirStruct =
        whileAnalysing(session, struct) {
            transformStructContent(struct, data)
        }

    /**
     * 解析 enum body resolve 内容。
     */
    override fun transformEnum(enum: CfirEnum, data: ResolutionMode): CfirEnum =
        whileAnalysing(session, enum) {
            transformEnumContent(enum, data)
        }

    /**
     * 解析 extend body resolve 内容。
     */
    override fun transformExtend(extend: CfirExtend, data: ResolutionMode): CfirExtend =
        whileAnalysing(session, extend) {
            transformExtendContent(extend, data)
        }

    /**
     * class 声明内容解析入口。
     */
    protected open fun transformClassContent(klass: CfirClass, data: ResolutionMode): CfirClass =
        doTransformRegularClassContent(klass, data)

    /**
     * interface 声明内容解析入口。
     */
    protected open fun transformInterfaceContent(
        interfaceDeclaration: CfirInterface,
        data: ResolutionMode,
    ): CfirInterface = transformOtherClassLikeDeclaration(interfaceDeclaration, data) as CfirInterface

    /**
     * struct 声明内容解析入口。
     */
    protected open fun transformStructContent(struct: CfirStruct, data: ResolutionMode): CfirStruct =
        transformOtherClassLikeDeclaration(struct, data) as CfirStruct

    /**
     * enum 声明内容解析入口。
     */
    protected open fun transformEnumContent(enum: CfirEnum, data: ResolutionMode): CfirEnum =
        transformOtherClassLikeDeclaration(enum, data) as CfirEnum

    /**
     * extend 声明内容解析入口。
     *
     * 该入口建立 extend 专属 scope 和 container，结束时恢复 tower data context 并推进 phase。
     */
    protected open fun transformExtendContent(extend: CfirExtend, data: ResolutionMode): CfirExtend {
        val savedContext = context.towerDataContext
        try {
            return context.withScopesForExtend(extend, components) {
                context.withContainer(extend) {
                    transformDeclarationContent(extend, data) as CfirExtend
                }
            }
        } finally {
            context.replaceTowerDataContext(savedContext)
            bumpPhase(extend)
        }
    }

    /**
     * regular class 对齐 Kotlin `doTransformRegularClassContent`：
     * class body 的 scope / container / CFG 生命周期统一走 dedicated regular-class 入口。
     */
    protected open fun doTransformRegularClassContent(
        klass: CfirClass,
        data: ResolutionMode,
    ): CfirClass = forRegularClassBody(klass) {
        transformDeclarationContent(klass, data) as CfirClass
    }

    /**
     * 解析非 regular class-like 声明内容。
     *
     * interface/struct/enum 共享该入口：建立 class scope，执行声明内容解析，
     * 恢复 tower data context，并把声明推进到 BODY_RESOLVE。
     */
    private fun <T : CfirClassLikeDeclaration> transformOtherClassLikeDeclaration(classLike: T, data: ResolutionMode): T {
        val savedContext = context.towerDataContext

        val resolveContent = {
            context.withContainer(classLike) {
                transformDeclarationContent(classLike, data) as T
            }
        }

        context.withScopesForClass(classLike, components, resolveContent)

        context.replaceTowerDataContext(savedContext)
        bumpPhase(classLike)
        return classLike
    }

    /**
     * 进入 regular class body 的 DFA 区间；exit 后把 CFG 写回 class。
     * 对齐 Kotlin `FirDeclarationsResolveTransformer.forRegularClassBody`。
     */
    open fun forRegularClassBody(klass: CfirClass, action: () -> CfirClass): CfirClass {
        dataFlowAnalyzer.enterClass(klass, buildGraph = transformer.preserveCFGForClasses)
        val result = context.withContainingClass(klass) {
            context.forRegularClassBody(klass, components) {
                action()
            }
        }
        dataFlowAnalyzer.exitClass()?.let { graph ->
            result.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
        }
        return result
    }

    // ── Function ──────────────────────────────────────────────────────────

    /**
     * 通用函数分发入口不应被直接调用。
     *
     * CFIR 中每类 function-like 节点都必须走自己的具体 transform 入口，
     * 以便建立正确的局部作用域、container 和 CFG 生命周期。
     */
    override fun transformFunction(
        function: CfirFunction,
        data: ResolutionMode,
    ): CfirFunction {
        error("Concrete transform functions should be called")
    }

    /**
     * 解析函数声明体的公共实现。
     *
     * 当 `shouldResolveEverything` 为 true 时先解析签名和注解，再按返回类型决定 body 的期望类型，
     * 最后退出函数级 DFA 并把 CFG 写回函数。
     */
    protected open fun transformFunctionContent(
        function: CfirFunction,
        resolutionModeForBody: ResolutionMode,
        shouldResolveEverything: Boolean,
    ): CfirFunction {
        dataFlowAnalyzer.enterFunction(function)

        if (shouldResolveEverything) {
            // 对齐 Kotlin FIR：函数完整 body resolve 必须先解析返回类型、参数默认值和注解，
            // 否则默认参数里的调用不会进入统一的调用解析与诊断流水线。
            function
                .transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
                .transformValueParameters(transformer, ResolutionMode.ContextIndependent)
                .transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        }

        val bodyResolutionMode = function.returnTypeRef
            .takeUnless { it is CfirImplicitTypeRef }
            ?.let(::withExpectedType)
            ?: resolutionModeForBody

        val body = function.body
        if (body != null) {
            function.transformBody(transformer, bodyResolutionMode)
        }
        function.replaceControlFlowGraphReference(dataFlowAnalyzer.exitFunction(function))
        return function
    }

    /**
     * Call completion 专用的 lambda body 解析入口。
     *
     * 对齐 Kotlin FIR `doTransformAnonymousFunctionBodyFromCallCompletion`：补全阶段已经
     * 确定了参数类型和候选系统，只能在保存的 lambda tower context 下解析函数体，
     * 不能重新走整个 anonymous-function expression 入口，否则会再次触发 postponed
     * lambda 的上下文存储/早退路径。
     */
    internal fun doTransformAnonymousFunctionBodyFromCallCompletion(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        expectedReturnTypeFromCallPosition: CfirResolvedTypeRef?,
        resolutionModeForBody: ResolutionMode? = null,
    ) {
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        val expectedReturnTypeRef = expectedReturnTypeFromCallPosition
            ?: anonymousFunction.returnTypeRef.takeUnless { it is CfirImplicitTypeRef }
        val actualResolutionModeForBody = resolutionModeForBody
            ?: expectedReturnTypeRef?.let(::withExpectedType)
            ?: ResolutionMode.ContextDependent

        if (expectedReturnTypeFromCallPosition == null) {
            context.withLambdaBeingAnalyzedInDependentContext(anonymousFunction.symbol) {
                transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef, actualResolutionModeForBody)
            }
        } else {
            transformAnonymousFunctionBody(anonymousFunction, expectedReturnTypeRef, actualResolutionModeForBody)
        }
    }

    /**
     * 在已经保存的匿名函数上下文中解析 lambda body。
     */
    private fun transformAnonymousFunctionBody(
        anonymousFunction: CfirAnonymousFunction,
        expectedReturnTypeRef: CfirTypeRef?,
        resolutionModeForBody: ResolutionMode,
    ): CfirAnonymousFunction {
        val lambdaType = anonymousFunction.typeRef
        return context.withAnonymousFunction(anonymousFunction, components) {
            withFullBodyResolve {
                if (expectedReturnTypeRef is CfirResolvedTypeRef &&
                    anonymousFunction.returnTypeRef !is CfirResolvedTypeRef
                ) {
                    anonymousFunction.replaceReturnTypeRef(expectedReturnTypeRef)
                }

                whileAnalysing(session, anonymousFunction) {
                    transformFunctionContent(
                        anonymousFunction,
                        resolutionModeForBody = resolutionModeForBody,
                        shouldResolveEverything = true,
                    ) as CfirAnonymousFunction
                }
            }
        }.apply {
            replaceTypeRef(lambdaType)
        }
    }

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * 解析普通构造器 body resolve 内容。
     */
    override fun transformConstructor(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor =
        whileAnalysing(session, constructor) {
            if (transformer.implicitTypeOnly) return constructor
            // 仓颉的 annotation class 语义与 Kotlin 不完全一致（没有 ClassKind 体系），
            // 当前阶段不对 annotation 构造器做特殊 tower-data 切换；
            // 后续若引入 CfirClass.isAnnotation 标记，可再在此处分流到 withAnnotationContext。
            return transformConstructorContent(constructor, data)
        }

    /**
     * 解析构造器内容，包括签名、委托构造调用、构造器参数作用域和构造器 body。
     */
    protected open fun transformConstructorContent(
        constructor: CfirConstructor,
        data: ResolutionMode,
    ): CfirConstructor {
        val owningClass = context.containerIfAny as? CfirClass

        dataFlowAnalyzer.enterFunction(constructor)

        context.forConstructor(constructor) {
            constructor.transformTypeParameters(transformer, data)
                .transformAnnotations(transformer, data)
                .transformReturnTypeRef(transformer, data)

            context.forConstructorParameters(constructor, owningClass, components) {
                constructor.transformValueParameters(transformer, data)
            }
            transformDelegatedConstructorCall(constructor, data)
            context.forConstructorBody(constructor, session) {
                constructor.transformBody(transformer, data)
            }
        }

        val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(constructor)
        constructor.replaceControlFlowGraphReference(controlFlowGraphReference)
        return constructor
    }

    /**
     * 解析构造器 body 中第一条委托构造调用。
     */
    private fun transformDelegatedConstructorCall(
        constructor: CfirConstructor,
        data: ResolutionMode,
    ) {
        val call = constructor.body?.statements?.firstOrNull()?.delegatedConstructorCallOrNull() ?: return
        call.transform<CfirExpression, ResolutionMode>(transformer, data)
    }

    /**
     * 从语句包装层中抽取委托构造调用表达式。
     */
    private fun CfirStatement.delegatedConstructorCallOrNull(): CfirFunctionCall? {
        val expression = when (this) {
            is CfirWrappedExpression -> this.expression
            is CfirExpression -> this
            else -> return null
        }
        val call = expression as? CfirFunctionCall ?: return null
        return call.takeIf { it.origin.isConstructorDelegation }
    }

    // ── Enum constructor ──────────────────────────────────────────────────

    /**
     * 解析 enum constructor body resolve 内容。
     */
    override fun transformEnumConstructor(
        enumConstructor: CfirEnumConstructor,
        data: ResolutionMode,
    ): CfirEnumConstructor = whileAnalysing(session, enumConstructor) {
        transformEnumConstructorContent(enumConstructor, data)
    }

    /**
     * 解析 enum constructor 的值参数和返回类型，并把隐式返回类型回填为 owner enum 类型。
     */
    protected open fun transformEnumConstructorContent(
        enumConstructor: CfirEnumConstructor,
        data: ResolutionMode,
    ): CfirEnumConstructor {
        val savedContext = context.towerDataContext

        context.withContainer(enumConstructor) {
            if (enumConstructor.typeParameters.isNotEmpty()) {
                context.addNonLocalScope(CfirTypeParameterScopeImpl(enumConstructor.typeParameters))
            }

            enumConstructor.valueParameters.forEach { parameter ->
                parameter.replaceReturnTypeRef(
                    resolveExplicitTypeRefIfNeeded(parameter.returnTypeRef, enumConstructor.typeParameters),
                )
            }

            val returnTypeRef = enumConstructor.returnTypeRef
            if (returnTypeRef is CfirImplicitTypeRef) {
                val ownerEnum = context.containers.filterIsInstance<CfirEnum>().lastOrNull()
                val ownerType = ownerEnum?.let(::buildConstructedTypeForClass)
                    ?: ConeErrorType(ConeSimpleDiagnostic("enum constructor has no owning enum"))
                enumConstructor.replaceReturnTypeRef(
                    returnTypeRef.resolvedTypeFromPrototype(ownerType, returnTypeRef.source),
                )
            } else {
                enumConstructor.replaceReturnTypeRef(
                    resolveExplicitTypeRefIfNeeded(returnTypeRef, enumConstructor.typeParameters),
                )
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(enumConstructor)
        return enumConstructor
    }

    // ── Property ──────────────────────────────────────────────────────────

    /**
     * 解析属性声明 body resolve 内容。
     */
    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty =
        whileAnalysing(session, property) {
            transformPropertyContent(property, data)
        }

    /**
     * 解析属性类型、注解、访问器与 body resolve 状态。
     */
    protected open fun transformPropertyContent(
        property: CfirProperty,
        data: ResolutionMode,
    ): CfirProperty {
        if (property.bodyResolveState >= CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
            bumpPhase(property)
            return property
        }

        val shouldResolveEverything = !transformer.implicitTypeOnly
        if (property.isLocal) {
            context.storeProperty(property, session)
        }

        context.withProperty(property) {
            property.replaceReturnTypeRef(
                resolveExplicitTypeRefIfNeeded(
                    property.returnTypeRef,
                    property.typeParameters,
                ),
            )

            if (shouldResolveEverything) {
                property.transformAnnotations(transformer, data)
                property.transformTypeParameters(transformer, ResolutionMode.ContextIndependent)
            }

            property.getter?.let { getter ->
                transformAccessor(getter, property, shouldResolveEverything)
                property.replaceBodyResolveState(CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED)
            }

            if (shouldResolveEverything) {
                property.setter?.let { setter ->
                    transformAccessor(setter, property, shouldResolveEverything)
                }
                property.replaceBodyResolveState(CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED)
            }
        }

        bumpPhase(property)
        return property
    }

    /**
     * 访问器节点本身由所属属性统一驱动解析。
     */
    override fun transformPropertyAccessor(
        propertyAccessor: CfirPropertyAccessor,
        data: ResolutionMode,
    ): CfirPropertyAccessor {
        transformProperty(propertyAccessor.propertySymbol.cfir, data)
        return propertyAccessor
    }

    /**
     * 在属性访问器上下文内解析 getter/setter body。
     */
    private fun transformAccessor(
        accessor: CfirPropertyAccessor,
        owner: CfirProperty,
        shouldResolveEverything: Boolean,
    ): Unit = whileAnalysing(session, accessor) {
        context.withPropertyAccessor(owner, accessor, components) {
            prepareSignatureForBodyResolve(accessor)
            withFullBodyResolve {
                transformFunctionWithGivenSignature(
                    accessor,
                    shouldResolveEverything,
                    inferImplicitReturnType = accessor.isGetter,
                )
            }
        }
    }

    // ── Variable (generic fallback) ───────────────────────────────────────

    /**
     * 解析普通变量声明。
     */
    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable =
        whileAnalysing(session, variable) {
            transformVariableContent(variable, data)
        }

    /**
     * 解析变量显式类型、initializer，并从 initializer 回填隐式变量类型。
     */
    protected open fun transformVariableContent(
        variable: CfirVariable,
        data: ResolutionMode,
    ): CfirVariable {
        variable.replaceReturnTypeRef(resolveExplicitTypeRefIfNeeded(variable.returnTypeRef))

        val explicitTypeRef = variable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            ResolutionMode.WithExpectedType(explicitTypeRef)
        } else {
            ResolutionMode.ContextIndependent
        }

        variable.initializer?.let {
            variable.transformInitializer(transformer, initializerMode)
        }

        variable.resolveImplicitReturnTypeFromInitializer()

        context.storeVariable(variable, session)

        bumpPhase(variable)
        return variable
    }

    // ── Value parameter ───────────────────────────────────────────────────

    /**
     * 解析值参数声明。
     */
    override fun transformValueParameter(
        valueParameter: CfirValueParameter,
        data: ResolutionMode,
    ): CfirValueParameter = whileAnalysing(session, valueParameter) {
        transformValueParameterContent(valueParameter, data)
    }

    /**
     * 解析值参数类型、默认值以及参数级 CFG。
     */
    protected open fun transformValueParameterContent(
        valueParameter: CfirValueParameter,
        data: ResolutionMode,
    ): CfirValueParameter {
        dataFlowAnalyzer.enterValueParameter(valueParameter)

        val result = context.withValueParameter(valueParameter, session) {
            // 仓颉的 annotation parameter 默认值必须是常量表达式，
            // 但常量求值由后续 checker / codegen 阶段完成，
            // 这里只负责类型推断与 DFA，不做 ArrayLiteralPosition 的注解参数分流。
            valueParameter.replaceReturnTypeRef(
                resolveExplicitTypeRefIfNeeded(valueParameter.returnTypeRef, valueParameter.typeParameters),
            )
            transformDeclarationContent(
                valueParameter,
                withExpectedType(valueParameter.returnTypeRef),
            ) as CfirValueParameter
        }

        dataFlowAnalyzer.exitValueParameter(result)?.let { graph ->
            result.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
        }

        return result
    }

    // ── Field variable ────────────────────────────────────────────────────

    /**
     * 解析字段变量声明。
     */
    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: ResolutionMode,
    ): CfirFieldVariable = whileAnalysing(session, fieldVariable) {
        transformFieldVariableContent(fieldVariable, data)
    }

    /**
     * 解析字段变量显式类型、initializer，并把字段 initializer 的 DFA 生命周期接入上层图。
     */
    protected open fun transformFieldVariableContent(
        fieldVariable: CfirFieldVariable,
        data: ResolutionMode,
    ): CfirFieldVariable {
        fieldVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(fieldVariable.returnTypeRef, fieldVariable.typeParameters),
        )

        val explicitTypeRef = fieldVariable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            ResolutionMode.WithExpectedType(explicitTypeRef)
        } else {
            ResolutionMode.ContextIndependent
        }

        val initializer = fieldVariable.initializer
        if (initializer != null) {
            dataFlowAnalyzer.enterFieldInitializer(fieldVariable)
            fieldVariable.transformInitializer(transformer, initializerMode)
            // 仓颉 CfirFieldVariable 不是 CfirControlFlowGraphOwner：
            // 字段初始化器的 CFG 归属包含它的 class initializer / primary constructor，
            // 这里只负责通知 DFA 出栈，graph 由 DFA 合并到上层。
            dataFlowAnalyzer.exitFieldInitializer()
        }

        fieldVariable.resolveImplicitReturnTypeFromInitializer()

        context.storeVariable(fieldVariable, session)

        bumpPhase(fieldVariable)
        return fieldVariable
    }

    // ── Declaration (generic fallback) ────────────────────────────────────

    /**
     * 未专门覆盖的声明只推进 body resolve phase。
     */
    override fun transformDeclaration(declaration: CfirDeclaration, data: ResolutionMode): CfirDeclaration =
        whileAnalysing(session, declaration) {
            transformDeclarationBumpPhaseContent(declaration, data)
        }

    /**
     * 通用声明 phase 推进入口。
     */
    protected open fun transformDeclarationBumpPhaseContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    // ── Pattern binding / pattern variable ────────────────────────────────

    /**
     * 解析模式绑定变量声明。
     */
    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable = whileAnalysing(session, patternBindingVariable) {
        transformPatternBindingVariableContent(patternBindingVariable, data)
    }

    /**
     * 解析模式绑定变量显式类型并推进 phase。
     */
    protected open fun transformPatternBindingVariableContent(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable {
        patternBindingVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(patternBindingVariable.returnTypeRef, patternBindingVariable.typeParameters),
        )
        bumpPhase(patternBindingVariable)
        return patternBindingVariable
    }

    /**
     * 解析带 initializer 的模式变量声明。
     */
    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable = whileAnalysing(session, patternVariable) {
        transformPatternVariableContent(patternVariable, data)
    }

    /**
     * 解析模式变量类型、initializer、整体模式以及模式绑定类型。
     */
    protected open fun transformPatternVariableContent(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable {
        patternVariable.replaceReturnTypeRef(
            resolveExplicitTypeRefIfNeeded(patternVariable.returnTypeRef, patternVariable.typeParameters),
        )

        val explicitTypeRef = patternVariable.returnTypeRef
        val initializerMode = if (explicitTypeRef is CfirResolvedTypeRef) {
            ResolutionMode.WithExpectedType(explicitTypeRef)
        } else {
            ResolutionMode.ContextIndependent
        }

        val initializer = patternVariable.initializer
        if (initializer != null) {
            patternVariable.transformInitializer(transformer, initializerMode)
        }

        patternVariable.resolveImplicitReturnTypeFromInitializer()
        propagateWholeInitializerToSimplePatternBinding(patternVariable.pattern, patternVariable.initializer)

        patternVariable.transformPattern(transformer, ResolutionMode.ContextIndependent)
        resolvePatternBindingTypes(
            pattern = patternVariable.pattern,
            expectedType = patternVariable.returnTypeRef.coneTypeOrNull,
            typeResolver = specificTypeResolverTransformer,
        )
        registerPatternBindings(patternVariable.pattern)

        bumpPhase(patternVariable)
        return patternVariable
    }

    // ── Block（非 declaration，不走 whileAnalysing） ───────────────────────

    /**
     * 在 block 作用域中解析表达式 block。
     */
    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression =
        context.forBlock(session) {
            transformer.expressionsTransformer.transformBlock(block, data)
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * 对齐 Kotlin `storeVariableReturnType`：隐式变量类型必须在声明解析阶段收敛。
     * 有 initializer 时取 initializer 类型；没有可用类型时写入 error type ref，避免
     * 后续隐式类型缓存或 checker 继续看到裸 `CfirImplicitTypeRef`。
     */
    private fun CfirVariable.resolveImplicitReturnTypeFromInitializer() {
        val implicitTypeRef = returnTypeRef as? CfirImplicitTypeRef ?: return
        val initType = initializer?.coneTypeOrNull
        val resolvedTypeRef = if (initType != null) {
            val resolvedType = IdealTypeResolver.resolveIfIdeal(initType).approximateThisTypeForDeclaration()
            implicitTypeRef.resolvedTypeFromPrototype(resolvedType, implicitTypeRef.source)
        } else {
            buildErrorTypeRef {
                source = implicitTypeRef.source ?: this@resolveImplicitReturnTypeFromInitializer.source
                diagnostic = ConeSimpleDiagnostic(
                    "Cannot infer variable type without an initializer",
                    DiagnosticKind.InferenceError,
                )
            }
        }
        replaceReturnTypeRef(resolvedTypeRef)
    }

    /**
     * 构造 body resolve 使用的导入和本地声明 scope 列表。
     *
     * 该列表保持声明解析阶段的查找优先级：默认导入最低，本地文件声明和显式 simple import 位于高优先级侧。
     */
    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
        val symbolProvider = session.symbolProvider
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
            // Scope 列表按低优先级到高优先级排列，声明解析阶段同样保持本地声明优先。
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider, resolvedImports))
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
            add(CfirFileDeclaredTopLevelScope(file))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider, resolvedImports))
        }
    }
    // ── Named function ────────────────────────────────────────────────────

    /**
     * 仓颉的 named function 解析流程（对齐 K2 `transformNamedFunction` 但按仓颉语义裁剪）：
     *  1. `whileAnalysing` 包外壳，`implicitTypeOnly` 且返回类型已显式时直接返回；
     *  2. 进入 `withNamedFunction`（非类成员时注册到当前局部作用域）；
     *  3. 解析类型参数（仓颉没有 Kotlin 的 receiverParameter 独立声明、contract 系统、repl snippet、
     *     header mode 等，对应分支全部剔除）；
     *  4. 非顶层、非类成员时（即局部嵌套函数）先把签名里的 return / 参数类型解析出来；
     *  5. 进入 `forFunctionBody`，按 `transformFunctionContent` 解析 body，必要时从 body 回推隐式返回类型。
     */
    override fun transformNamedFunction(
        namedFunction: CfirNamedFunction,
        data: ResolutionMode,
    ): CfirNamedFunction = transformFunctionLikeDeclaration(namedFunction) { action ->
        context.withNamedFunction(namedFunction, session) {
            action()
        }
    }

    /**
     * `main` 在 CFIR 树上是独立声明节点，但 body resolve 约束仍与普通函数一致：
     * 需要走具体声明入口，而不是落回 `transformFunction()` 这个抽象兜底分发。
     */
    override fun transformMainFunction(
        mainFunction: CfirMainFunction,
        data: ResolutionMode,
    ): CfirMainFunction = transformFunctionLikeDeclaration(mainFunction) { action ->
        context.withContainer(mainFunction) {
            action()
        }
    }

    /**
     * macro declaration 在 CFIR 树上是独立的 function-like 声明，
     * BODY_RESOLVE 必须走与普通函数一致的局部作用域/CFG 框架，而不是只靠默认 children 递归。
     */
    override fun transformMacroDeclaration(
        macroDeclaration: CfirMacroDeclaration,
        data: ResolutionMode,
    ): CfirMacroDeclaration = transformFunctionLikeDeclaration(macroDeclaration) { action ->
        context.withContainer(macroDeclaration) {
            action()
        }
    }

    /**
     * finalizer 复用 function body resolve 的作用域与 CFG 生命周期，
     * 但保持现有返回类型行为：不在这里把 block 尾表达式回推到 finalizer 返回类型。
     */
    override fun transformFinalizer(
        finalizer: CfirFinalizer,
        data: ResolutionMode,
    ): CfirFinalizer = transformFunctionLikeDeclaration(
        finalizer,
        inferImplicitReturnType = false,
    ) { action ->
        context.withContainer(finalizer) {
            action()
        }
    }

    /**
     * `main` / `macro` / `finalizer` 这些独立 function-like 声明在 BODY_RESOLVE 上
     * 共享同一套 header -> local scope -> body -> CFG 框架；
     * 是否额外把声明注册为当前局部函数，由外层 `withFunctionContext` 决定。
     */
    private inline fun <F : CfirFunction> transformFunctionLikeDeclaration(
        function: F,
        inferImplicitReturnType: Boolean = true,
        withFunctionContext: (() -> F) -> F,
    ): F = whileAnalysing(session, function) {
        val shouldResolveEverything = !transformer.implicitTypeOnly
        val returnTypeRef = function.returnTypeRef
        if (returnTypeRef !is CfirImplicitTypeRef && transformer.implicitTypeOnly) {
            return function
        }

        val containingDeclaration = context.containerIfAny
        withFunctionContext {
            if (shouldResolveEverything) {
                function.transformTypeParameters(this, ResolutionMode.ContextIndependent)
            }

            // 局部嵌套函数（不在类体/文件顶层）需要先把签名解析好，
            // 因为类成员的签名由前面的阶段（SUPER_TYPES / STATUS）处理过。
            val isLocalNested = containingDeclaration != null &&
                    containingDeclaration !is CfirClass &&
                    containingDeclaration !is CfirFile
            if (isLocalNested) {
                prepareSignatureForBodyResolve(function)
            }

            val resolveBody = {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(
                        function,
                        shouldResolveEverything,
                        inferImplicitReturnType = inferImplicitReturnType,
                    )
                }
            }

            if (function is CfirFinalizer) {
                context.forFinalizerBody(function, components, resolveBody)
            } else {
                context.forFunctionBody(function, components, resolveBody)
            }
        }
    }

    /**
     * 解析函数 body 并在需要时回推隐式返回类型。
     * 仓颉没有 Kotlin 的类型近似（`approximateDeclarationType` / `visibilityForApproximation`）和
     * header mode，简化为"若返回类型仍是 implicit，直接用 body 的 cone type 封装"。
     */
    private fun <F : CfirFunction> transformFunctionWithGivenSignature(
        function: F,
        shouldResolveEverything: Boolean,
        inferImplicitReturnType: Boolean = true,
    ): F {
        @Suppress("UNCHECKED_CAST")
        val result = transformFunctionContent(
            function,
            resolutionModeForBody = ResolutionMode.ContextIndependent,
            shouldResolveEverything = shouldResolveEverything,
        ) as F

        val alreadyResolvedReturnTypeRef = (result.returnTypeRef as? ResolvedImplicitTypeRef)?.typeRef
        if (alreadyResolvedReturnTypeRef != null) {
            result.transformReturnTypeRef(transformer, ResolutionMode.UpdateImplicitTypeRef(alreadyResolvedReturnTypeRef))
        } else if (inferImplicitReturnType && result.returnTypeRef is CfirImplicitTypeRef) {
            val inferredType = inferFunctionReturnType(result)
            val resolved = result.returnTypeRef.resolvedTypeFromPrototype(
                inferredType,
                result.returnTypeRef.source,
            )
            result.replaceReturnTypeRef(resolved)
        }

        return result
    }

    /**
     * 从当前函数 CFG 提取到的返回结果里推断函数返回类型。
     *
     * 返回结果统一包含：
     * - 显式 `return expr`
     * - 函数体正常流出时的 block 尾表达式
     *
     * 这样可以让“最后一条表达式是返回值”与显式 return 共享同一套推断入口，
     * 仓颉官方会把显式 return 后面的 block 尾表达式也纳入隐式返回类型推断；
     * 这种尾表达式即使在控制流上不可达，仍会让返回类型推断失败。
     */
    private fun inferFunctionReturnType(function: CfirFunction): ConeCangJieType {
        val returnExpressions = components.dataFlowAnalyzer.returnExpressionsOfFunction(function)
        if (returnExpressions.isEmpty()) {
            return session.builtinTypes.unitType
        }

        val expressionTypes = returnExpressions.map { expression ->
            expression.coneTypeOrNull ?: ConeErrorType(
                ConeSimpleDiagnostic("Postponed inference", DiagnosticKind.InferenceError)
            )
        }

        if (expressionTypes.size == 1) {
            return expressionTypes.single()
        }

        expressionTypes.commonThisReturnTypeOrNull()?.let { return it }

        val commonType = session.typeContext.commonSuperTypeOrNull(expressionTypes)
        if (commonType != null && commonType !is ConeErrorType && commonType.isAcceptableInferredReturnType(expressionTypes)) {
            return commonType
        }

        val message = expressionTypes.joinToString(
            prefix = "The types ",
            postfix = " do not have the smallest common supertype",
        ) { "'$it'" }
        return ConeErrorType(ConeSimpleDiagnostic(message, DiagnosticKind.InferenceError))
    }

    /**
     * 如果所有返回表达式都是同一个 `This` 类型，则直接把该 `This` 类型作为公共返回类型。
     */
    private fun List<ConeCangJieType>.commonThisReturnTypeOrNull(): ConeClassLikeType? {
        val thisTypes = map { type -> type as? ConeClassLikeType ?: return null }
        if (thisTypes.any { !it.isThisType }) return null
        val first = thisTypes.firstOrNull() ?: return null
        return first.takeIf { candidate -> thisTypes.all { it == candidate } }
    }

    /**
     * 函数隐式返回类型不能只因为所有候选都可装箱到 `Any` 就吞掉推断失败。
     *
     * 仓颉允许 `class` 返回值与基本类型共同推断为 `Any`；但纯基本类型/值类型候选之间
     * 若唯一公共父类型退化到 `Any`，官方语义仍要求报告“没有最小公共父类型”。
     */
    private fun ConeCangJieType.isAcceptableInferredReturnType(expressionTypes: List<ConeCangJieType>): Boolean {
        if (!isAnyType()) return true
        return expressionTypes.any { it is ConeClassLikeType && !it.isAnyType() }
    }

    /**
     * 判断类型是否为标准库 `Any`。
     */
    private fun ConeCangJieType.isAnyType(): Boolean {
        return this === ConeAnyType || (this is ConeClassLikeType && classId == StdlibClassIds.Any)
    }

    /**
     * 提前把函数签名（返回类型、各参数类型）解析到 resolved 状态。
     * 用于局部嵌套函数进入 body resolve 前的一次性签名准备。
     */
    private fun prepareSignatureForBodyResolve(callableMember: CfirCallableDeclaration) {
        callableMember.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
        if (callableMember is CfirFunction) {
            callableMember.valueParameters.forEach {
                it.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            }
        }
    }

    /**
     * 在当前声明上下文中解析显式类型引用。
     *
     * 隐式类型保持原样；已解析但内部包含错误并保留 delegated type ref 的引用会尝试用 delegated ref 重新解析。
     */
    private fun resolveExplicitTypeRefIfNeeded(
        typeRef: CfirTypeRef,
        additionalTypeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirTypeRef {
        if (typeRef is CfirImplicitTypeRef) return typeRef
        val typeParametersFromContainers = context.containers
            .filterIsInstance<CfirDeclaration>()
            .flatMap(::extractTypeParameters)
        val config = CfirTypeResolutionConfiguration(
            scopes = components.createCurrentScopeList(),
            containingClassDeclarations = context.containingClassDeclarations.toList(),
            useSiteFile = context.file,
            topContainer = context.containerIfAny,
        ).withAdditionalTypeParameters(typeParametersFromContainers + additionalTypeParameters)

        if (typeRef is CfirResolvedTypeRef) {
            val delegated = typeRef.delegatedTypeRef
            if (
                typeRef.coneType.contains { it is ConeErrorType } &&
                delegated != null &&
                delegated !is CfirImplicitTypeRef
            ) {
                return specificTypeResolverTransformer.transformTypeRef(delegated, config)
            }
            return typeRef
        }

        return specificTypeResolverTransformer.transformTypeRef(
            typeRef,
            config,
        )
    }

    /**
     * 提取声明在当前类型解析上下文中暴露的类型参数。
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

            is CfirEnumConstructor -> declaration.typeParameters
            else -> emptyList()
        }
    }

    /**
     * 将声明推进到 BODY_RESOLVE 阶段。
     */
    private fun bumpPhase(declaration: CfirDeclaration) {
        if (declaration.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES &&
            declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE
        ) {
            declaration.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
    }

    /**
     * 在当前文件和容器上下文中解析 class-like 声明的稳定 ClassId。
     */
    private fun resolveClassId(klass: CfirClassLikeDeclaration): ClassId? {
        val packageFqName = try {
            context.file.packageDirective.packageFqName
        } catch (_: UninitializedPropertyAccessException) {
            FqName.ROOT
        }

        // 当前 class-like 自身会出现在容器栈里；只有当它外面还包着别的 class-like，
        // 才说明它不属于公开类型标识体系，此时不能再为它构造稳定 ClassId。
        val classLikeContainers = context.containers.filterIsInstance<CfirClassLikeDeclaration>()
        val hasOuterClassLike = classLikeContainers.any { it !== klass }
        return if (hasOuterClassLike) {
            null
        } else {
            ClassId(packageFqName, klass.name)
        }
    }

    /**
     * 为构造器 owner 构造返回类型。
     *
     * class/interface/struct/enum 分别生成对应 Cone 类型，类型实参使用 owner 自身声明的类型参数。
     */
    private fun buildConstructedTypeForClass(klass: CfirClassLikeDeclaration): ConeCangJieType {
        val classId = resolveClassId(klass)
            ?: return ConeErrorType(ConeSimpleDiagnostic("cannot resolve class id for constructor owner"))
        val typeArguments = klass.typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        val lookupTag = ConeClassLikeLookupTagImpl(classId)
        return when (klass) {
            is CfirInterface -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
                isInterface = true,
            )

            is CfirStruct -> ConeStructType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
            )

            is CfirEnum -> ConeEnumType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
                isRefEnum = klass.isRefEnum,
            )

            else -> ConeClassLikeType(
                lookupTag = lookupTag,
                typeArguments = typeArguments,
            )
        }
    }
}
