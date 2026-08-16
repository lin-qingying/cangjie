@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve.calls

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirLocalScopes
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.DeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirNamedFunctionImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirValueParameterImpl
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.impl.CfirLiteralExpressionImpl
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CfirSamResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirCallResolver
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.stages.CheckerSinkImpl
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.inference.CfirCallCompleter
import org.cangnova.cangjie.cfir.resolve.inference.ConstraintSystemCompleter
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirNullSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSyntheticCallGenerator
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.safeSubstitute
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

/**
 * 调用解析阶段测试的共享构造工具。
 *
 * 负责构造最小的测试 session（语言设置、类型系统、推断组件）、函数/参数/实参/调用点
 * 以及 [Candidate]，并驱动挂起式 [ResolutionStage] 检查与约束系统 completion。
 * 对齐 Kotlin FIR 测试中 `CallResolutionTestFixtures` 的定位；与旧版相比：
 * - 不再有自定义 [ResolutionContext] / [Candidate] / [CallInfo]，全部使用生产类型；
 * - [buildFunctionSymbol] 返回绑定的 [CfirNamedFunctionSymbol]（symbol.fir 可用）；
 * - 参数实参校验走真实的 `CfirMapArguments`/`CfirCheckArguments` 阶段。
 */
object CallResolutionTestFixtures {

    /**
     * 测试用最小 source session。
     *
     * 携带模块数据，并在创建时注册语言设置、类型组件与推断组件；
     * 需要 class 继承关系的测试额外通过 [registerHierarchyProviders] 注册符号提供器。
     */
    class TestSession : CfirSession(Kind.Source) {
        /** 当前 session 的模块数据。 */
        lateinit var moduleData: CfirModuleData
            internal set

        override fun toString(): String = "CallResolutionTestSession"
    }

    /** 各测试间共享的无状态 body resolve 上下文；调用解析阶段不读取其状态。 */
    private val STUB_BODY_RESOLVE_CONTEXT = BodyResolveContext(ReturnTypeCalculator.Default, CfirDataFlowAnalyzerContext())

