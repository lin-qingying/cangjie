package org.cangnova.cangjie.cfir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirEnumImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirStructImpl
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind

import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirFileDeclaredTopLevelScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId

import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRefCopy
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.coneTypeSafe
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.util.PrivateForInline

internal class CfirSupertypeResolverProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.SUPER_TYPES,
) {
    override val transformer: CfirSupertypeResolverTransformer =
        CfirSupertypeResolverTransformer(session, scopeSession)

    override fun beforePhase() {
        super.beforePhase()

        val files = (runCatching { session.cfirProvider }.getOrNull() as? CfirProviderImpl)?.getAllFiles().orEmpty()
        if (files.isEmpty()) return

        session.superTypeGraphStoreOrNull?.let { graphStore ->
            graphStore.clearExtended()
            CfirExtendSupertypesCollector(session, scopeSession, graphStore).collect(files)
        }
    }
}

internal class CfirSupertypeResolverTransformer(
    override val session: CfirSession,
    scopeSession: ScopeSession,
) : CfirAbstractPhaseTransformer<Any?>(CfirResolvePhase.SUPER_TYPES) {
    private val supertypeComputationSession = SupertypeComputationSession()
    private val supertypeResolverVisitor = CfirSupertypeResolverVisitor(session, supertypeComputationSession, scopeSession)
    private val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)

    override fun <E : CfirElement> transformElement(element: E, data: Any?): E = element

    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            file.accept(supertypeResolverVisitor, null)
            supertypeComputationSession.breakLoops(session, supertypeResolverVisitor.localClassesNavigationInfo)
            file.transform(applySupertypesTransformer, null)
        }
    }
}

fun <F : CfirClassLikeDeclaration> F.runSupertypeResolvePhaseForLocalClass(
    session: CfirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<CfirScope>,
    localClassesNavigationInfo: LocalClassesNavigationInfo,
    useSiteFile: CfirFile,
    containingDeclarations: List<CfirDeclaration>,
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = CfirSupertypeResolverVisitor(
        session = session,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = scopeSession,
        scopeForLocalClass = currentScopeList.toPersistentList(),
        localClassesNavigationInfo = localClassesNavigationInfo,
        useSiteFile = useSiteFile,
        containingDeclarations = containingDeclarations,
    )
    accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session, localClassesNavigationInfo)
    val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)
    @Suppress("UNCHECKED_CAST")
    return this.transform<CfirClassLikeDeclaration, Any?>(applySupertypesTransformer, null) as F
}

/**
 * low-level API 对非局部声明执行 SUPER_TYPES 阶段的主干入口。
 *
 * 它复用编译器主干的 visitor / loop breaking / apply transformer，
 * 只是去掉 local-class 专用上下文，供 IDE 的 designated lazy resolve 直接调用。
 */
fun <F : CfirClassLikeDeclaration> F.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration(
    session: CfirSession,
    scopeSession: ScopeSession,
    useSiteFile: CfirFile?,
    containingDeclarations: List<CfirDeclaration> = emptyList(),
): F {
    val supertypeComputationSession = SupertypeComputationSession()
    val supertypeResolverVisitor = CfirSupertypeResolverVisitor(
        session = session,
        supertypeComputationSession = supertypeComputationSession,
        scopeSession = scopeSession,
        useSiteFile = useSiteFile,
        containingDeclarations = containingDeclarations,
    )
    accept(supertypeResolverVisitor, null)
    supertypeComputationSession.breakLoops(session, localClassesNavigationInfo = null)
    val applySupertypesTransformer = CfirApplySupertypesTransformer(supertypeComputationSession, session, scopeSession)
    @Suppress("UNCHECKED_CAST")
    return this.transform<CfirClassLikeDeclaration, Any?>(applySupertypesTransformer, null) as F
}

sealed class SupertypeComputationStatus {
    data object NotComputed : SupertypeComputationStatus()
    data object Computing : SupertypeComputationStatus()
    class Computed(val supertypeRefs: List<CfirResolvedTypeRef>) : SupertypeComputationStatus()
}

open class SupertypeComputationSession {
    private val fileScopes: MutableMap<CfirFile, ScopePersistentList> = linkedMapOf()
    private val resolvedExpandedTypeRefs: MutableMap<CfirTypeAlias, CfirResolvedTypeRef> = linkedMapOf()
    private val computationStatus: MutableMap<CfirClassLikeDeclaration, SupertypeComputationStatus> =
        linkedMapOf<CfirClassLikeDeclaration, SupertypeComputationStatus>().withDefault { SupertypeComputationStatus.NotComputed }
    private val declarationIndex: MutableMap<ClassId, CfirClassLikeDeclaration> = linkedMapOf()

