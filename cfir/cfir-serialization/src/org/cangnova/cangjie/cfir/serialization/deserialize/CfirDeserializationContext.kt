package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.Package
import PackageFormat.Position
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.source.CfirSourceCallableSignature
import org.cangnova.cangjie.cfir.source.CfirImportedDeclarationHeaderSource
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirConstantValueExpression
import org.cangnova.cangjie.cfir.CfirEvaluatorResult
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBuiltInTypeSymbol
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourcePositionResolver
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceExportSnapshot
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourcePosition
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedConstantStorage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoConstantRootSlot
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedConstantRoot
import org.cangnova.cangjie.cfir.source.CfirSourcePositionIdentity
import org.cangnova.cangjie.cfir.source.CfirSourceImportedPosition
import org.cangnova.cangjie.cfir.source.patterns
import org.cangnova.cangjie.cfir.session.implicitSystemAnnotations
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.source.CjSourceLocation
import java.util.concurrent.ConcurrentHashMap

/** loaded Generic 的原分组与最终 GenericTyInfo 上界分开，重复/位置/派生标记完整保留。 */
data class CfirDeserializedGenericConstraint(
    val subject: ConeCangJieType,
    val upperBounds: List<CfirTypeRef>,
    val isImplicitlyIntroduced: Boolean,
    val begin: CjSourceLocation?,
    val end: CjSourceLocation?,
)

/**
 * 反序列化上下文。
 *
 * 该对象承载当前包数据、跨包加载能力以及面向 `FullId` 的统一解析入口。
 */