    /**
     * 构造带模块数据与基础组件（语言设置、类型系统、推断组件）的测试 session。
     *
     * 同时注册空的 [CfirExtendProvider]：重载消歧（`overrides`）会无条件访问
     * `session.extendProvider`，未注册时访问器直接抛出。
     */
    fun newTestSession(): TestSession {
        val session = TestSession()
        val moduleData = CfirSourceModuleData(
            name = Name.identifier("call-resolution-test"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        )
        moduleData.bindSession(session)
        session.register(CfirModuleData::class, moduleData)
        session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
    session.register(TypeComponents::class, TypeComponents(session))
    session.register(InferenceComponents::class, InferenceComponents(session))
    session.register(CfirExtendProvider::class, TestExtendProvider(emptyList()))
    session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
    session.register(CfirSymbolProvider::class, TestSymbolProvider(session, emptyList()))
    session.register(CfirProvider::class, CfirProviderImpl(session))
        session.moduleData = moduleData
        return session
    }

    /**
     * 为 session 注册内存符号提供器，使 class 继承关系（如 `Child <: Parent`）可参与类型检查。
     *
     * [newTestSession] 已注册空 [CfirExtendProvider]；此处只补充符号提供器与类型感知父类型提供器。
     */
    fun TestSession.registerHierarchyProviders(
        declarations: List<CfirClassLikeDeclaration>,
    ) {
        register(CfirSymbolProvider::class, TestSymbolProvider(this, declarations))
        register(CfirTypeAwareSupertypeProvider::class, CfirTypeAwareSupertypeProviderImpl(this))
    }

    /**
     * 构造 body resolve 组件的桩实现；除 session 与 scope session 外全部不可用。
     *
     * 调用解析阶段只读取 session（推断组件、语言设置）与 scope session，
     * 其余访问在测试路径中不会发生。
     */
    fun newStubBodyResolveComponents(session: CfirSession): BodyResolveComponents = StubBodyResolveComponents(session)

    /**
     * 构造调用解析测试使用的 [ResolutionContext]。
     */
    fun newResolutionContext(session: CfirSession): ResolutionContext =
        ResolutionContext(session, newStubBodyResolveComponents(session), STUB_BODY_RESOLVE_CONTEXT)

    /**
     * 构造并绑定指定签名的函数符号。
     *
     * @param parameterDefaults 与 [parameterTypes] 对应的“是否有默认值”标记。
     * @param typeParameters 函数的声明类型参数（如 `newTypeParameter` 构造的实例）。
     */
    fun buildFunctionSymbol(
        session: TestSession,
        name: String,
        returnType: ConeCangJieType = ConePrimitiveType.UNIT,
        parameterTypes: List<ConeCangJieType> = emptyList(),
        parameterNames: List<String>? = null,
        parameterDefaults: List<Boolean>? = null,
        typeParameters: List<CfirTypeParameter> = emptyList(),
    ): CfirNamedFunctionSymbol {
        val symbol = CfirNamedFunctionSymbol(CallableId(FqName.ROOT, Name.identifier(name)))
        val parameters = parameterTypes.mapIndexed { index, type ->
            buildValueParameter(
                session = session,
                name = parameterNames?.getOrNull(index) ?: "p$index",
                type = type,
                hasDefault = parameterDefaults?.getOrNull(index) ?: false,
                containingDeclarationSymbol = symbol,
            )
        }
        CfirNamedFunctionImpl(
            source = null,
            moduleData = session.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            deprecationsProvider = EmptyDeprecationsProvider,
            dispatchReceiverType = null,
            status = CfirDeclarationStatusImpl(),
            typeParameters = typeParameters.toMutableList(),
            returnTypeRef = resolvedTypeRef(returnType),
            valueParameters = parameters.toMutableList(),
            body = null,
            symbol = symbol,
            name = Name.identifier(name),
            isMut = false,
        )
        return symbol
    }

    /**
     * 构造并绑定指定的值参数。
     */
    fun buildValueParameter(
        session: TestSession,
        name: String,
        type: ConeCangJieType,
        hasDefault: Boolean = false,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
    ): CfirValueParameter {
        val symbol = CfirValueParameterSymbol(CallableId(FqName.ROOT, Name.identifier(name)))
        return CfirValueParameterImpl(
            source = null,
            moduleData = session.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            deprecationsProvider = EmptyDeprecationsProvider,
            dispatchReceiverType = null,
            symbol = symbol,
            containingDeclarationSymbol = containingDeclarationSymbol,
            isNamed = false,
            status = CfirDeclarationStatusImpl(),
            typeParameters = mutableListOf(),
            returnTypeRef = resolvedTypeRef(type),
            name = Name.identifier(name),
            defaultValue = if (hasDefault) defaultLiteral() else null,
        )
    }

    /**
     * 构造带类型的字面量实参表达式。
     *
     * 字面量 kind 按类型推导（整数/浮点/布尔），新 [CfirCheckArguments] 会按
     * kind 把 IDEAL 输入重写为对应理想类型（INT→IDEAL_INT、FLOAT→IDEAL），
     * 因此这里必须与类型保持一致，否则“理想类型兼容”断言会失效。
     */
    fun buildTypedExpression(type: ConeCangJieType): CfirExpression {
        return CfirLiteralExpressionImpl(
            source = literalSource(),
            annotations = MutableOrEmptyList.empty(),
            coneTypeOrNull = type,
            kind = type.literalKind(),
            value = 0,
        )
    }

    /**
     * 构造函数调用点信息。
     *
     * callSite 必须携带非空 source：[CfirMapArguments] 的调用形状构造
     * （`createCallShape`）要求 call site 或实参带 source，否则直接报错。
     */
    fun buildCallInfo(
        session: TestSession,
        name: String,
        arguments: List<CfirExpression> = emptyList(),
        callKind: CallKind = CallKind.Function,
        resolutionMode: ResolutionMode = ResolutionMode.ContextIndependent,
    ): CallInfo {
        return CallInfo(
            callSite = CfirLiteralExpressionImpl(
                source = literalSource(),
                annotations = MutableOrEmptyList.empty(),
                coneTypeOrNull = null,
                kind = CfirLiteralKind.INT,
                value = 0,
            ),
            callKind = callKind,
            name = Name.identifier(name),
            explicitReceiver = null,
            arguments = arguments,
            isUsedAsGetClassReceiver = false,
            typeArguments = emptyList(),
            session = session,
            containingFile = ExtendTestFixtures.newFile(session.moduleData, FqName.ROOT, emptyList()),
            containingDeclarations = emptyList(),
            resolutionMode = resolutionMode,
        )
    }

    /**
     * 构造调用解析候选。
     *
     * 使用独立的空约束系统作为 base system，并绑定 call info 中的 session；
     * 候选的约束系统在首次访问时按需创建。
     */
    fun buildCandidate(
        session: TestSession,
        functionSymbol: CfirFunctionSymbol<*>,
        callInfo: CallInfo,
    ): Candidate {
        return Candidate(
            symbol = functionSymbol,
            dispatchReceiver = null,
            givenExtensionReceiver = null,
            explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
            constraintSystemFactory = session.inferenceComponents.constraintSystemFactory,
            baseSystem = session.inferenceComponents.createConstraintSystem().asReadOnlyStorage(),
            callInfo = callInfo,
            originScope = null,
            isFromCompanionObjectTypeScope = false,
            isFromOriginalTypeInPresenceOfSmartCast = false,
            bodyResolveContext = STUB_BODY_RESOLVE_CONTEXT,
        ).also { candidate ->
            candidate.initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList())
        }
    }

