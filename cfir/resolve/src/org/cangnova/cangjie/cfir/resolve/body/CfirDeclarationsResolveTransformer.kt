package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.*
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.commonSuperTypeOrNull
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
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

    override fun transformFile(file: CfirFile, data: ResolutionMode): CfirFile =
        whileAnalysing(session, file) {
            doTransformFile(file, data)
        }

    private fun doTransformFile(file: CfirFile, data: ResolutionMode): CfirFile =
        withFile(file) {
            transformFileContent(file, data)
        }

    /**
     * 包装 file 级别的 scope / DFA 生命周期。对齐 K2 `CfirDeclarationsResolveTransformer.withFile`：
     * 进入 `context.withFile`、装配 imports、开启 file CFG，最后把 CFG 写回 file。
     */
    protected open fun withFile(file: CfirFile, action: () -> CfirFile): CfirFile {
        val savedContext = context.towerDataContext
        try {
            return context.withFile(file) {
                val importScopes = createImportingScopes(file)
                context.addNonLocalScopes(importScopes)

                dataFlowAnalyzer.enterFile(file, buildGraph = true)
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

    protected open fun transformFileContent(file: CfirFile, data: ResolutionMode): CfirFile {
        file.transformDeclarations(transformer, ResolutionMode.ContextIndependent)
        return file
    }

    // ── Class-like declarations ───────────────────────────────────────────

    override fun transformClass(klass: CfirClass, data: ResolutionMode): CfirClass =
        whileAnalysing(session, klass) {
            transformClassContent(klass, data)
        }

    override fun transformInterface(`interface`: CfirInterface, data: ResolutionMode): CfirInterface =
        whileAnalysing(session, `interface`) {
            transformInterfaceContent(`interface`, data)
        }

    override fun transformStruct(struct: CfirStruct, data: ResolutionMode): CfirStruct =
        whileAnalysing(session, struct) {
            transformStructContent(struct, data)
        }

    override fun transformEnum(enum: CfirEnum, data: ResolutionMode): CfirEnum =
        whileAnalysing(session, enum) {
            transformEnumContent(enum, data)
        }

    protected open fun transformClassContent(klass: CfirClass, data: ResolutionMode): CfirClass =
        transformClassLikeDeclaration(klass)

    protected open fun transformInterfaceContent(
        interfaceDeclaration: CfirInterface,
        data: ResolutionMode,
    ): CfirInterface = transformClassLikeDeclaration(interfaceDeclaration)

    protected open fun transformStructContent(struct: CfirStruct, data: ResolutionMode): CfirStruct =
        transformClassLikeDeclaration(struct)

    protected open fun transformEnumContent(enum: CfirEnum, data: ResolutionMode): CfirEnum =
        transformClassLikeDeclaration(enum)

    private fun <T : CfirClassLikeDeclaration> transformClassLikeDeclaration(classLike: T): T {
        val savedContext = context.towerDataContext

        context.withContainer(classLike) {
            val resolveDeclarations = {
                // 接口成员在 CFIR 树中只保留 declarations 这一条主存。
                // 不能再把它回写到并行镜像列表，否则会重新引入重复遍历。
                classLike.transformDeclarations(transformer, ResolutionMode.ContextIndependent)
            }

            when (classLike) {
                is CfirClass -> forClassBody(classLike) {
                    context.withScopesForClass(classLike, components, resolveDeclarations)
                }
                else -> context.withScopesForClass(classLike, components, resolveDeclarations)
            }
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(classLike)
        return classLike
    }

    /**
     * 进入 class body 的 DFA 区间；exit 后把 CFG 写回 class。对齐 K2 `forRegularClassBody`。
     */
    protected open fun forClassBody(klass: CfirClass, action: () -> Unit) {
        dataFlowAnalyzer.enterClass(klass, buildGraph = true)
        context.withContainingClass(klass) {
            action()
        }
        dataFlowAnalyzer.exitClass()?.let { graph ->
            klass.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
        }
    }

    // ── Function ──────────────────────────────────────────────────────────

    override fun transformFunction(
        function: CfirFunction,
        data: ResolutionMode,
    ): CfirFunction {
        error("Concrete transform functions should be called")
    }

    protected open fun transformFunctionContent(
        function: CfirFunction,
        resolutionModeForBody: ResolutionMode,
        shouldResolveEverything: Boolean,
    ): CfirFunction {
        dataFlowAnalyzer.enterFunction(function)
        val body = function.body
        if (body != null) {
            function.transformBody(transformer, resolutionModeForBody)
        }
        function.replaceControlFlowGraphReference(dataFlowAnalyzer.exitFunction(function))
        return function
    }

    // ── Constructor ───────────────────────────────────────────────────────

    override fun transformConstructor(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor =
        whileAnalysing(session, constructor) {
            if (transformer.implicitTypeOnly) return constructor
            // 仓颉的 annotation class 语义与 Kotlin 不完全一致（没有 ClassKind 体系），
            // 当前阶段不对 annotation 构造器做特殊 tower-data 切换；
            // 后续若引入 CfirClass.isAnnotation 标记，可再在此处分流到 withAnnotationContext。
            return transformConstructorContent(constructor, data)
        }

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
            // 仓颉没有独立的 delegated-constructor 节点：委托调用（this(...)/super(...)）
            // 作为 body 内的普通表达式在下面统一解析。
            context.forConstructorBody(constructor, session) {
                constructor.transformBody(transformer, data)
            }
        }

        val controlFlowGraphReference = dataFlowAnalyzer.exitFunction(constructor)
        constructor.replaceControlFlowGraphReference(controlFlowGraphReference)
        return constructor
    }

    // ── Enum constructor ──────────────────────────────────────────────────

    override fun transformEnumConstructor(
        enumConstructor: CfirEnumConstructor,
        data: ResolutionMode,
    ): CfirEnumConstructor = whileAnalysing(session, enumConstructor) {
        transformEnumConstructorContent(enumConstructor, data)
    }

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

    override fun transformProperty(property: CfirProperty, data: ResolutionMode): CfirProperty =
        whileAnalysing(session, property) {
            transformPropertyContent(property, data)
        }

    protected open fun transformPropertyContent(
        property: CfirProperty,
        data: ResolutionMode,
    ): CfirProperty {
        val savedContext = context.towerDataContext

        context.withContainer(property) {
            property.replaceReturnTypeRef(
                resolveExplicitTypeRefIfNeeded(
                    property.returnTypeRef,
                    property.typeParameters,
                )
            )
        }

        context.replaceTowerDataContext(savedContext)
        bumpPhase(property)
        return property
    }

    // ── Variable (generic fallback) ───────────────────────────────────────

    override fun transformVariable(variable: CfirVariable, data: ResolutionMode): CfirVariable =
        whileAnalysing(session, variable) {
            transformVariableContent(variable, data)
        }

    protected open fun transformVariableContent(
        variable: CfirVariable,
        data: ResolutionMode,
    ): CfirVariable {
        bumpPhase(variable)
        return variable
    }

    // ── Value parameter ───────────────────────────────────────────────────

    override fun transformValueParameter(
        valueParameter: CfirValueParameter,
        data: ResolutionMode,
    ): CfirValueParameter = whileAnalysing(session, valueParameter) {
        transformValueParameterContent(valueParameter, data)
    }

    protected open fun transformValueParameterContent(
        valueParameter: CfirValueParameter,
        data: ResolutionMode,
    ): CfirValueParameter {
        dataFlowAnalyzer.enterValueParameter(valueParameter)

        val result = context.withValueParameter(valueParameter, session) {
            // 仓颉的 annotation parameter 默认值必须是常量表达式，
            // 但常量求值由后续 checker / codegen 阶段完成，
            // 这里只负责类型推断与 DFA，不做 ArrayLiteralPosition 的注解参数分流。
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

    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: ResolutionMode,
    ): CfirFieldVariable = whileAnalysing(session, fieldVariable) {
        transformFieldVariableContent(fieldVariable, data)
    }

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

        if (fieldVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = fieldVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                fieldVariable.replaceReturnTypeRef(
                    fieldVariable.returnTypeRef.resolvedTypeFromPrototype(
                        resolvedType,
                        fieldVariable.returnTypeRef.source,
                    ),
                )
            }
        }

        context.storeVariable(fieldVariable, session)

        bumpPhase(fieldVariable)
        return fieldVariable
    }

    // ── Declaration (generic fallback) ────────────────────────────────────

    override fun transformDeclaration(declaration: CfirDeclaration, data: ResolutionMode): CfirDeclaration =
        whileAnalysing(session, declaration) {
            transformDeclarationBumpPhaseContent(declaration, data)
        }

    protected open fun transformDeclarationBumpPhaseContent(
        declaration: CfirDeclaration,
        data: ResolutionMode,
    ): CfirDeclaration {
        bumpPhase(declaration)
        return declaration
    }

    // ── Pattern binding / pattern variable ────────────────────────────────

    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: ResolutionMode,
    ): CfirPatternBindingVariable = whileAnalysing(session, patternBindingVariable) {
        transformPatternBindingVariableContent(patternBindingVariable, data)
    }

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

    override fun transformPatternVariable(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable = whileAnalysing(session, patternVariable) {
        transformPatternVariableContent(patternVariable, data)
    }

    protected open fun transformPatternVariableContent(
        patternVariable: CfirPatternVariable,
        data: ResolutionMode,
    ): CfirPatternVariable {
        val rawTypeRef = patternVariable.returnTypeRef
        if (rawTypeRef !is CfirResolvedTypeRef && rawTypeRef !is CfirImplicitTypeRef) {
            val resolved = specificTypeResolverTransformer.transformTypeRef(
                rawTypeRef,
                CfirTypeResolutionConfiguration(
                    useSiteFile = context.file,
                    topContainer = context.containers.lastOrNull(),
                ),
            )
            patternVariable.replaceReturnTypeRef(resolved)
        }

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

        if (patternVariable.returnTypeRef is CfirImplicitTypeRef) {
            val initType = patternVariable.initializer?.coneTypeOrNull
            if (initType != null) {
                val resolvedType = IdealTypeResolver.resolveIfIdeal(initType)
                patternVariable.replaceReturnTypeRef(
                    patternVariable.returnTypeRef.resolvedTypeFromPrototype(
                        resolvedType,
                        patternVariable.returnTypeRef.source,
                    ),
                )
            }
        }

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

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression =
        context.forBlock(session) {
            transformer.expressionsTransformer.transformBlock(block, data)
        }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun createImportingScopes(file: CfirFile): List<CfirScope> {
        val symbolProvider = session.symbolProvider
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
            // 声明解析阶段同样要先看到当前文件顶层声明，保证后续类型与 extend 规则建立在正确的本地符号之上。
            add(CfirFileDeclaredTopLevelScope(file))
            add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
            add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
            add(CfirExplicitStarImportingScope(imports, symbolProvider))
            add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
            add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
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
    ): CfirNamedFunction = whileAnalysing(session, namedFunction) {
        val shouldResolveEverything = !transformer.implicitTypeOnly
        val returnTypeRef = namedFunction.returnTypeRef
        if (returnTypeRef !is CfirImplicitTypeRef && transformer.implicitTypeOnly) {
            return namedFunction
        }

        val containingDeclaration = context.containerIfAny
        return context.withNamedFunction(namedFunction, session) {
            if (shouldResolveEverything) {
                namedFunction.transformTypeParameters(this, ResolutionMode.ContextIndependent)
            }

            // 局部嵌套函数（不在类体/文件顶层）需要先把签名解析好，
            // 因为类成员的签名由前面的阶段（SUPER_TYPES / STATUS）处理过。
            val isLocalNested = containingDeclaration != null &&
                    containingDeclaration !is CfirClass &&
                    containingDeclaration !is CfirFile
            if (isLocalNested) {
                prepareSignatureForBodyResolve(namedFunction)
            }

            context.forFunctionBody(namedFunction, components) {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(namedFunction, shouldResolveEverything)
                }
            }
        }
    }

    /**
     * `main` 在 CFIR 树上是独立声明节点，但 body resolve 约束仍与普通函数一致：
     * 需要走具体声明入口，而不是落回 `transformFunction()` 这个抽象兜底分发。
     */
    override fun transformMainFunction(
        mainFunction: CfirMainFunction,
        data: ResolutionMode,
    ): CfirMainFunction = whileAnalysing(session, mainFunction) {
        val shouldResolveEverything = !transformer.implicitTypeOnly
        val returnTypeRef = mainFunction.returnTypeRef
        if (returnTypeRef !is CfirImplicitTypeRef && transformer.implicitTypeOnly) {
            return mainFunction
        }

        context.withContainer(mainFunction) {
            if (shouldResolveEverything) {
                mainFunction.transformTypeParameters(this, ResolutionMode.ContextIndependent)
            }

            context.forFunctionBody(mainFunction, components) {
                withFullBodyResolve {
                    transformFunctionWithGivenSignature(mainFunction, shouldResolveEverything)
                }
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
    ): F {
        @Suppress("UNCHECKED_CAST")
        val result = transformFunctionContent(
            function,
            resolutionModeForBody = ResolutionMode.ContextIndependent,
            shouldResolveEverything = shouldResolveEverything,
        ) as F

        if (result.returnTypeRef is CfirImplicitTypeRef) {
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
     * 从当前函数 CFG 已确认可达的返回结果里推断函数返回类型。
     *
     * 返回结果统一包含：
     * - 显式 `return expr`
     * - 函数体正常流出时的 block 尾表达式
     *
     * 这样可以让“最后一条表达式是返回值”与显式 return 共享同一套推断入口，
     * 并自动排除 CFG 上不可达的块尾表达式。
     */
    private fun inferFunctionReturnType(function: CfirFunction): ConeCangJieType {
        val returnExpressions = components.dataFlowAnalyzer.returnExpressionsOfFunction(function)
        if (returnExpressions.isEmpty()) {
            return session.builtinTypes.unitType
        }

        val expressionTypes = returnExpressions.map { expression ->
            expression.coneTypeOrNull ?: ConeErrorType(ConeSimpleDiagnostic("Postponed inference"))
        }

        if (expressionTypes.size == 1) {
            return expressionTypes.single()
        }

        val commonType = session.typeContext.commonSuperTypeOrNull(expressionTypes)
        if (commonType != null && commonType !is ConeErrorType && commonType.isAcceptableInferredReturnType(expressionTypes)) {
            return commonType
        }

        val message = expressionTypes.joinToString(
            prefix = "The types ",
            postfix = " do not have the smallest common supertype",
        ) { "'$it'" }
        return ConeErrorType(ConeSimpleDiagnostic(message))
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
    private fun resolveExplicitTypeRefIfNeeded(
        typeRef: CfirTypeRef,
        additionalTypeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirTypeRef {
        if (typeRef is CfirImplicitTypeRef) return typeRef
        val typeParametersFromContainers = context.containers
            .filterIsInstance<CfirDeclaration>()
            .flatMap(::extractTypeParameters)
        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(typeParametersFromContainers + additionalTypeParameters)

        if (typeRef is CfirResolvedTypeRef) {
            val delegated = typeRef.delegatedTypeRef
            if (typeRef.coneType is ConeErrorType && delegated != null && delegated !is CfirImplicitTypeRef) {
                return specificTypeResolverTransformer.transformTypeRef(delegated, config)
            }
            return typeRef
        }

        return specificTypeResolverTransformer.transformTypeRef(
            typeRef,
            config,
        )
    }

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

    private fun bumpPhase(declaration: CfirDeclaration) {
        if (declaration.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES &&
            declaration.resolvePhase < CfirResolvePhase.BODY_RESOLVE
        ) {
            declaration.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
    }

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