    val supertypesSupplier: SupertypeSupplier = SupertypeSupplier { classId ->
        val declaration = declarationIndex[classId]
        supertypeRefs(declaration).mapNotNull(CfirTypeRef::coneTypeOrNull)
    }

    fun rememberDeclaration(classLikeDeclaration: CfirClassLikeDeclaration) {
        declarationIndex[classLikeDeclaration.symbol.classId] = classLikeDeclaration
    }

    fun getOrPutFileScope(file: CfirFile, builder: () -> ScopePersistentList): ScopePersistentList =
        fileScopes.getOrPut(file, builder)

    fun startComputingSupertypes(classLikeDeclaration: CfirClassLikeDeclaration) {
        computationStatus[classLikeDeclaration] = SupertypeComputationStatus.Computing
    }

    fun storeSupertypes(classLikeDeclaration: CfirClassLikeDeclaration, supertypeRefs: List<CfirResolvedTypeRef>) {
        computationStatus[classLikeDeclaration] = SupertypeComputationStatus.Computed(supertypeRefs)
        if (classLikeDeclaration is CfirTypeAlias && supertypeRefs.size == 1) {
            resolvedExpandedTypeRefs[classLikeDeclaration] = supertypeRefs.single()
        }
    }

    fun storeExpandedTypeRef(typeAlias: CfirTypeAlias, expandedTypeRef: CfirResolvedTypeRef) {
        resolvedExpandedTypeRefs[typeAlias] = expandedTypeRef
        computationStatus[typeAlias] = SupertypeComputationStatus.Computed(listOf(expandedTypeRef))
    }

    fun getSupertypesComputationStatus(classLikeDeclaration: CfirClassLikeDeclaration): SupertypeComputationStatus =
        computationStatus.getValue(classLikeDeclaration)

    fun getResolvedSupertypeRefs(classLikeDeclaration: CfirClassLikeDeclaration): List<CfirResolvedTypeRef> {
        val status = computationStatus[classLikeDeclaration]
        if (status is SupertypeComputationStatus.Computed) return status.supertypeRefs
        if (classLikeDeclaration is CfirTypeAlias) {
            resolvedExpandedTypeRefs[classLikeDeclaration]?.let { return listOf(it) }
        }
        return classLikeDeclaration.superTypeRefs.filterIsInstance<CfirResolvedTypeRef>()
    }

    fun getResolvedExpandedTypeRef(typeAlias: CfirTypeAlias): CfirResolvedTypeRef =
        resolvedExpandedTypeRefs[typeAlias]
            ?: getResolvedSupertypeRefs(typeAlias).singleOrNull()
            ?: createErrorTypeRef(typeAlias.source, "Expanded type for ${typeAlias.symbol.classId.asString()} was not computed")

    fun supertypeRefs(classLikeDeclaration: CfirClassLikeDeclaration?): List<CfirTypeRef> {
        if (classLikeDeclaration == null) return emptyList()
        val status = computationStatus[classLikeDeclaration]
        if (status is SupertypeComputationStatus.Computed) return status.supertypeRefs
        if (classLikeDeclaration is CfirTypeAlias) {
            resolvedExpandedTypeRefs[classLikeDeclaration]?.let { return listOf(it) }
        }
        return classLikeDeclaration.superTypeRefs
    }

    /**
     * 对齐 Kotlin `SupertypeComputationSession.expandTypealiasInPlace(...)`：
     * `SUPER_TYPES` 阶段是否真的把 alias 改写成展开类型，必须受 session 里的全局 analysis flag 控制，
     * 不能在这里无条件把 `ConeTypeAliasType` 抹平成真实类型。
     */
    fun expandTypealiasInPlace(typeRef: CfirResolvedTypeRef, session: CfirSession): CfirResolvedTypeRef {
        if (!session.languageVersionSettings.getFlag(AnalysisFlags.expandTypeAliasesInTypeResolution)) {
            return typeRef
        }

        val expandedType = typeRef.coneType.fullyExpandedType(session)
        if (expandedType == typeRef.coneType) return typeRef
        val expandedTypeRef = buildResolvedTypeRefCopy(typeRef) {
            coneType = expandedType
        }
        return if (expandedTypeRef.coneType is ConeErrorType) {
            expandedTypeRef.toErrorTypeRef()
        } else {
            expandedTypeRef
        }
    }