    /**
     * 依次运行指定阶段，直至候选检查完成或中途挂起。
     *
     * 复制生产 `ResolutionStageRunner.processCandidate` 的驱动惯用法：
     * 阶段协程在挂起（yield）时保存 continuation，外部循环负责继续恢复；
     * 测试默认不启用 stop-on-first-error，因此一次 resume 即可跑完全部阶段。
     */
    fun runStagesForTest(
        candidate: Candidate,
        context: ResolutionContext,
        vararg stages: ResolutionStage,
    ) {
        val sink = CheckerSinkImpl(candidate)
        var finished = false
        sink.continuation = suspend {
            for (stage in stages) {
                context(context, sink) { stage.check(candidate) }
            }
        }.createCoroutineUnintercepted(object : Continuation<Unit> {
            override val context: CoroutineContext get() = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                result.exceptionOrNull()?.let { throw it }
                finished = true
            }
        })
        while (!finished) {
            sink.continuation!!.resume(Unit)
        }
    }

    /**
     * 对候选运行约束系统 completion 并返回最终替换后的返回类型。
     *
     * @param expectedType 非空时先注入 `返回类型 <: expectedType` 约束，
     *   复刻生产 [CfirCallCompleter] 从期望类型建立约束的通用路径。
     */
    fun completeCallForTest(
        candidate: Candidate,
        context: ResolutionContext,
        expectedType: ConeCangJieType? = null,
    ): ConeCangJieType {
        val system = candidate.system
        val callSite = candidate.callInfo.callSite as? CfirExpression
            ?: error("call resolution fixture requires an expression call site")
        if (expectedType != null) {
            system.addSubtypeConstraint(candidate.substitutedReturnType(), expectedType, ConeExpectedTypeConstraintPosition)
        }
        ConstraintSystemCompleter(newStubBodyResolveComponents(context.session)).complete(
            system.asConstraintSystemCompleterContext(),
            ConstraintSystemCompletionMode.FULL,
            listOf(ConeAtomWithCandidate(callSite, candidate)),
            candidate.substitutedReturnType(),
            context,
        ) { _, _ -> }
        return system.buildCurrentSubstitutor().safeSubstitute(system, candidate.substitutedReturnType()).asCone()
    }

    /**
     * 构造已解析类型的 type ref。
     */
    private fun resolvedTypeRef(type: ConeCangJieType): CfirResolvedTypeRefImpl = CfirResolvedTypeRefImpl(
        source = null,
        annotations = MutableOrEmptyList.empty(),
        customRenderer = false,
        coneType = type,
        delegatedTypeRef = null,
    )

    /**
     * 构造默认值占位字面量；调用解析测试不检查默认值内容。
     */
    private fun defaultLiteral(): CfirExpression = CfirLiteralExpressionImpl(
        source = literalSource(),
        annotations = MutableOrEmptyList.empty(),
        coneTypeOrNull = null,
        kind = CfirLiteralKind.INT,
        value = 0,
    )

    /**
     * 构造满足非空要求的最小 source。
     *
     * 生产代码的表达式 source 来自 PSI/轻量树；纯单元测试没有语法树，
     * 因此这里构造带空树结构的 [CjLightSourceElement]。调用形状构造
     * （`CfirMapArguments.createCallShape`）只读取 offset，不会遍历树。
     */
    private fun literalSource(): CjSourceElement = CjLightSourceElement(
        lighterASTNode = TEST_LIGHTER_AST_NODE,
        startOffset = 0,
        endOffset = 1,
        treeStructure = TEST_TREE_STRUCTURE,
        kind = CjRealSourceElementKind,
    )

    /** 测试用伪轻量树节点：只提供偏移范围，不绑定真实语法树。 */
    private val TEST_LIGHTER_AST_NODE = object : LighterASTNode {
        override fun getTokenType(): IElementType = TokenType.DUMMY_HOLDER
        override fun getStartOffset(): Int = 0
        override fun getEndOffset(): Int = 1
    }

    /** 测试用空轻量树结构：调用形状构造只读取节点 offset，不遍历子节点。 */
    private val TEST_TREE_STRUCTURE = object : FlyweightCapableTreeStructure<LighterASTNode> {
        override fun getRoot(): LighterASTNode = TEST_LIGHTER_AST_NODE
        override fun getParent(node: LighterASTNode): LighterASTNode? = null
        override fun getChildren(node: LighterASTNode, nodesRef: Ref<Array<LighterASTNode>>): Int = 0
        override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}
        override fun getStartOffset(node: LighterASTNode): Int = node.startOffset
        override fun getEndOffset(node: LighterASTNode): Int = node.endOffset
        override fun toString(node: LighterASTNode): CharSequence = ""
    }

    /**
     * 把类型映射到字面量 kind，与 [CfirCheckArguments] 的 IDEAL 重写规则对齐。
     */
    private fun ConeCangJieType.literalKind(): CfirLiteralKind = when (this) {
        is ConePrimitiveType -> when (kind) {
            PrimitiveTypeKind.BOOLEAN -> CfirLiteralKind.BOOLEAN
            PrimitiveTypeKind.FLOAT64, PrimitiveTypeKind.IDEAL_FLOAT -> CfirLiteralKind.FLOAT
            else -> CfirLiteralKind.INT
        }

        is ConeErrorType -> CfirLiteralKind.INT
        else -> CfirLiteralKind.INT
    }

    /**
     * [BodyResolveComponents] 的测试桩：除 session 与 scope session 外不可用。
     */
    private class StubBodyResolveComponents(
        override val session: CfirSession,
    ) : BodyResolveComponents() {
        override val scopeSession: ScopeSession = ScopeSession()
        override val returnTypeCalculator: ReturnTypeCalculator get() = error("not used in call resolution tests")
        override val implicitValueStorage: ImplicitValueStorage get() = error("not used in call resolution tests")
        override val containingDeclarations: List<CfirDeclaration> get() = error("not used in call resolution tests")
        override val fileImportsScope: List<CfirScope> get() = error("not used in call resolution tests")
        override val towerDataElements: List<CfirTowerDataElement> get() = error("not used in call resolution tests")
        override val towerDataContext: CfirTowerDataContext get() = error("not used in call resolution tests")
        override val localScopes: CfirLocalScopes get() = error("not used in call resolution tests")
        override val noExpectedType: CfirTypeRef get() = error("not used in call resolution tests")
        override val symbolProvider: CfirSymbolProvider get() = error("not used in call resolution tests")
        override val file: CfirFile get() = error("not used in call resolution tests")
        override val container: CfirDeclaration get() = error("not used in call resolution tests")
        override val resolutionStageRunner: ResolutionStageRunner get() = error("not used in call resolution tests")
        override val samResolver: CfirSamResolver get() = error("not used in call resolution tests")
        override val callResolver: CfirCallResolver get() = error("not used in call resolution tests")
        override val callCompleter: CfirCallCompleter get() = error("not used in call resolution tests")
        override val syntheticCallGenerator: CfirSyntheticCallGenerator get() = error("not used in call resolution tests")
        override val dataFlowAnalyzer: CfirDataFlowAnalyzer get() = error("not used in call resolution tests")
        override val integerLiteralAndOperatorApproximationTransformer: IntegerLiteralAndOperatorApproximationTransformer
            get() = error("not used in call resolution tests")
        override val inlineFunction: CfirFunction? get() = error("not used in call resolution tests")
    }

    /**
     * 基于内存声明表的测试符号提供器（复刻 CfirTypeAwareSupertypeProviderTest）。
     */
    private class TestSymbolProvider(
        session: CfirSession,
        declarations: List<CfirClassLikeDeclaration>,
    ) : CfirSymbolProvider(session) {
        private val declarationsByClassId = declarations.associateBy { it.symbol.classId }

        override val symbolNamesProvider: CfirSymbolNamesProvider = CfirNullSymbolNamesProvider

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            declarationsByClassId[classId]?.symbol

        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
        }

        override fun hasPackage(fqName: FqName): Boolean =
            declarationsByClassId.keys.any { it.packageFqName == fqName }
    }

    /**
     * 基于内存 extend 列表的测试 extend provider（复刻 CfirTypeAwareSupertypeProviderTest）。
     */
    private class TestExtendProvider(
        extends: List<CfirExtend>,
    ) : CfirExtendProvider {
        private val extendsByClassId: Map<ClassId, List<CfirExtend>> = extends.groupBy { extend ->
            val extendedType = extend.extendedTypeRef as? CfirResolvedTypeRef
                ?: error("extend target must already be resolved in test fixtures")
            extendedType.coneType.classIdOrPrimitiveClassId
                ?: error("extend target must be classifier type in test fixtures")
        }

        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
            extendsByClassId[classId].orEmpty()

        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
            (targetKey as? CfirExtendTargetKey.ClassLike)?.let { getExtendsForClass(it.classId) } ?: emptyList()

        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
            extendsByClassId
                .filterKeys { it.packageFqName == packageFqName }
                .values
                .flatten()

        override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> =
            extendsByClassId[kind.classId].orEmpty()
    }
}