class CfirDeserializationContext(
    /** 当前反序列化的 FlatBuffers Package。 */
    val pkg: Package,
    /** 当前包头。 */
    val header: CjoPackageHeader,
    /** 库模块元数据。 */
    val moduleData: CfirModuleData,
    /** `.cjo` 包管理器，用于跨包声明索引和包头装载。 */
    val cjoManager: CjoManager,
    /** 实际读取 CJO 的路径；内存/旧调用方默认不加载 sidecar。 */
    val sourcePath: java.nio.file.Path? = null,
    /** 从 session-owned 平台配置读取隐式系统注解；绝不从 sourcePath 推断。 */
    val implicitSystemAnnotations: Set<org.cangnova.cangjie.name.FqName> =
        moduleData.session.implicitSystemAnnotations,
    /** 通过所属 deserialized provider 的真实 package context 解析跨包声明索引。 */
    private val initializationDependencyResolver: ((java.nio.file.Path?, String, Int, Int) -> CfirDeclaration?)? = null,
    /** FullId 目标通过实际快照所属 provider 物化；跨包参数不创建同名替身。 */
    private val declarationResolver: ((java.nio.file.Path?, String, Int) -> CfirDeclaration?)? = null,
    /** 由请求输入的真实依赖图选择目标快照，多候选无选择证据时必须拒绝。 */
    private val dependencySnapshotResolver: ((String) -> org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage?)? = null,
    /** 正式 provider 所属实际装载身份；FlatBuffers accessor wrapper 不能替代此 token。 */
    internal val loadedPackage: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage? = null,
    /** 注解映射仅请求实际遗漏参数的默认值，不授目标自己的注解或正文。 */
    private val parameterDefaultsResolver: ((java.nio.file.Path?, String, List<Int>) -> Map<CfirValueParameter, org.cangnova.cangjie.cfir.expressions.CfirExpression?>)? = null,
) {
    /** 同一包的节点、类型和参数保留一个可重入事务锁，避免逐槽锁的交叉依赖死锁。 */
    internal val materializationLock = Any()
    /** 仅事务内可见的真实参数壳；完成上界和元数据后才进入公开声明缓存。 */
    internal val reservedTypeParameters = HashMap<Int, CfirTypeParameter>()
    internal val genericTypes = HashMap<CfirTypeParameterSymbol, ConeTypeParameterType>()
    internal val genericBounds = HashMap<CfirTypeParameterSymbol, List<CfirTypeRef>>()
    internal val genericBoundsInProgress = HashSet<CfirTypeParameterSymbol>()
    /** 名义身份先于 self/header；仅本次事务可见，不写入旧 Cone/tag 或公开 classifier cache。 */
    internal val reservedClassLikeSymbols = HashMap<Int, CfirClassLikeSymbol<*>>()
    internal val classLikeHeadersInProgress = HashSet<Int>()
    private val pendingClassLikeHeaders = LinkedHashSet<Int>()
    private val pendingGenericTypes = LinkedHashSet<Int>()
    private val pendingClassLikeSelections = HashMap<Int, CjoSourceContentSelection>()
    private val pendingGenericSelections = HashMap<Int, CjoSourceContentSelection>()

    internal fun enqueueGenericType(index: Int) {
        val selection = checkNotNull(currentSourceSelection)
        pendingGenericTypes.add(index)
        pendingGenericSelections.putIfAbsent(index, selection)
    }
    internal fun finishGenericType(index: Int) {
        pendingGenericTypes.remove(index)
        pendingGenericSelections.remove(index)
    }

    /**
     * 反序列化必须消费所属 session 的同一份语言设置；不能为 CJO 重新创建默认设置。
     */
    val languageVersionSettings: LanguageVersionSettings
        get() = moduleData.session.languageVersionSettings

    /** 在首次声明物化前完成快照与匹配计划；整代 context/provider/session 重建才会重新读取。 */
    val sidecar: org.cangnova.cangjie.cfir.serialization.cjd.CjdBinaryAnnotationOverlay? by lazy {
        sourcePath?.let { org.cangnova.cangjie.cfir.serialization.cjd.CjdBinaryAnnotationOverlay.load(this, it) }
    }
    private data class CompletedGraph(
        val types: Map<Int, ConeCangJieType>,
        val declarations: Map<Int, CfirDeclaration>,
        val constraints: Map<CfirBasedSymbol<*>, List<CfirDeserializedGenericConstraint>>,
        val source: CfirDeserializedSourceGraph?,
        val headerSource: CfirDeserializedSourceGraph?,
        val sourceHeaders: Map<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration, org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader>,
        val sourceFunctions: Map<CjoSourceFunctionIdentity, org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>,
        val constantValues: Map<UInt, CfirConstantValue>,
        val constantRoots: Map<CjoConstantRootSlot, CjoSavedConstantRoot>,
        val constantStorage: Map<CfirConstantValue, CjoSavedConstantStorage>,
        val sourcePositions: Map<CfirSourcePositionIdentity, CjoSavedSourcePosition>,
        val callableSignatures: Map<CfirCallableDeclaration, CfirSourceCallableSignature>,
        val initializationOwners: Set<Int>,
        val bodies: CfirBodyDeserializationSnapshot,
        val sourceRegistry: CjoSourceOwnershipPlan? = null,
        val sourceGraphPublications: Map<org.cangnova.cangjie.cfir.source.CfirSourceGraph, org.cangnova.cangjie.cfir.source.CfirSourceGraph> = emptyMap(),
        val definitionScopes: Map<Any, DefinitionScope> = emptyMap(),
        val positionFileBindings: Map<org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference, org.cangnova.cangjie.cfir.serialization.cjo.CjoPositionFileBinding> = emptyMap(),
    )
    @Volatile
    private var completedGraph = CompletedGraph(emptyMap(), emptyMap(), emptyMap(), null, null, emptyMap(), emptyMap(), emptyMap(),
        emptyMap(), emptyMap(), emptyMap(), emptyMap(), emptySet(), CfirBodyDeserializationSnapshot())
    internal var bodyState = CfirBodyDeserializationState()
    private var sourceRegistry: CjoSourceOwnershipPlan? = null
    /** 仅memo已发表metadata的验证请求；Context/Package生命周期固定，不缓存失败或工作域发现。 */
    private val completedMetadataDemands = ConcurrentHashMap<CjoSourceContentRoot, CjoSourceContentSelection>()
    internal fun metadataDemand(root: CjoSourceContentRoot): CjoSourceContentSelection {
        check(root is CjoSourceContentRoot.DeclarationHeader || root is CjoSourceContentRoot.TypeMetadata)
        if (!inMaterialization) completedMetadataDemands[root]?.let { return it }
        val selection = CjoSourceOwnershipPlan.request(sourceHeaderRegistry, listOf(root))
        if (!inMaterialization && selection.borrowedContexts.declarations.keys.all { it.toInt() - 1 in declCache } &&
            selection.types.keys.all { it.toInt() - 1 in typeCache }) completedMetadataDemands.putIfAbsent(root, selection)
        return selection
    }
    /** 同包metadata登记目录只在根完成代发表；未选正文不进入registry。 */
    internal val sourceHeaderRegistry: CjoSourceOwnershipPlan
        get() {
            if (!inMaterialization) completedGraph.sourceRegistry?.let { return it }
            return materialize {
                sourceRegistry ?: CjoSourceOwnershipPlan.headers(pkg,
                    if (header.isCjmpCommonPart) CjoSourceHeaderBindingPolicy.COMMON_DEPENDENCIES
                    else CjoSourceHeaderBindingPolicy.STANDARD).also { sourceRegistry = it }
            }
        }

    internal fun registerSourceSelection(selection: CjoSourceContentSelection) {
        check(transactionDepth != 0 && selection.registeredHeaders.ownerPackage === pkg)
        sourceRegistry = selection.registeredHeaders
    }

    internal fun registerDiscoveredHeaders(registry: CjoSourceOwnershipPlan) {
        check(inMaterialization && registry.kind == CjoSourcePlanKind.HEADER_REGISTRY && registry.ownerPackage === pkg)
        sourceRegistry = registry
    }
    internal fun isPublishedDeclaration(declaration: CfirDeclaration): Boolean =
        completedGraph.declarations.values.any { it === declaration } ||
            completedGraph.sourceFunctions.values.any { it === declaration }

    internal fun sourceFunctionIdentity(function: org.cangnova.cangjie.cfir.declarations.CfirFunction): CjoSourceFunctionIdentity =
        (if (inMaterialization) sourceFunctions else completedGraph.sourceFunctions).entries.single { it.value === function }.key

    /** 实际 Package 的唯一被动数据源；读取只捕获已发布快照，不进入 materialize。 */
    private val readOnlyContentDepth = ThreadLocal.withInitial { 0 }
    internal val publishedContentSource: CfirImportedDeclarationHeaderSource = object : CfirImportedDeclarationHeaderSource,
        org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceGraphPublicationOwner {
        override fun snapshot(): org.cangnova.cangjie.cfir.CfirPublishedContentSnapshot = completedGraph.bodies
        override val binderIdentity: Any get() = sourceExportBinderIdentity
        override fun completedSnapshotOf(original: org.cangnova.cangjie.cfir.source.CfirSourceGraph): CjoSourceExportSnapshot = completedGraphSnapshotOf(original)
        override fun currentPublicationIdentity(): Any = synchronized(materializationLock) {
            check(transactionDepth == 0) { "SOURCE export cannot observe a running materialization transaction" }
            completedGraph
        }
        override fun requirePublicationUnchanged(publicationIdentity: Any) = synchronized(materializationLock) {
            check(transactionDepth == 0 && completedGraph === publicationIdentity) { "SOURCE export publication changed after final preparation" }
        }

        override fun completeHeader(declaration: CfirDeclaration) {
            check(declaration.symbol.isBound && declaration.symbol.cfir === declaration &&
                declaration.contentSource === this && isPublishedDeclaration(declaration)) {
                "Imported header completion requires its original bound declaration and content owner"
            }
            createDeclDeserializer().deserializeHeaderContents(declaration)
        }

        override fun <T> withReadOnlyContent(action: () -> T): T {
            val previous = readOnlyContentDepth.get()
            readOnlyContentDepth.set(previous + 1)
            return try { action() } finally { readOnlyContentDepth.set(previous) }
        }
    }

    /** 仅向根事务工作集登记正文；所有真实节点的 getter 仍观察上一完成代。 */
    internal fun publishFunctionBody(function: CfirCallableDeclaration, body: org.cangnova.cangjie.cfir.expressions.CfirBlock) {
        check(transactionDepth != 0 && function is org.cangnova.cangjie.cfir.CfirFunctionBodyOwner)
        check(function.symbol.isBound && function.symbol.cfir === function)
        check(function.contentSource === publishedContentSource) { "The F owner must use its actual package content source" }
        check(bodyState.bodies.putIfAbsent(function, body).let { it == null || it === body }) {
            "A canonical F cannot acquire a different completed body"
        }
    }

    /** initializer 与正文同代发布；新声明及旧声明均不得直接写普通字段。 */
    internal fun publishInitializer(variable: org.cangnova.cangjie.cfir.declarations.CfirVariable, initializer: org.cangnova.cangjie.cfir.expressions.CfirExpression) {
        check(transactionDepth != 0 && variable.symbol.isBound && variable.symbol.cfir === variable)
        check(variable.contentSource === publishedContentSource) { "The initializer owner must use its actual package content source" }
        check(bodyState.initializers.putIfAbsent(variable, initializer).let { it == null || it === initializer }) {
            "A canonical variable cannot acquire a different completed initializer"
        }
    }

    /** 默认值只归原参数；身份与普通正文/注解在同一个根完成代可见。 */
    internal fun publishDefaultValue(parameter: CfirValueParameter, expression: org.cangnova.cangjie.cfir.expressions.CfirExpression) {
        check(transactionDepth != 0 && parameter.symbol.isBound && parameter.symbol.cfir === parameter)
        check(parameter.contentSource === publishedContentSource)
        check(bodyState.defaults.putIfAbsent(parameter, expression).let { it == null || it === expression }) { "A canonical parameter cannot acquire another completed default" }
    }

    /** inline AnnoArgument的named wrapper没有独立e槽，其来源必须指向同一真实argument e。 */
    internal fun registerSourceOrigin(element: org.cangnova.cangjie.cfir.CfirElement, reference: UInt) {
        check(transactionDepth != 0 && reference in bodyState.records)
        val identity = bodyState.identities.getValue(reference)
        check(bodyState.additionalOrigins.putIfAbsent(element, identity).let { it == null || it === identity })
    }

    /** 注解工作只写未发布列表；已有完成声明的注解节点不得被后续事务改写。 */
    internal fun stageAnnotations(declaration: CfirDeclaration, annotations: List<org.cangnova.cangjie.cfir.expressions.CfirAnnotation>) {
        check(transactionDepth != 0 && declaration.symbol.isBound && declaration.contentSource === publishedContentSource)
        check(declaration !in bodyState.annotationContents) { "Completed annotation nodes cannot be rebound or replaced" }
        check(bodyState.annotationDrafts.putIfAbsent(declaration, annotations).let { it == null || it === annotations })
    }

    /** 旧Unselected header可以首次完成内容；旧Complete产品及其nodes永久复用，不能再次写入。 */
    internal fun publishAnnotations(declaration: CfirDeclaration, product: org.cangnova.cangjie.cfir.CfirPublishedAnnotations.Complete) {
        check(transactionDepth != 0 && declaration.symbol.isBound && declaration.contentSource === publishedContentSource)
        check(declaration !in completedGraph.bodies.annotationContents) { "A completed annotation product cannot be replaced" }
        check(bodyState.annotationDrafts.remove(declaration) != null) { "Annotation metadata requires its actual binding draft" }
        check(bodyState.annotationContents.putIfAbsent(declaration, product) == null)
    }
    private var initializationOwners = linkedSetOf<Int>()
    private var initializationRestorer: ((Set<Int>) -> Set<Int>)? = null
    /** 每个新header都会产生依赖完成工作，包括依赖向量为空的新owner。 */
    private var initializationWorkPending = false
    private val initializationSelections = ArrayDeque<CjoSourceContentSelection>()
    private val queuedInitializationSelections = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CjoSourceContentSelection, Boolean>())

    /** metadata header的真实依赖也在第一次根提交前完成，不等normal注解升级才改旧字段。 */
    internal fun enqueueHeaderDependencies() {
        if (!header.isCjmpCommonPart) return
        val selection = checkNotNull(currentSourceSelection)
        if (queuedInitializationSelections.add(selection)) initializationSelections.addLast(selection)
        initializationWorkPending = true
    }

    /** 初始化依赖由共同根事务完成；type-first和公开声明入口不得各自后补旧字段。 */
    internal fun registerInitializationRestorer(restore: (Set<Int>) -> Set<Int>) {
        if (initializationRestorer == null) initializationRestorer = restore
    }
    private var callableSignatures = java.util.IdentityHashMap<CfirCallableDeclaration, CfirSourceCallableSignature>()

    /** 正式 binder 一次捕获语义签名；header/decoder 只查询该产品，不再读取活动 returnTypeRef。 */
    internal fun registerCallableSignature(function: CfirCallableDeclaration, signature: CfirSourceCallableSignature) {
        check(transactionDepth != 0 && function.symbol.isBound)
        check(callableSignatures.putIfAbsent(function, signature).let { it == null || it == signature }) {
            "A canonical callable cannot acquire a different semantic signature"
        }
    }

    internal fun callableSignature(function: CfirCallableDeclaration): CfirSourceCallableSignature =
        (if (transactionDepth != 0) callableSignatures else completedGraph.callableSignatures).getValue(function)
    internal var constantValues = HashMap<UInt, CfirConstantValue>()
    internal val constantValuesInProgress = HashSet<UInt>()
    internal var constantRoots = HashMap<CjoConstantRootSlot, CjoSavedConstantRoot>()
    internal var constantStorage = java.util.IdentityHashMap<CfirConstantValue, CjoSavedConstantStorage>()
    private var sourcePositions = java.util.IdentityHashMap<CfirSourcePositionIdentity, CjoSavedSourcePosition>()
    private class ConstantCompletion(val selection: CjoSourceContentSelection, val complete: () -> Unit)
    private val pendingConstantInitializers = ArrayDeque<ConstantCompletion>()
    /** 声明身份/header先完成，注解及其派生产品在同一根事务的后置完成阶段装配。 */
    private class DeclarationCompletion(
        val declaration: CfirDeclaration,
        val selection: CjoSourceContentSelection?,
        val complete: () -> Unit,
    )
    private val pendingDeclarationCompletions = ArrayDeque<DeclarationCompletion>()
    private val pendingAnnotationBindings = ArrayDeque<DeclarationCompletion>()
    private val pendingDeclarationMetadata = ArrayDeque<DeclarationCompletion>()
    private val queuedDeclarationCompletions = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<CfirDeclaration, Boolean>(),
    )
    private val declarationCompletionsInProgress = java.util.Collections.newSetFromMap(
        java.util.IdentityHashMap<CfirDeclaration, Boolean>(),
    )
    private class HeaderContentRequest(var includeInitializers: Boolean, var selection: CjoSourceContentSelection) { var started: Boolean = false }
    private val headerContentRequests = java.util.IdentityHashMap<CfirDeclaration, HeaderContentRequest>()

    /** 同一header的显式normal需求合并到根工作项；只合并已请求策略，不修改旧完成声明。 */
    internal fun enqueueHeaderContents(declaration: CfirDeclaration, includeInitializers: Boolean, selection: CjoSourceContentSelection, complete: (Boolean) -> Unit) {
        headerContentRequests[declaration]?.let { request ->
            check(!request.started || !includeInitializers || request.includeInitializers) {
                "A running header completion cannot acquire a new initializer demand"
            }
            request.includeInitializers = request.includeInitializers || includeInitializers
            if (!request.started) request.selection = CjoSourceOwnershipPlan.request(sourceHeaderRegistry,
                (request.selection.roots + selection.roots).distinct()).also(::registerSourceSelection)
            return
        }
        val request = HeaderContentRequest(includeInitializers, selection)
        headerContentRequests[declaration] = request
        withSourceSelection(null) {
            enqueueDeclarationCompletion(declaration) {
                request.started = true
                withSourceSelection(request.selection) { complete(request.includeInitializers) }
            }
        }
    }

    internal fun enqueueDeclarationCompletion(declaration: CfirDeclaration, complete: () -> Unit) {
        check(transactionDepth != 0 && declaration.symbol.isBound && queuedDeclarationCompletions.add(declaration)) {
            "A declaration header must be bound exactly once before its completion is queued"
        }
        check(declarations.values.any { it === declaration }) { "Declaration completion requires its registered header" }
        pendingDeclarationCompletions.addLast(DeclarationCompletion(declaration, currentSourceSelection, complete))
    }

    internal fun enqueueAnnotationBinding(owner: CfirDeclaration, bind: () -> Unit) {
        check(transactionDepth != 0 && owner.symbol.isBound)
        pendingAnnotationBindings.addLast(DeclarationCompletion(owner, currentSourceSelection, bind))
    }

    internal fun enqueueDeclarationMetadata(declaration: CfirDeclaration, complete: () -> Unit) {
        check(transactionDepth != 0 && declaration.symbol.isBound)
        pendingDeclarationMetadata.addLast(DeclarationCompletion(declaration, currentSourceSelection, complete))
    }

    private fun runDeclarationCompletion(pending: DeclarationCompletion) {
        check(declarationCompletionsInProgress.add(pending.declaration))
        try {
            withSourceSelection(pending.selection, pending.complete)
        } finally {
            check(declarationCompletionsInProgress.remove(pending.declaration))
        }
    }

    private fun hasHeaderWork(): Boolean = pendingDeclarationCompletions.isNotEmpty() ||
        pendingClassLikeHeaders.isNotEmpty() || pendingGenericTypes.isNotEmpty() || initializationWorkPending

    /** 类型、声明与owner依赖互相产生新工作；下游阶段只能消费同一个完整header闭包。 */
    private fun completeHeaderWork() {
        do {
            completeTypeHeaders()
            if (initializationWorkPending) {
                val restore = checkNotNull(initializationRestorer) { "Header dependencies require their registered producer" }
                while (initializationSelections.isNotEmpty()) {
                    val selection = initializationSelections.removeFirst()
                    withSourceSelection(selection) { initializationOwners.addAll(restore(initializationOwners.toSet())) }
                }
                initializationWorkPending = false
            }
            while (pendingDeclarationCompletions.isNotEmpty()) {
                completeTypeHeaders()
                runDeclarationCompletion(pendingDeclarationCompletions.removeFirst())
            }
        } while (hasHeaderWork())
    }

    /** 后置工作可登记其所需的新header；排空所有工作和类型header之后才允许联合发布。 */
    internal fun completeDeclarationHeaders() {
        check(declarationCompletionsInProgress.isEmpty()) {
            "A declaration completion cannot force whole-package SOURCE materialization"
        }
        do {
            completeHeaderWork()
            while (pendingAnnotationBindings.isNotEmpty() && !hasHeaderWork()) {
                runDeclarationCompletion(pendingAnnotationBindings.removeFirst())
            }
            if (hasHeaderWork() || pendingAnnotationBindings.isNotEmpty()) continue
            while (pendingDeclarationMetadata.isNotEmpty() && !hasHeaderWork() && pendingAnnotationBindings.isEmpty()) {
                runDeclarationCompletion(pendingDeclarationMetadata.removeFirst())
            }
            completeConstantInitializers()
        } while (hasHeaderWork() || pendingAnnotationBindings.isNotEmpty() ||
            pendingDeclarationMetadata.isNotEmpty() || pendingConstantInitializers.isNotEmpty() ||
            pendingClassLikeHeaders.isNotEmpty() || pendingGenericTypes.isNotEmpty())
    }
    private val queuedConstantVariables = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirFieldVariable, Boolean>())
    /** 同一 container 的 VarInfo/Pattern/v 池使用唯一语义 helper；所有调用都在本 context 事务内。 */
    internal val constDeserializer: CfirConstDeserializer by lazy { CfirConstDeserializer(this) }
    /** Header-first 的匿名 F 与后续 SOURCE 使用同一内联 table 身份；随完成图原子发布。 */
    internal var sourceFunctions = HashMap<CjoSourceFunctionIdentity, org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration>()
    internal val completedSourceHeaders get() = completedGraph.sourceHeaders
    internal var sourceHeaders = HashMap<org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration, org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader>()
    internal val completedSourceGraph: CfirDeserializedSourceGraph? get() = completedGraph.source
    internal var sourceGraph: CfirDeserializedSourceGraph? = null
    internal val completedHeaderSourceGraph: CfirDeserializedSourceGraph? get() = completedGraph.headerSource
    internal var headerSourceGraph: CfirDeserializedSourceGraph? = null
    /** 公开查询只见完成图；一次 volatile 发布同时切换声明与类型缓存。 */
    val typeCache: Map<Int, ConeCangJieType> get() = completedGraph.types
    val declCache: Map<Int, CfirDeclaration> get() = completedGraph.declarations
    val genericConstraints: Map<CfirBasedSymbol<*>, List<CfirDeserializedGenericConstraint>>
        get() = completedGraph.constraints
    internal var constraints: MutableMap<CfirBasedSymbol<*>, List<CfirDeserializedGenericConstraint>> = HashMap()
        private set
    internal var types: MutableMap<Int, ConeCangJieType> = HashMap()
        private set
    internal var declarations: MutableMap<Int, CfirDeclaration> = HashMap()
        private set
    private var transactionDepth = 0
    private class DefinitionScope(
        val request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest,
        val roots: List<CjoSourceContentRoot>, val declarations: List<UInt>,
        val functions: Set<CjoSourceFunctionIdentity>, val patterns: Set<Int>,
        val positionFiles: Set<org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference>,
    )
    private var definitionScopes = java.util.IdentityHashMap<Any, DefinitionScope>()
    private var positionFileBindings = HashMap<org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference, org.cangnova.cangjie.cfir.serialization.cjo.CjoPositionFileBinding>()

    /** 壳、引用与上界在同一事务完成后原子发布；失败/取消不留下本次部分缓存。 */
    internal fun <T> materialize(action: () -> T): T = synchronized(materializationLock) {
        check(readOnlyContentDepth.get() == 0) { "Imported content materialization is forbidden in a read-only input region" }
        val root = transactionDepth == 0
        val oldGenericTypes = if (root) genericTypes.toMap() else emptyMap()
        val oldGenericBounds = if (root) genericBounds.toMap() else emptyMap()
        if (root) {
            types = HashMap(completedGraph.types)
            declarations = HashMap(completedGraph.declarations)
            constraints = HashMap(completedGraph.constraints)
            sourceGraph = completedGraph.source
            headerSourceGraph = completedGraph.headerSource
            sourceHeaders = HashMap(completedGraph.sourceHeaders)
            sourceFunctions = HashMap(completedGraph.sourceFunctions)
            constantValues = HashMap(completedGraph.constantValues)
            constantRoots = HashMap(completedGraph.constantRoots)
            constantStorage = java.util.IdentityHashMap(completedGraph.constantStorage)
            sourcePositions = java.util.IdentityHashMap(completedGraph.sourcePositions)
            callableSignatures = java.util.IdentityHashMap(completedGraph.callableSignatures)
            initializationOwners = LinkedHashSet(completedGraph.initializationOwners)
            bodyState = CfirBodyDeserializationState(completedGraph.bodies)
            sourceRegistry = completedGraph.sourceRegistry
            definitionScopes = java.util.IdentityHashMap(completedGraph.definitionScopes)
            positionFileBindings = HashMap(completedGraph.positionFileBindings)
        }
        transactionDepth++
        try {
            val result = action()
            if (root) {
                completeDeclarationHeaders()
                check(!hasHeaderWork()) { "CJO cannot publish incomplete header or owner dependencies" }
                check(declarationCompletionsInProgress.isEmpty()) { "CJO cannot publish a running declaration completion" }
                check(reservedTypeParameters.isEmpty() && genericBoundsInProgress.isEmpty()) {
                    "CJO materialization cannot publish incomplete generic owners"
                }
                check(constantValuesInProgress.isEmpty()) { "CJO materialization cannot publish incomplete constant values" }
                check(sourceFunctions.values.all { sourceHeaders.containsKey(it) }) {
                    "CJO materialization cannot publish an incomplete FuncBody header"
                }
                check(reservedClassLikeSymbols.isEmpty() && pendingClassLikeHeaders.isEmpty() && classLikeHeadersInProgress.isEmpty() && pendingGenericTypes.isEmpty()) {
                    "CJO materialization cannot publish incomplete classifier headers"
                }
                val ordinary = bodyState.identities.mapNotNull { (reference, identity) ->
                    bodyState.expressions[identity]?.let { reference to it }
                }.toMap()
                val aliasGraphs = java.util.IdentityHashMap<org.cangnova.cangjie.cfir.source.CfirSourceGraph, org.cangnova.cangjie.cfir.source.CfirSourceGraph>()
                val publications = java.util.IdentityHashMap(completedGraph.sourceGraphPublications)
                fun aliased(graph: org.cangnova.cangjie.cfir.source.CfirSourceGraph): org.cangnova.cangjie.cfir.source.CfirSourceGraph =
                    aliasGraphs.getOrPut(graph) {
                        check((graph.provenance as? org.cangnova.cangjie.cfir.source.CfirSourceGraphProvenance.Published)?.owner === sourceGraphPublicationOwner) {
                            "SOURCE publication requires the graph's actual decoding owner"
                        }
                        val aliases = java.util.IdentityHashMap<org.cangnova.cangjie.cfir.CfirElement, org.cangnova.cangjie.cfir.source.CfirSourceExpressionIdentity>()
                        ordinary.forEach { (reference, expression) ->
                            val identity = bodyState.identities.getValue(reference)
                            if (identity in graph.expressions) aliases[expression] = identity
                        }
                        bodyState.additionalOrigins.forEach { (element, identity) ->
                            if (identity in graph.expressions) aliases[element] = identity
                        }
                        graph.withOriginAliases(aliases).also { final ->
                            // 只继承已成功发布的历史 key；本事务未发表的别名前草稿不被认领。
                            publications.entries.forEach { if (it.value === graph) it.setValue(final) }
                            publications[final] = final
                        }
                    }
                sourceGraph = sourceGraph?.let { it.withGraph(aliased(it.graph)) }
                headerSourceGraph = headerSourceGraph?.let { it.withGraph(aliased(it.graph)) }
                bodyState.remapSourceGraphs(::aliased)
                val bodySnapshot = bodyState.snapshot()
                val publication = CompletedGraph(
                    java.util.Collections.unmodifiableMap(HashMap(types)),
                    java.util.Collections.unmodifiableMap(HashMap(declarations)),
                    java.util.Collections.unmodifiableMap(HashMap(constraints)),
                    sourceGraph,
                    headerSourceGraph,
                    java.util.Collections.unmodifiableMap(HashMap(sourceHeaders)),
                    java.util.Collections.unmodifiableMap(HashMap(sourceFunctions)),
                    java.util.Collections.unmodifiableMap(HashMap(constantValues)),
                    java.util.Collections.unmodifiableMap(HashMap(constantRoots)),
                    java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(constantStorage)),
                    java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(sourcePositions)),
                    java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(callableSignatures)),
                    java.util.Collections.unmodifiableSet(LinkedHashSet(initializationOwners)),
                    bodySnapshot,
                    sourceRegistry,
                    java.util.Collections.unmodifiableMap(publications),
                    java.util.Collections.unmodifiableMap(java.util.IdentityHashMap(definitionScopes)),
                    java.util.Collections.unmodifiableMap(HashMap(positionFileBindings)),
                )
                completedGraph = publication
            }
            result
        } catch (failure: Throwable) {
            if (root) {
                reservedTypeParameters.clear()
                genericBoundsInProgress.clear()
                genericTypes.clear()
                genericTypes.putAll(oldGenericTypes)
                genericBounds.clear()
                genericBounds.putAll(oldGenericBounds)
            }
            throw failure
        } finally {
            transactionDepth--
            if (root) {
                types = HashMap()
                declarations = HashMap()
                constraints = HashMap()
                sourceGraph = null
                headerSourceGraph = null
                sourceHeaders = HashMap()
                sourceFunctions = HashMap()
                constantValues = HashMap()
                constantRoots = HashMap()
                constantStorage = java.util.IdentityHashMap()
                sourcePositions = java.util.IdentityHashMap()
                callableSignatures = java.util.IdentityHashMap()
                initializationOwners = linkedSetOf()
                initializationWorkPending = false
                initializationSelections.clear()
                queuedInitializationSelections.clear()
                bodyState = CfirBodyDeserializationState()
                sourceRegistry = null
                constantValuesInProgress.clear()
                pendingConstantInitializers.clear()
                pendingDeclarationCompletions.clear()
                pendingAnnotationBindings.clear()
                pendingDeclarationMetadata.clear()
                queuedDeclarationCompletions.clear()
                headerContentRequests.clear()
                declarationCompletionsInProgress.clear()
                queuedConstantVariables.clear()
                reservedClassLikeSymbols.clear()
                pendingClassLikeHeaders.clear()
                pendingClassLikeSelections.clear()
                classLikeHeadersInProgress.clear()
                pendingGenericTypes.clear()
                pendingGenericSelections.clear()
            }
        }
    }

    /**
     * 声明 header 的 identity/字段先完成，再填保存的值；类自身 static const 不可重入未完成类。
     * 新值及 evaluatedInitializer 在同一根事务完成后发布，不改写此前完成的值或字段集合。
     */
    internal fun enqueueConstantInitializer(variable: CfirFieldVariable, info: PackageFormat.VarInfo, typeReference: UInt, hostPosition: Int) {
        check(transactionDepth != 0 && queuedConstantVariables.add(variable))
        pendingConstantInitializers.addLast(ConstantCompletion(checkNotNull(currentSourceSelection)) {
            val value = checkNotNull(constDeserializer.read(info, typeReference, hostPosition))
            variable.evaluatedInitializer = CfirEvaluatorResult.Evaluated(
                CfirConstantValueExpression(source = null, constantValue = value),
            )
        })
    }

    /** 允许值的真实 classifier header 继续登记常量依赖，直到整个相连值产品完成。 */
    internal fun completeConstantInitializers() {
        check(transactionDepth != 0)
        while (pendingConstantInitializers.isNotEmpty()) {
            completeTypeHeaders()
            val pending = pendingConstantInitializers.removeFirst()
            withSourceSelection(pending.selection, pending.complete)
        }
        completeTypeHeaders()
    }

    /** Type-first 与 declaration-first 同一个 d 身份；先登记自身 TP，再允许解析递归 self 类型。 */
    internal fun reserveClassLikeSymbol(index: Int): CfirClassLikeSymbol<*> {
        requireSourceDeclaration(index)
        (declarations[index] as? CfirClassLikeDeclaration)?.let { return it.symbol }
        reservedClassLikeSymbols[index]?.let { return it }
        val wire = checkNotNull(pkg.allDecls(index))
        check(sourceHeaderRegistry.declarations.containsKey((index + 1).toUInt())) { "CJO classifier has no owning root" }
        val classId = checkNotNull(fullIdResolver.resolveClassIdOfDeclaration(index))
        val symbol = when (wire.kind) {
            PackageFormat.DeclKind.ClassDecl -> CfirClassSymbol(classId, useFixedSymbolLookupTag = true)
            PackageFormat.DeclKind.InterfaceDecl -> CfirInterfaceSymbol(classId, useFixedSymbolLookupTag = true)
            PackageFormat.DeclKind.StructDecl -> CfirStructSymbol(classId, useFixedSymbolLookupTag = true)
            PackageFormat.DeclKind.EnumDecl -> CfirEnumSymbol(classId, isRefEnum = false, useFixedSymbolLookupTag = true)
            PackageFormat.DeclKind.TypeAliasDecl -> CfirTypeAliasSymbol(classId)
            PackageFormat.DeclKind.BuiltInDecl -> {
                val info = wire.info(PackageFormat.BuiltInInfo()) as PackageFormat.BuiltInInfo
                val kind = when (info.builtInType) {
                    PackageFormat.BuiltInType.Array -> CfirBuiltInTypeKind.ARRAY
                    PackageFormat.BuiltInType.VArray -> CfirBuiltInTypeKind.VARRAY
                    PackageFormat.BuiltInType.CPointer -> CfirBuiltInTypeKind.CPOINTER
                    PackageFormat.BuiltInType.CString -> CfirBuiltInTypeKind.CSTRING
                    PackageFormat.BuiltInType.CFunc -> CfirBuiltInTypeKind.CFUNC
                    else -> error("Invalid BuiltInType classifier")
                }
                CfirBuiltInTypeSymbol(classId, kind)
            }
            else -> error("FullId does not identify a class-like declaration")
        }
        reservedClassLikeSymbols[index] = symbol
        pendingClassLikeHeaders += index
        pendingClassLikeSelections.putIfAbsent(index, checkNotNull(currentSourceSelection))
        createDeclDeserializer().reserveClassLikeTypeParameters(index, symbol)
        return symbol
    }

    internal fun materializeClassLikeSymbol(fullId: PackageFormat.FullId): CfirClassLikeSymbol<*> {
        val target = fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration
            ?: error("Invalid classifier FullId")
        return if (target.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
            reserveClassLikeSymbol(target.declaration.zeroBasedIndex)
        } else checkNotNull(materializeExternalDeclaration(target) as? CfirClassLikeDeclaration).symbol
    }

    internal fun completeClassLikeHeader(index: Int, declaration: CfirClassLikeDeclaration) {
        check(reservedClassLikeSymbols[index] === declaration.symbol && declaration.symbol.isBound)
        reservedClassLikeSymbols.remove(index)
        pendingClassLikeHeaders.remove(index)
        pendingClassLikeSelections.remove(index)
    }

    /** 类型身份缓存不等于 bounds/header 完成；所有新增工作在值比较或根发布前排空。 */
    internal fun completeTypeHeaders() {
        check(transactionDepth != 0)
        while (pendingClassLikeHeaders.isNotEmpty() || pendingGenericTypes.isNotEmpty()) {
            while (pendingClassLikeHeaders.isNotEmpty()) {
                val index = pendingClassLikeHeaders.first()
                check(index !in classLikeHeadersInProgress) { "A classifier header is still being constructed" }
                withSourceSelection(pendingClassLikeSelections.getValue(index)) {
                    checkNotNull(createDeclDeserializer().deserializeCanonicalDeclaration(index) as? CfirClassLikeDeclaration)
                }
            }
            while (pendingGenericTypes.isNotEmpty()) {
                val index = pendingGenericTypes.first()
                withSourceSelection(pendingGenericSelections.getValue(index)) { createTypeDeserializer().completeGenericType(index) }
            }
        }
    }

    /** GenericParamDecl.type 可缺失；同表真实 GenericTyInfo 仍是其最终语义产品的索引。 */
    internal val genericTypeIndicesByParameter: Map<Int, List<Int>> by lazy {
        sourceHeaderRegistry.genericTypesByParameter.mapKeys { (parameter, _) -> parameter.toInt() - 1 }
            .mapValues { (_, references) -> references.map { it.toInt() - 1 } }
    }


    /** 导入包索引 -> 包级声明索引。 */
    internal val importedPackageIndices = ConcurrentHashMap<Int, CjoPackageIndex>()


    /** FullId 统一解析器。 */
    internal val fullIdResolver: CjoFullIdResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjoFullIdResolver(this)
    }

    private var sourceSelectionThread: Thread? = null
    private var sourceSelection: CjoSourceContentSelection? = null

    internal val inMaterialization: Boolean get() = transactionDepth != 0
    internal val currentSourceSelection: CjoSourceContentSelection?
        get() = if (sourceSelectionThread === Thread.currentThread()) sourceSelection else null
    internal fun selectedSourceGraph(roots: List<CjoSourceContentRoot>): CfirDeserializedSourceGraph? =
        (if (inMaterialization) bodyState.selectedGraphs else completedGraph.bodies.selectedGraphs)[roots]

    /** 请求不继承外层全集；队列的null scope表示必须从自己的原槽重新取得显式授权。 */
    internal fun <T> withSourceSelection(selection: CjoSourceContentSelection?, action: () -> T): T {
        check(transactionDepth != 0)
        if (selection != null) check(selection.ownedContent.ownerPackage === pkg)
        val previousSelection = sourceSelection
        val previousThread = sourceSelectionThread
        sourceSelection = selection
        sourceSelectionThread = Thread.currentThread()
        return try { action() } finally {
            sourceSelection = previousSelection
            sourceSelectionThread = previousThread
        }
    }

    internal fun requireSourceDeclaration(declarationIndex: Int) {
        val selection = checkNotNull(currentSourceSelection) { "A declaration requires its explicit SOURCE header demand" }
        check((declarationIndex + 1).toUInt() in selection.borrowedContexts.declarations) {
            "SOURCE declaration d ${declarationIndex + 1} is outside its frozen header demand"
        }
    }

    /** 缓存命中也必须检查同包冻结的type槽；planner负责完整结构/type/TP固定点。 */
    internal fun requireSourceTypeIdentity(typeIndex: Int) {
        val selection = checkNotNull(currentSourceSelection) { "A type requires its explicit SOURCE metadata demand" }
        check((typeIndex + 1).toUInt() in selection.types) {
            "SOURCE type ${typeIndex + 1} is outside its frozen typed demand"
        }
    }

    /** 通过所属管理器解析位置自己的源码包，声明的二进制归属仍由当前 context 持有。 */
    internal fun sourceLocation(position: Position?): CjSourceLocation? =
        CjoSourcePositionResolver.resolve(pkg, position, object : org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackageProvider {
            override fun loadPackageSnapshot(fullPkgName: String) = importedSnapshot(fullPkgName)
        })

    /** 原保存槽与诊断锚点同时捕获；无效行列仍保留存在性和全部标量。 */
    internal fun sourcePositions(begin: Position?, end: Position?): CfirSourceImportedPosition {
        check(transactionDepth != 0)
        fun record(position: Position?): CfirSourcePositionIdentity? = position?.let {
            CfirSourcePositionIdentity().also { identity ->
                sourcePositions[identity] = CjoSavedSourcePosition(it.file, it.pkgId, it.line, it.column, it.ignore)
            }
        }
        return CfirSourceImportedPosition(sourceLocation(begin), sourceLocation(end), record(begin), record(end))
    }

    private val sourceExportBinderIdentity = Any()

    /** graph 携带的是原 owner 身份；只有此 context 的成功完成目录可以证明 membership。 */
    internal val sourceGraphPublicationOwner: org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceGraphPublicationOwner
        get() = publishedContentSource as org.cangnova.cangjie.cfir.serialization.cjo.CjoSourceGraphPublicationOwner

    private fun completedGraphSnapshotOf(original: org.cangnova.cangjie.cfir.source.CfirSourceGraph): CjoSourceExportSnapshot = synchronized(materializationLock) {
            check(transactionDepth == 0) { "SOURCE export cannot capture a running materialization transaction" }
            check((original.provenance as? org.cangnova.cangjie.cfir.source.CfirSourceGraphProvenance.Published)?.owner === sourceGraphPublicationOwner) {
                "SOURCE graph belongs to a different publication owner"
            }
            val completed = completedGraph
            val final = checkNotNull(completed.sourceGraphPublications[original]) {
                "SOURCE graph was not successfully published by its actual context"
            }
            exportSnapshot(completed, checkNotNull(loadedPackage), final)
    }

    /** 仅读取所选 e 的保存字段和既有 header 证明；VArray 的 unused factory 不进入 binder。 */
    internal fun savedSourceReference(reference: PackageFormat.FullId?): org.cangnova.cangjie.cfir.source.CfirSourceSavedReference {
        val target = when (reference?.pkgId) {
            null -> org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference.Target.Missing
            org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex.INVALID.value ->
                org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference.Target.Invalid
            org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex.CURRENT.value -> {
                val header = checkNotNull(sourceHeaderRegistry.declarations[reference.index]) {
                    "A saved-only SOURCE reference requires its actual registered declaration header"
                }
                org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference.Target.Current(
                    reference.index.toInt() - 1, header.wire.kind)
            }
            org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex.PACKAGE_REFERENCE.value ->
                org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference.Target.Package(checkNotNull(reference.decl), reference.index)
            else -> {
                require(reference.pkgId >= 0 && reference.pkgId < pkg.importsLength)
                org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference.Target.External(
                    checkNotNull(pkg.imports(reference.pkgId)), checkNotNull(reference.decl))
            }
        }
        return org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedSourceReference(
            sourceExportBinderIdentity, target, reference?.decl, reference?.index ?: 0u)
    }

    /** 只导出同一实际 context 已完成的产品；不交出事务工作容器或活动 reader。 */
    internal fun sourceExportSnapshot(source: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage, includeContent: Boolean): CjoSourceExportSnapshot = synchronized(materializationLock) {
        check(transactionDepth == 0) { "SOURCE export cannot capture a running materialization transaction" }
        check(source === loadedPackage)
        val completed = completedGraph
        val graph = if (includeContent) checkNotNull(completed.source).graph else checkNotNull(completed.headerSource).graph
        exportSnapshot(completed, source, graph)
    }

    private fun exportSnapshot(completed: CompletedGraph, source: org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage,
        graph: org.cangnova.cangjie.cfir.source.CfirSourceGraph): CjoSourceExportSnapshot {
        check(source === loadedPackage && completed.sourceGraphPublications[graph] === graph)
        return CjoSourceExportSnapshot(source, sourceExportBinderIdentity, moduleData.session, graph,
            completed.declarations, completed.types, completed.sourceHeaders, completed.sourcePositions, completed.constantStorage, completed.constantRoots,
            completed, completed.sourceGraphPublications, completed.positionFileBindings)
    }

    /** 原 request 的 source/binder/session 和 canonical d 必须同时属于当前 context。 */
    internal fun definitionRequestViolation(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): String? = synchronized(materializationLock) {
        if (request.source !== loadedPackage || request.binderIdentity !== sourceExportBinderIdentity || request.domain !== moduleData.session)
            return@synchronized "Definition request belongs to a different source/binder/session"
        (request.seeds + request.contents.map { it.declaration }).forEach { declaration ->
            val origin = declaration.cjoDeclarationOrigin
            if (origin == null || !declaration.symbol.isBound || declaration.symbol.cfir !== declaration ||
                declaration.contentSource !== publishedContentSource || completedGraph.declarations[origin.declarationIndex] !== declaration)
                return@synchronized "Definition request requires the original published canonical d binding"
        }
        null
    }

    internal fun matchesDefinitionSource(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): Boolean =
        request.source === loadedPackage && request.binderIdentity === sourceExportBinderIdentity && request.domain === moduleData.session

    internal fun registerDefinitionScope(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest,
        roots: List<CjoSourceContentRoot>, declarations: Set<UInt>, functions: Set<CjoSourceFunctionIdentity>, patterns: Set<Int>) {
        check(transactionDepth != 0 && matchesDefinitionSource(request))
        check(bodyState.selectedGraphs.containsKey(roots)) { "Definition scope requires its actual completed SOURCE selection" }
        val graph = bodyState.selectedGraphs.getValue(roots)
        declarations.forEach { reference ->
            val declaration = this.declarations.getValue(reference.toInt() - 1)
            check(declaration.symbol.isBound && declaration.symbol.cfir === declaration && declaration.contentSource === publishedContentSource)
            check(graph.graph.declarationOwners.containsKey(declaration)) { "Definition scope lacks its actual declaration owner product" }
        }
        functions.forEach { identity ->
            val function = sourceFunctions.getValue(identity)
            check(graph.graph.functionHeaders[function] === sourceHeaders.getValue(function)) { "Definition scope lacks its actual F header product" }
        }
        patterns.forEach { position -> graph.graph.pattern(bodyState.patternIdentities.getValue(position)) }
        graph.selection.typeBindings.filter { it.reference != 0u }.forEach { types.getValue(it.reference.toInt() - 1) }
        val files = completeDefinitionPositionFiles(request, graph.graph, declarations)
        check(definitionScopes.putIfAbsent(request.identity, DefinitionScope(request, roots.toList(), declarations.toList(), functions.toSet(), patterns.toSet(), files)) == null) {
            "A definition request cannot replace its completed scope"
        }
    }

    /** 只查成功 request 目录；别名升级由同一次 body/graph publication 提供最终图。 */
    internal fun completedDefinitionSnapshot(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest): org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult.Completed? = synchronized(materializationLock) {
        check(transactionDepth == 0) { "Definition snapshot cannot observe a running materialization" }
        val completed = completedGraph
        val scope = completed.definitionScopes[request.identity] ?: return@synchronized null
        check(scope.request === request && matchesDefinitionSource(request))
        val graph = completed.bodies.selectedGraphs.getValue(scope.roots)
        val definitions = scope.declarations.associate { reference -> reference.toInt() - 1 to completed.declarations.getValue(reference.toInt() - 1) }
        val borrowed = graph.selection.borrowedContexts.declarations.keys.filterNot { it in scope.declarations }
            .associate { reference -> reference.toInt() - 1 to completed.declarations.getValue(reference.toInt() - 1) }
        val owners = java.util.IdentityHashMap<CfirDeclaration, CfirDeclaration?>()
        definitions.values.forEach { owners[it] = graph.graph.declarationOwners.getValue(it) }
        val selection = org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotSelection(request, scope, definitions, borrowed, owners,
            scope.functions.associate { it.tablePosition to completed.sourceFunctions.getValue(it) },
            scope.patterns.associateWith { completed.bodies.patternIdentities.getValue(it) },
            scope.patterns.filterNot { it in graph.selection.ownedContent.patterns }.toSet(), scope.roots,
            graph.selection.declarationBindings.map { binding ->
                org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionDeclarationBinding(binding.site,
                    org.cangnova.cangjie.cfir.serialization.cjo.CjoExpressionTargetMetadata(binding.target.pkgId, binding.target.decl, binding.target.index))
            }, graph.selection.typeBindings.map { binding ->
                org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionTypeBinding(binding.site, binding.reference,
                    if (binding.reference == 0u) null else completed.types.getValue(binding.reference.toInt() - 1))
            }, scope.positionFiles.associateWith { completed.positionFileBindings.getValue(it) })
        org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotResult.Completed(
            exportSnapshot(completed, request.source, graph.graph), selection)
    }

    /** 只解析所选声明/载荷的原位置，不扫描其它位置或借 FILE 读取源码/声明。 */
    private fun completeDefinitionPositionFiles(request: org.cangnova.cangjie.cfir.serialization.cjo.CjoDefinitionSnapshotRequest,
        graph: org.cangnova.cangjie.cfir.source.CfirSourceGraph, definitions: Set<UInt>): Set<org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference> {
        check(transactionDepth != 0 && matchesDefinitionSource(request))
        val files = linkedSetOf<org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference>()
        fun bind(packageIndex: UInt, fileIndex: UInt) {
            val reference = org.cangnova.cangjie.cfir.serialization.cjo.CjoSavedFileReference(packageIndex, fileIndex)
            files += reference
            if (positionFileBindings.containsKey(reference)) return
            val binding = if (packageIndex == 0u && fileIndex == 0u) {
                org.cangnova.cangjie.cfir.serialization.cjo.CjoPositionFileBinding.NoFile(request.source, sourceGraphPublicationOwner,
                    sourceExportBinderIdentity, moduleData.session, reference)
            } else {
                val target = if (packageIndex == 0u) request.source else {
                    require(packageIndex <= pkg.importsLength.toUInt()) { "A scoped Position requires its actual import slot" }
                    checkNotNull(importedSnapshot(checkNotNull(pkg.imports(packageIndex.toInt() - 1)))) {
                        "A scoped Position requires its actually selected imported source"
                    }
                }
                val ordinal = if (packageIndex == 0u) {
                    require(fileIndex in 1u..target.pkg.allFilesLength.toUInt()) { "Invalid current scoped FILE ordinal" }
                    fileIndex.toInt() - 1
                } else {
                    require(fileIndex < target.pkg.allFilesLength.toUInt()) { "Invalid imported scoped FILE ordinal" }
                    fileIndex.toInt()
                }
                // LoadedPackage 的原 allFiles 表仍是独立只读目录，不借 source/common 混合 CfirFile 向量。
                checkNotNull(target.pkg.allFiles(ordinal))
                org.cangnova.cangjie.cfir.serialization.cjo.CjoPositionFileBinding.Loaded(request.source, sourceGraphPublicationOwner,
                    sourceExportBinderIdentity, moduleData.session, reference,
                    org.cangnova.cangjie.cfir.serialization.cjo.CjoSnapshotTableEntry(target,
                        org.cangnova.cangjie.cfir.serialization.cjo.CjoSnapshotTableDomain.FILE, ordinal))
            }
            positionFileBindings[reference] = binding
        }
        fun position(position: PackageFormat.Position?) { position?.let { bind(it.pkgId, it.file) } }
        fun imported(position: org.cangnova.cangjie.cfir.source.CfirSourceImportedPosition?) {
            listOfNotNull(position?.beginIdentity, position?.endIdentity).forEach { identity ->
                val saved = sourcePositions.getValue(identity)
                bind(saved.pkgId, saved.file)
            }
        }
        fun pattern(pattern: org.cangnova.cangjie.cfir.source.CfirSourcePattern) {
            imported(pattern.importedPosition)
            pattern.patterns.forEach(::pattern)
        }
        definitions.forEach { reference ->
            val declaration = checkNotNull(pkg.allDecls(reference.toInt() - 1))
            position(declaration.begin); position(declaration.end); position(declaration.identifierPos)
            declaration.generic?.let { generic -> (0 until generic.constraintsLength).forEach { index ->
                val constraint = checkNotNull(generic.constraints(index))
                position(constraint.begin); position(constraint.end)
            } }
            if (declaration.infoType == PackageFormat.DeclInfo.EnumInfo)
                position((declaration.info(PackageFormat.EnumInfo()) as PackageFormat.EnumInfo).ellipsisPos)
        }
        (graph.expressions.values + graph.borrowedExpressions.values).forEach { record ->
            imported(record.importedPosition)
            record.payload.patterns().forEach(::pattern)
        }
        graph.declarationPatterns.values.forEach(::pattern)
        return java.util.Collections.unmodifiableSet(files)
    }


    /** 按 FullId 的包与声明下标，通过拥有该包的真实 provider context 物化 canonical 声明。 */
    internal fun deserializeInitializationDependency(
        targetSourcePath: java.nio.file.Path?,
        fullPackageName: String,
        ownerDeclarationIndex: Int,
        declarationIndex: Int,
    ): CfirDeclaration? = initializationDependencyResolver?.invoke(
        targetSourcePath,
        fullPackageName,
        ownerDeclarationIndex,
        declarationIndex,
    )

    /**
     * `CfirDeclDeserializer` / `CfirTypeDeserializer` 都带有调用栈态，
     * 必须按次创建，不能像 Kotlin 的无栈 member deserializer 那样直接复用实例。
     */
    internal fun createTypeDeserializer(): CfirTypeDeserializer = CfirTypeDeserializer(this)

    /** 创建一次性的声明反序列化器，并配套创建同上下文的类型反序列化器。 */
    internal fun createDeclDeserializer(): CfirDeclDeserializer =
        if (currentSourceSelection != null) CfirDeclDeserializer.forSourceGraph(this)
        else CfirDeclDeserializer(this, createTypeDeserializer())

    /** 类型回指先恢复真实 owner 的参数域，当前域可读取尚未发布的已绑定参数壳。 */
    internal fun materializeTypeParameter(fullId: PackageFormat.FullId): CfirTypeParameter {
        val resolved = fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration
            ?: error("Generic type does not reference a declaration: ${fullIdResolver.describe(fullId)}")
        val index = resolved.declaration.zeroBasedIndex
        check(resolved.declaration.decl.kind == PackageFormat.DeclKind.GenericParamDecl)
        val parameter = if (resolved.source == ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) {
            requireSourceDeclaration(index)
            reservedTypeParameters[index] ?: declarations[index] ?: createDeclDeserializer().deserializeCanonicalDeclaration(index)
        } else {
            declarationResolver?.invoke(resolved.packageIndex.sourcePath, resolved.packageIndex.packageFqName.asString(), index)
        }
        return checkNotNull(parameter as? CfirTypeParameter) {
            "Cannot materialize canonical generic parameter: ${fullIdResolver.describe(fullId)}"
        }
    }

    internal fun importedSnapshot(fullPackageName: String): org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage? {
        if (dependencySnapshotResolver != null) return dependencySnapshotResolver.invoke(fullPackageName)
        val candidates = cjoManager.loadPackageSnapshots(fullPackageName)
        check(candidates.size <= 1) { "Ambiguous imported CJO snapshots for '$fullPackageName'" }
        return candidates.singleOrNull()
    }

    internal fun materializeExternalDeclaration(target: ResolvedFullId.Declaration): CfirDeclaration? =
        declarationResolver?.invoke(target.packageIndex.sourcePath, target.packageIndex.packageFqName.asString(),
            target.declaration.zeroBasedIndex)

    internal fun materializeExternalDefaults(target: ResolvedFullId.Declaration, parameters: List<CfirValueParameter>): Map<CfirValueParameter, org.cangnova.cangjie.cfir.expressions.CfirExpression?> =
        checkNotNull(parameterDefaultsResolver) { "External defaults require their actual provider" }.invoke(
            target.packageIndex.sourcePath, target.packageIndex.packageFqName.asString(),
            parameters.map { checkNotNull(it.cjoDeclarationOrigin).declarationIndex })

}