    fun breakLoops(session: CfirSession, localClassesNavigationInfo: LocalClassesNavigationInfo?) {
        val declarations = LinkedHashSet<CfirClassLikeDeclaration>()
        declarations += declarationIndex.values
        declarations += localClassesNavigationInfo?.parentForClass?.keys.orEmpty()

        for (declaration in declarations) {
            when (declaration) {
                is CfirTypeAlias -> {
                    val original = resolvedExpandedTypeRefs[declaration] ?: continue
                    val rewritten = breakLoopsInTypeRef(declaration, original, session, listOf(declaration))
                    if (rewritten !== original) {
                        resolvedExpandedTypeRefs[declaration] = rewritten
                        computationStatus[declaration] = SupertypeComputationStatus.Computed(listOf(rewritten))
                    }
                }

                else -> {
                    val refs = getResolvedSupertypeRefs(declaration)
                    val rewritten = refs.map { breakLoopsInTypeRef(declaration, it, session, listOf(declaration)) }
                    if (rewritten != refs) {
                        computationStatus[declaration] = SupertypeComputationStatus.Computed(rewritten)
                    }
                }
            }
        }
    }

    private fun breakLoopsInTypeRef(
        owner: CfirClassLikeDeclaration,
        typeRef: CfirResolvedTypeRef,
        session: CfirSession,
        currentPath: List<CfirClassLikeDeclaration>,
    ): CfirResolvedTypeRef {
        val target = typeRef.toReferencedDeclaration(session) ?: return typeRef
        val pathSet = currentPath.toSet()
        val hitsCurrentPath = target in pathSet || reachesAny(target, pathSet, session, mutableSetOf())
        if (!hitsCurrentPath) return typeRef

        val message = if (owner is CfirTypeAlias) {
            "Recursive typealias expansion for ${owner.symbol.classId.asString()}"
        } else {
            "Loop in supertype definition for ${owner.symbol.classId.asString()}"
        }
        return createErrorTypeRef(
            sourceElement = typeRef.source,
            message = message,
            kind = if (owner is CfirTypeAlias) DiagnosticKind.Other else DiagnosticKind.LoopInSupertype,
        )
    }

    private fun reachesAny(
        declaration: CfirClassLikeDeclaration,
        targets: Set<CfirClassLikeDeclaration>,
        session: CfirSession,
        visited: MutableSet<CfirClassLikeDeclaration>,
    ): Boolean {
        if (!visited.add(declaration)) return false
        val directDependencies = directDependencies(declaration, session)
        for (dependency in directDependencies) {
            if (dependency in targets) return true
            if (reachesAny(dependency, targets, session, visited)) return true
        }
        return false
    }

    private fun directDependencies(
        declaration: CfirClassLikeDeclaration,
        session: CfirSession,
    ): List<CfirClassLikeDeclaration> {
        val refs = when (declaration) {
            is CfirTypeAlias -> listOf(getResolvedExpandedTypeRef(declaration))
            else -> getResolvedSupertypeRefs(declaration)
        }
        return refs.mapNotNull { it.toReferencedDeclaration(session) }
    }
}

private typealias ScopePersistentList = PersistentList<CfirScope>

internal open class CfirSupertypeResolverVisitor(
    private val session: CfirSession,
    private val supertypeComputationSession: SupertypeComputationSession,
    private val scopeSession: ScopeSession,
    private val scopeForLocalClass: PersistentList<CfirScope>? = null,
    val localClassesNavigationInfo: LocalClassesNavigationInfo? = null,
    @property:PrivateForInline var useSiteFile: CfirFile? = null,
    containingDeclarations: List<CfirDeclaration> = emptyList(),
) : CfirDefaultVisitor<Unit, Any?>() {
    @PrivateForInline
    val classDeclarationsStack: ArrayDeque<CfirClassLikeDeclaration> = ArrayDeque()

    init {
        containingDeclarations.forEach {
            if (it is CfirClassLikeDeclaration) {
                @OptIn(PrivateForInline::class)
                classDeclarationsStack.addLast(it)
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withFile(file: CfirFile, block: () -> R): R {
        val oldFile = useSiteFile
        return try {
            useSiteFile = file
            block()
        } finally {
            useSiteFile = oldFile
        }
    }

    override fun visitElement(element: CfirElement, data: Any?) = Unit

    override fun visitFile(file: CfirFile, data: Any?) {
        withFile(file) {
            file.acceptChildren(this, data)
        }
    }

    override fun visitClass(klass: CfirClass, data: Any?) {
        withClassLike(klass) {
            resolveSpecificClassLikeSupertypes(klass, klass.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(klass, data)
        }
    }

    override fun visitInterface(`interface`: CfirInterface, data: Any?) {
        withClassLike(`interface`) {
            resolveSpecificClassLikeSupertypes(`interface`, `interface`.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(`interface`, data)
        }
    }

    override fun visitStruct(struct: CfirStruct, data: Any?) {
        withClassLike(struct) {
            resolveSpecificClassLikeSupertypes(struct, struct.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(struct, data)
        }
    }

    override fun visitEnum(enum: CfirEnum, data: Any?) {
        withClassLike(enum) {
            resolveSpecificClassLikeSupertypes(enum, enum.superTypeRefs, resolveRecursively = true)
            visitDeclarationContent(enum, data)
        }
    }

    override fun visitTypeAlias(typeAlias: CfirTypeAlias, data: Any?) {
        withClassLike(typeAlias) {
            resolveTypeAliasSupertype(typeAlias)
            visitDeclarationContent(typeAlias, data)
        }
    }

    private fun visitDeclarationContent(declaration: CfirDeclaration, data: Any?) {
        declaration.acceptChildren(this, data)
    }

    inline fun <T> withClassLike(classLikeDeclaration: CfirClassLikeDeclaration, body: () -> T): T {
        @OptIn(PrivateForInline::class)
        classDeclarationsStack.addLast(classLikeDeclaration)
        return try {
            body()
        } finally {
            @OptIn(PrivateForInline::class)
            classDeclarationsStack.removeLast()
        }
    }

    private fun prepareFileScopes(file: CfirFile): ScopePersistentList {
        return supertypeComputationSession.getOrPutFileScope(file) {
            // 这里返回的顺序必须与 TypeResolutionConfiguration 的契约一致：
            // 高优先级 scope 在前，低优先级 scope 在后。
            // SUPER_TYPES 阶段如果把它反转，就会让默认导入先于当前文件声明命中，
            // 从而把本地 `Box/Hashable` 解析成 `std.core.*`。
            createImportingScopes(file, session).toPersistentList()
        }
    }

    /**
     * supertype 解析始终从 use-site 文件导入作用域起步，
     * 再附加当前声明自身的类型参数作用域。
     */
    private fun prepareScopes(classLikeDeclaration: CfirClassLikeDeclaration): ScopePersistentList {
        @OptIn(PrivateForInline::class)
        val fileScopes = useSiteFile?.let(::prepareFileScopes) ?: persistentListOf()
        return fileScopes.pushIfNotNull(classLikeDeclaration.typeParametersScope())
    }

    private fun resolveTypeAliasSupertype(typeAlias: CfirTypeAlias): CfirResolvedTypeRef {
        supertypeComputationSession.rememberDeclaration(typeAlias)
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(typeAlias)) {
            is SupertypeComputationStatus.Computed -> return supertypeComputationSession.getResolvedExpandedTypeRef(typeAlias)
            is SupertypeComputationStatus.Computing -> {
                return createErrorTypeRef(
                    typeAlias.expandedTypeRef.source,
                    "Recursive typealias expansion for ${typeAlias.symbol.classId.asString()}",
                )
            }

            SupertypeComputationStatus.NotComputed -> Unit
        }

        supertypeComputationSession.startComputingSupertypes(typeAlias)
        val resolvedExpandedType = typeAlias.expandedTypeRef.transform<CfirTypeRef, CfirTypeResolutionConfiguration>(
            CfirSpecificTypeResolverTransformer(
                session = session,
                supertypeSupplier = supertypeComputationSession.supertypesSupplier,
                expandTypeAliases = false,
            ),
            createTypeResolutionConfiguration(typeAlias, prepareScopes(typeAlias)),
        )
        val resolvedTypeRef = when (resolvedExpandedType) {
            is CfirResolvedTypeRef -> {
                if (resolvedExpandedType.coneType is ConeErrorType) {
                    resolvedExpandedType.toErrorTypeRef()
                } else {
                    resolvedExpandedType
                }
            }
            else -> createErrorTypeRef(
                typeAlias.expandedTypeRef.source,
                "Unresolved expanded type: ${typeAlias.expandedTypeRef.renderReadable()}",
                DiagnosticKind.UnresolvedSupertype,
            )
        }
        supertypeComputationSession.storeExpandedTypeRef(typeAlias, resolvedTypeRef)
        return resolvedTypeRef
    }

    fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        supertypeRefs: List<CfirTypeRef>,
        resolveRecursively: Boolean,
    ): List<CfirResolvedTypeRef> {
        return resolveSpecificClassLikeSupertypes(classLikeDeclaration) { transformer, configuration ->
            supertypeRefs.mapTo(mutableListOf()) { superTypeRef ->
                val transformed = superTypeRef.transform<CfirTypeRef, CfirTypeResolutionConfiguration>(transformer, configuration)
                val typeParameterType = transformed.coneTypeSafe<ConeTypeParameterType>()
                val referencedTypeAlias = transformed.coneTypeSafe<ConeTypeAliasType>()
                    ?.let { session.symbolProvider.getClassLikeSymbolByClassId(it.classId)?.cfir as? CfirTypeAlias }

                if (resolveRecursively && referencedTypeAlias != null) {
                    visitTypeAlias(referencedTypeAlias, null)
                }

                when {
                    typeParameterType != null -> createErrorTypeRef(
                        superTypeRef.source,
                        "Type parameter cannot be used as a supertype",
                    )

                    transformed !is CfirResolvedTypeRef -> createErrorTypeRef(
                        superTypeRef.source,
                        "Unresolved super-type: ${superTypeRef.renderReadable()}",
                        DiagnosticKind.UnresolvedSupertype,
                    )

                    else -> transformed
                }
            }
        }
    }

    private fun resolveSpecificClassLikeSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        resolveSuperTypeRefs: (CfirSpecificTypeResolverTransformer, CfirTypeResolutionConfiguration) -> List<CfirResolvedTypeRef>,
    ): List<CfirResolvedTypeRef> {
        supertypeComputationSession.rememberDeclaration(classLikeDeclaration)
        when (val status = supertypeComputationSession.getSupertypesComputationStatus(classLikeDeclaration)) {
            is SupertypeComputationStatus.Computed -> return status.supertypeRefs
            is SupertypeComputationStatus.Computing -> {
                return listOf(
                    createErrorTypeRef(
                        classLikeDeclaration.source,
                        "Loop in supertype definition for ${classLikeDeclaration.symbol.classId.asString()}",
                        DiagnosticKind.LoopInSupertype,
                    )
                )
            }

            SupertypeComputationStatus.NotComputed -> Unit
        }

        supertypeComputationSession.startComputingSupertypes(classLikeDeclaration)
        val resolvedTypeRefs = resolveSuperTypeRefs(
            CfirSpecificTypeResolverTransformer(
                session = session,
                supertypeSupplier = supertypeComputationSession.supertypesSupplier,
                expandTypeAliases = false,
            ),
            createTypeResolutionConfiguration(classLikeDeclaration, prepareScopes(classLikeDeclaration)),
        ).map { ref ->
            if (ref.coneType is ConeErrorType) ref.toErrorTypeRef() else ref
        }.let { resolvedRefs ->
            resolvedRefs.withImplicitStdCoreSupertypes(classLikeDeclaration, session)
        }.markDuplicateSupertypes()
        supertypeComputationSession.storeSupertypes(classLikeDeclaration, resolvedTypeRefs)
        return resolvedTypeRefs
    }

    @OptIn(PrivateForInline::class)
    private fun createTypeResolutionConfiguration(
        classLikeDeclaration: CfirClassLikeDeclaration,
        scopes: ScopePersistentList,
    ): CfirTypeResolutionConfiguration {
        return CfirTypeResolutionConfiguration(
            scopes = scopes,
            containingClassDeclarations = classDeclarationsStack.filterIsInstance<CfirClass>(),
            useSiteFile = useSiteFile,
        ).withAdditionalTypeParameters(classLikeDeclaration.typeParametersForResolution())
    }

}

/**
 * 在“非标准库编译”模式下，源码里未显式写出的标准父类型需要在此阶段补齐。
 *
 * 设计约束：
 * 1. 规则只用于 `std.core` 自举，避免把普通源码声明整体改写成显式 `Object/Any` 继承图；
 * 2. 只补“缺失的槽位”，不覆盖用户已经显式声明的继承关系；
 * 3. class 默认补直接父 class：普通 class -> Object，Object -> Any，Any -> 无；
 * 4. interface / struct / enum 在完全没有显式父类型时默认补 Any，但 Any 自身不能补成 `Any : Any` 自环。
 */
private fun List<CfirResolvedTypeRef>.withImplicitStdCoreSupertypes(
    declaration: CfirClassLikeDeclaration,
    session: CfirSession,
): List<CfirResolvedTypeRef> {
    if (session.languageVersionSettings.getFlag(AnalysisFlags.stdlibCompilation)) return this
    if (declaration.symbol.classId.packageFqName != StandardNames.FqNames.core) return this

    val implicitRefs = when (declaration) {
        is CfirClass -> declaration.implicitClassSupertypes(this)
        is CfirInterface, is CfirStruct, is CfirEnum ->
            if (declaration.symbol.classId != StdlibClassIds.Any && isEmpty()) {
                listOf(implicitStdCoreTypeRef(StdlibClassIds.Any))
            } else {
                emptyList()
            }
        else -> emptyList()
    }

    if (implicitRefs.isEmpty()) return this
    return this + implicitRefs
}

private fun CfirClass.implicitClassSupertypes(
    resolvedDirectSuperTypes: List<CfirResolvedTypeRef>,
): List<CfirResolvedTypeRef> {
    val classId = symbol.classId
    if (classId == StdlibClassIds.Any) return emptyList()

    val hasExplicitConcreteSuper = resolvedDirectSuperTypes
        .map(CfirResolvedTypeRef::coneType)
        .any(ConeCangJieType::isConcreteSuperClassifier)

    if (hasExplicitConcreteSuper) return emptyList()

    return when (classId) {
        StdlibClassIds.Object -> listOf(implicitStdCoreTypeRef(StdlibClassIds.Any))
        else -> listOf(implicitStdCoreTypeRef(StdlibClassIds.Object))
    }
}

private fun ConeCangJieType.isConcreteSuperClassifier(): Boolean = when (this) {
    is ConeClassLikeType -> !isInterface
    is ConeStructType, is ConeEnumType -> true
    else -> false
}

private fun implicitStdCoreTypeRef(classId: ClassId): CfirResolvedTypeRef {
    val implicitType = when (classId) {
        // `std.core.Any` 是根接口，补图时必须保留 interface 身份，
        // 否则后续“具体父类”判定会把它误当成 class。
        StdlibClassIds.Any -> ConeClassLikeType(classId.toLookupTag(), isInterface = true)
        else -> ConeClassLikeType(classId.toLookupTag())
    }
    return implicitType.toCfirResolvedTypeRef()
}

private class CfirApplySupertypesTransformer(
    private val supertypeComputationSession: SupertypeComputationSession,
    private val session: CfirSession,
    private val scopeSession: ScopeSession,
) : CfirDefaultTransformer<Any?>() {
    override fun <E : CfirElement> transformElement(element: E, data: Any?): E = element

    private fun transformDeclarationContent(declaration: CfirDeclaration, data: Any?): CfirDeclaration {
        @Suppress("UNCHECKED_CAST")
        return declaration.transformChildren(this, data) as CfirDeclaration
    }

    override fun transformFile(file: CfirFile, data: Any?): CfirFile {
        return withFileAnalysisExceptionWrapping(file) {
            transformDeclarationContent(file, null) as CfirFile
        }
    }

    override fun transformClass(klass: CfirClass, data: Any?): CfirClass {
        applyResolvedSupertypesToClassLike(klass)
        klass.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(klass, null) as CfirClass
    }

    override fun transformInterface(`interface`: CfirInterface, data: Any?): CfirInterface {
        applyResolvedSupertypesToClassLike(`interface`)
        `interface`.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(`interface`, null) as CfirInterface
    }

    override fun transformStruct(struct: CfirStruct, data: Any?): CfirStruct {
        applyResolvedSupertypesToClassLike(struct)
        struct.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(struct, null) as CfirStruct
    }

    override fun transformEnum(enum: CfirEnum, data: Any?): CfirEnum {
        applyResolvedSupertypesToClassLike(enum)
        enum.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(enum, null) as CfirEnum
    }

    override fun transformExtend(extend: CfirExtend, data: Any?): CfirExtend {
        extend.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(extend, null) as CfirExtend
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirTypeAlias {
        if (typeAlias.expandedTypeRef !is CfirResolvedTypeRef) {
            val expanded = supertypeComputationSession.expandTypealiasInPlace(
                supertypeComputationSession.getResolvedExpandedTypeRef(typeAlias),
                session,
            )
            typeAlias.replaceExpandedTypeRef(expanded)
        }
        typeAlias.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(typeAlias, null) as CfirTypeAlias
    }

    private fun applyResolvedSupertypesToClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        val resolvedRefs = supertypeComputationSession.getResolvedSupertypeRefs(classLikeDeclaration)
            .map { ref -> supertypeComputationSession.expandTypealiasInPlace(ref, session) }
        if (classLikeDeclaration.superTypeRefs.any { it !is CfirResolvedTypeRef }) {
            classLikeDeclaration.replaceSuperTypeRefs(resolvedRefs)
        }
        recordResolvedSupertypes(classLikeDeclaration, resolvedRefs)
    }

    private fun recordResolvedSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        resolvedRefs: List<CfirResolvedTypeRef>,
    ) {
        val graphStore = session.superTypeGraphStoreOrNull ?: return
        graphStore.recordDeclared(classLikeDeclaration, resolvedRefs.map { ref ->
            CfirSuperTypeGraphEdge(
                typeRef = ref,
                renderedType = ref.renderReadable(),
                resolvedClassSymbol = ref.toReferencedDeclaration(session)?.symbol
                    as? org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>,
            )
        })
    }
}

private class CfirExtendSupertypesCollector(
    private val session: CfirSession,
    private val scopeSession: ScopeSession,
    private val graphStore: org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore,
) : CfirDefaultVisitor<Unit, CfirTypeResolutionConfiguration>() {
    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    private val classDeclarationsStack: ArrayDeque<CfirClassLikeDeclaration> = ArrayDeque()
    private var useSiteFile: CfirFile? = null

    fun collect(files: List<CfirFile>) {
        files.forEach { file ->
            val configuration = CfirTypeResolutionConfiguration.EMPTY
                .withUseSiteFile(file)
                // extend 超类型采集与正式 SUPER_TYPES 解析必须共享同一套作用域优先级，
                // 否则索引阶段会先把本地类型错误解析成默认导入类型，后续所有 extend 规则都会被污染。
                .withScopes(createImportingScopes(file, session).toPersistentList())
            withFile(file) {
                file.declarations.forEach { declaration ->
                    declaration.accept(this, configuration)
                }
            }
        }
    }

    override fun visitElement(element: CfirElement, data: CfirTypeResolutionConfiguration) = Unit

    override fun visitClass(klass: CfirClass, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(klass, data)
    }

    override fun visitInterface(`interface`: CfirInterface, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(`interface`, data)
    }

    override fun visitStruct(struct: CfirStruct, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(struct, data)
    }

    override fun visitEnum(enum: CfirEnum, data: CfirTypeResolutionConfiguration) {
        visitNestedContainer(enum, data)
    }

    override fun visitExtend(extend: CfirExtend, data: CfirTypeResolutionConfiguration) {
        val configuration = data
            .withUseSiteFile(useSiteFile ?: data.useSiteFile ?: return)
            .withTopContainer(extend)
            .withContainingClassDeclarations(classDeclarationsStack.filterIsInstance<CfirClass>())
            .withAdditionalTypeParameters(extend.typeParameters)

        extend.transformExtendedTypeRef(typeResolverTransformer, configuration)
        extend.transformSuperTypeRefs(typeResolverTransformer, configuration)

        val ownerClassId = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.classIdOrPrimitiveClassId ?: return
        val edges = extend.superTypeRefs.mapNotNull { ref ->
            val resolvedRef = ref as? CfirResolvedTypeRef ?: return@mapNotNull null
            CfirSuperTypeGraphEdge(
                typeRef = resolvedRef,
                renderedType = resolvedRef.renderReadable(),
                resolvedClassSymbol = resolvedRef.toReferencedDeclaration(session)?.symbol
                    as? org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol<*>,
            )
        }
        graphStore.recordExtended(ownerClassId, edges)
    }

    private fun visitNestedContainer(
        declaration: CfirClassLikeDeclaration,
        data: CfirTypeResolutionConfiguration,
    ) {
        val configuration = data.withAdditionalTypeParameters(declaration.typeParametersForResolution())
        classDeclarationsStack.addLast(declaration)
        try {
            declaration.declarations.forEach { child ->
                child.accept(this, configuration)
            }
        } finally {
            classDeclarationsStack.removeLast()
        }
    }

    private inline fun withFile(file: CfirFile, block: () -> Unit) {
        val previous = useSiteFile
        useSiteFile = file
        try {
            block()
        } finally {
            useSiteFile = previous
        }
    }
}

private fun createImportingScopes(file: CfirFile, session: CfirSession): List<CfirScope> {
    val symbolProvider: CfirSymbolProvider = session.symbolProvider
    val imports = file.imports
    val defaultImports = session.defaultImportsProvider
        .getDefaultImports(includeLowPriorityImports = true)
        .filter { it.fqName !in session.defaultImportsProvider.excludedImports }
        .map(ImportPath::toImport)

    return buildList {
        // CfirTypeResolver 按顺序查找 scope；supertype 解析同样必须高优先级在前。
        add(CfirFileDeclaredTopLevelScope(file))
        add(CfirPackageMemberScope(file.packageDirective.packageFqName, session))
        add(CfirExplicitSimpleImportingScope(imports, symbolProvider))
        add(CfirExplicitStarImportingScope(imports, symbolProvider))
        add(CfirExplicitSimpleImportingScope(defaultImports, symbolProvider))
        add(CfirExplicitStarImportingScope(defaultImports, symbolProvider))
    }
}

private fun ImportPath.toImport() = buildImport {
    source = null
    importedFqName = fqName
    isAllUnder = this@toImport.isAllUnder
    aliasName = alias
    aliasSource = null
}

private fun CfirClassLikeDeclaration.typeParametersScope(): CfirScope? {
    val typeParameters = typeParametersForResolution()
    if (typeParameters.isEmpty()) return null
    return CfirTypeParameterScopeImpl(typeParameters)
}

private fun CfirClassLikeDeclaration.typeParametersForResolution(): List<CfirTypeParameter> = when (this) {
    is CfirClass -> typeParameters
    is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    is CfirTypeAlias -> typeParameters
}

private fun CfirClassLikeDeclaration.replaceSuperTypeRefs(newRefs: List<CfirTypeRef>) {
    when (this) {
        is CfirClassImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirInterfaceImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirStructImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        is CfirEnumImpl -> superTypeRefs.apply {
            clear()
            addAll(newRefs)
        }
        else -> Unit
    }
}

private fun CfirResolvedTypeRef.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? =
    coneType.toReferencedDeclaration(session)

private fun ConeCangJieType.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? {
    val classId = when (this) {
        is org.cangnova.cangjie.cfir.types.ConePrimitiveType -> kind.classId
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    } ?: return null
    return session.cfirProvider.getCfirClassifierByFqName(classId)
        ?: session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

private fun createErrorTypeRef(
    sourceElement: CjSourceElement?,
    message: String,
    kind: DiagnosticKind = DiagnosticKind.Other,
): CfirResolvedTypeRef = buildErrorTypeRef {
    source = sourceElement

    diagnostic = ConeSimpleDiagnostic(message, kind)
}

private fun CfirResolvedTypeRef.toErrorTypeRef(): CfirResolvedTypeRef {
    val errorType = coneType as? ConeErrorType ?: return this
    return buildErrorTypeRef {
        source = this@toErrorTypeRef.source
        coneType = errorType
        annotations += this@toErrorTypeRef.annotations
        delegatedTypeRef = this@toErrorTypeRef.delegatedTypeRef ?: this@toErrorTypeRef
        diagnostic = errorType.diagnostic
    }
}

private fun List<CfirResolvedTypeRef>.markDuplicateSupertypes(): List<CfirResolvedTypeRef> {
    val firstIndexByKey = linkedMapOf<String, Int>()
    val duplicates = mutableSetOf<Int>()

    forEachIndexed { index, ref ->
        val key = ref.duplicateKey() ?: return@forEachIndexed
        val previous = firstIndexByKey.putIfAbsent(key, index)
        if (previous != null) {
            duplicates += previous
        }
    }

    if (duplicates.isEmpty()) return this

    return mapIndexed { index, ref ->
        if (index !in duplicates || ref is CfirErrorTypeRef) return@mapIndexed ref
        buildErrorTypeRef {
            source = ref.source
            annotations += ref.annotations
            coneType = ref.coneType
            delegatedTypeRef = ref.delegatedTypeRef ?: ref
            diagnostic = ConeSimpleDiagnostic("Duplicate supertype: ${ref.renderReadable()}", DiagnosticKind.DuplicateSupertype)
        }
    }
}

private fun CfirResolvedTypeRef.duplicateKey(): String? {
    return when (val type = coneType) {
        is ConePrimitiveType -> "primitive:${type.kind.typeName}"
        is ConeClassLikeType -> "class:${type.classId}"
        is ConeStructType -> "struct:${type.classId}"
        is ConeEnumType -> "enum:${type.classId}"
        else -> null
    }
}

private fun CfirTypeRef.renderReadable(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.toString()
    else -> toString()
}

private fun <E> PersistentList<E>.push(element: E): PersistentList<E> = add(0, element)
private fun <E> PersistentList<E>.pushAll(collection: Collection<E>): PersistentList<E> = addAll(0, collection)
private fun ScopePersistentList.pushIfNotNull(scope: CfirScope?): ScopePersistentList = if (scope == null) this else push(scope)
