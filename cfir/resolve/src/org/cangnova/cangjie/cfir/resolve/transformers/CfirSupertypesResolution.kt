package org.cangnova.cangjie.cfir.resolve.transformers

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
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
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.LocalClassesNavigationInfo
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitSimpleImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExplicitStarImportingScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType

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

sealed class SupertypeComputationStatus {
    data object NotComputed : SupertypeComputationStatus()
    data object Computing : SupertypeComputationStatus()
    class Computed(val supertypeRefs: List<CfirResolvedTypeRef>) : SupertypeComputationStatus()
}

open class SupertypeComputationSession {
    private val fileScopes: MutableMap<CfirFile, ScopePersistentList> = linkedMapOf()
    private val nestedClassScopes: MutableMap<CfirClassLikeDeclaration, ScopePersistentList> = linkedMapOf()
    private val staticNestedClassScopes: MutableMap<CfirClassLikeDeclaration, ScopePersistentList> = linkedMapOf()
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

    fun getOrPutScopeForNestedClasses(
        declaration: CfirClassLikeDeclaration,
        builder: () -> ScopePersistentList,
    ): ScopePersistentList = nestedClassScopes.getOrPut(declaration, builder)

    fun getOrPutScopeForStaticNestedClasses(
        declaration: CfirClassLikeDeclaration,
        builder: () -> ScopePersistentList,
    ): ScopePersistentList = staticNestedClassScopes.getOrPut(declaration, builder)

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

    fun expandTypealiasInPlace(typeRef: CfirResolvedTypeRef): CfirResolvedTypeRef {
        val expandedType = fullyExpandTypeAlias(typeRef.coneType) ?: return typeRef
        if (expandedType == typeRef.coneType) return typeRef
        return buildResolvedTypeRefCopy(typeRef) {
            coneType = expandedType
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
        return createErrorTypeRef(typeRef.source, message)
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
            recordSupertypesIfNeeded(klass)
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
            createImportingScopes(file, session).asReversed().toPersistentList()
        }
    }

    private fun prepareScopeForNestedClasses(
        classLikeDeclaration: CfirClassLikeDeclaration,
        forStaticNestedClass: Boolean,
    ): ScopePersistentList {
        return if (forStaticNestedClass) {
            supertypeComputationSession.getOrPutScopeForStaticNestedClasses(classLikeDeclaration) {
                calculateScopes(classLikeDeclaration, true)
            }
        } else {
            supertypeComputationSession.getOrPutScopeForNestedClasses(classLikeDeclaration) {
                calculateScopes(classLikeDeclaration, false)
            }
        }
    }

    private fun calculateScopes(
        outerClass: CfirClassLikeDeclaration,
        forStaticNestedClass: Boolean,
    ): ScopePersistentList {
        resolveAllSupertypesForOuterClass(outerClass)
        return prepareScopes(outerClass, forStaticNestedClass).pushAll(createOtherScopesForNestedClasses(outerClass))
    }

    protected open fun resolveAllSupertypesForOuterClass(outerClass: CfirClassLikeDeclaration) {
        when (outerClass) {
            is CfirTypeAlias -> resolveTypeAliasSupertype(outerClass)
            else -> resolveAllSupertypes(outerClass, outerClass.superTypeRefs)
        }
    }

    private fun resolveAllSupertypes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        supertypeRefs: List<CfirTypeRef>,
        visited: MutableSet<CfirClassLikeDeclaration> = mutableSetOf(),
    ) {
        if (!visited.add(classLikeDeclaration)) return
        val supertypes = resolveSpecificClassLikeSupertypes(classLikeDeclaration, supertypeRefs, true).map(CfirResolvedTypeRef::coneType)
        for (supertype in supertypes) {
            val referencedDeclaration = supertype.toReferencedDeclaration(session) ?: continue
            resolveAllSupertypes(referencedDeclaration, supertypeComputationSession.supertypeRefs(referencedDeclaration), visited)
        }
    }

    private fun createOtherScopesForNestedClasses(klass: CfirClassLikeDeclaration): Collection<CfirScope> {
        val scopes = mutableListOf<CfirScope>()
        scopes += buildStaticScope(klass, scopeSession)
        for (supertypeRef in supertypeComputationSession.supertypeRefs(klass)) {
            val referencedDeclaration = supertypeRef.coneTypeOrNull?.toReferencedDeclaration(session) ?: continue
            scopes += buildStaticScope(referencedDeclaration, scopeSession)
        }
        return scopes
    }

    private fun prepareScopes(
        classLikeDeclaration: CfirClassLikeDeclaration,
        forStaticNestedClass: Boolean,
    ): ScopePersistentList {
        val result = when {
            classLikeDeclaration.isLocalClassLike() -> {
                val localNavigation = localClassesNavigationInfo ?: return persistentListOf()
                val parent = localNavigation.parentForClass[classLikeDeclaration]
                when (parent) {
                    null -> scopeForLocalClass ?: persistentListOf()
                    else -> prepareScopeForNestedClasses(parent, forStaticNestedClass)
                }
            }

            classLikeDeclaration.symbol.classId.isNestedClass -> {
                val outerClassId = classLikeDeclaration.symbol.classId.outerClassId ?: return persistentListOf()
                val outerClass = session.cfirProvider.getClassByClassId(outerClassId)
                    ?: session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)?.cfir
                    ?: return persistentListOf()
                prepareScopeForNestedClasses(outerClass, classLikeDeclaration.isStaticallyNested() || forStaticNestedClass)
            }

            else -> {
                @OptIn(PrivateForInline::class)
                useSiteFile?.let(::prepareFileScopes) ?: persistentListOf()
            }
        }

        return if (forStaticNestedClass) result else result.pushIfNotNull(classLikeDeclaration.typeParametersScope())
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
            createTypeResolutionConfiguration(typeAlias, prepareScopes(typeAlias, false)),
        )
        val resolvedTypeRef = when (resolvedExpandedType) {
            is CfirResolvedTypeRef -> resolvedExpandedType
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
            createTypeResolutionConfiguration(classLikeDeclaration, prepareScopes(classLikeDeclaration, false)),
        )
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

    private fun recordSupertypesIfNeeded(klass: CfirClass) {
        val graphStore = session.superTypeGraphStoreOrNull ?: return
        val edges = supertypeComputationSession.getResolvedSupertypeRefs(klass).map { ref ->
            CfirSuperTypeGraphEdge(
                renderedType = ref.renderReadable(),
                resolvedClassSymbol = ref.toReferencedDeclaration(session)?.symbol as? org.cangnova.cangjie.cfir.symbols.CfirClassSymbol,
            )
        }
        graphStore.record(klass, edges)
    }
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

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: Any?): CfirTypeAlias {
        if (typeAlias.expandedTypeRef !is CfirResolvedTypeRef) {
            val expanded = supertypeComputationSession.expandTypealiasInPlace(supertypeComputationSession.getResolvedExpandedTypeRef(typeAlias))
            typeAlias.replaceExpandedTypeRef(expanded)
        }
        typeAlias.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return transformDeclarationContent(typeAlias, null) as CfirTypeAlias
    }

    private fun applyResolvedSupertypesToClassLike(classLikeDeclaration: CfirClassLikeDeclaration) {
        if (classLikeDeclaration.superTypeRefs.all { it is CfirResolvedTypeRef }) return
        val resolvedRefs = supertypeComputationSession.getResolvedSupertypeRefs(classLikeDeclaration)
            .map(supertypeComputationSession::expandTypealiasInPlace)
        classLikeDeclaration.replaceSuperTypeRefs(resolvedRefs)
    }
}

private fun buildStaticScope(
    declaration: CfirClassLikeDeclaration,
    scopeSession: ScopeSession,
): CfirContainingNamesAwareScope {
    val key = "supertype-static:" + declaration.symbol.classId.asString()
    return scopeSession.getOrBuild(key, StaticScopeKey) {
        CfirClassStaticScope(declaration)
    }
}

private object StaticScopeKey : org.cangnova.cangjie.cfir.ScopeSessionKey<String, CfirContainingNamesAwareScope>()

private fun createImportingScopes(file: CfirFile, session: CfirSession): List<CfirScope> {
    val symbolProvider: CfirSymbolProvider = session.symbolProvider
    val imports = file.imports
    val defaultImports = session.defaultImportsProvider
        .getDefaultImports(includeLowPriorityImports = true)
        .filter { it.fqName !in session.defaultImportsProvider.excludedImports }
        .map(ImportPath::toImport)

    return buildList {
        add(CfirPackageMemberScope(file.packageDirective.packageFqName, symbolProvider))
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
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    is CfirTypeAlias -> typeParameters
}

private fun CfirClassLikeDeclaration.isLocalClassLike(): Boolean = symbol.classId.isLocal

private fun CfirClassLikeDeclaration.isStaticallyNested(): Boolean = when (this) {
    is CfirClass -> status.isStatic
    is CfirTypeAlias -> true
    is CfirInterface, is CfirStruct, is CfirEnum -> true
}

private fun CfirClassLikeDeclaration.replaceSuperTypeRefs(newRefs: List<CfirTypeRef>) {
    when (this) {
        is CfirClassImpl -> superTypeRefs = newRefs
        is CfirInterfaceImpl -> superTypeRefs = newRefs
        is CfirStructImpl -> superTypeRefs = newRefs
        is CfirEnumImpl -> superTypeRefs = newRefs
        else -> Unit
    }
}

private fun CfirResolvedTypeRef.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? =
    coneType.toReferencedDeclaration(session)

private fun ConeCangJieType.toReferencedDeclaration(session: CfirSession): CfirClassLikeDeclaration? {
    val classId = when (this) {
        is ConeClassLikeType -> classId
        is ConeStructType -> classId
        is ConeEnumType -> classId
        is ConeTypeAliasType -> classId
        else -> null
    } ?: return null
    return session.cfirProvider.getClassByClassId(classId)
        ?: session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

private fun fullyExpandTypeAlias(type: ConeCangJieType): ConeCangJieType? {
    var current: ConeCangJieType = type
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
    }
    return current
}

private fun createErrorTypeRef(
    sourceElement: CjSourceElement?,
    message: String,
    kind: DiagnosticKind = DiagnosticKind.Other,
): CfirResolvedTypeRef = buildErrorTypeRef {
    source = sourceElement

    diagnostic = ConeSimpleDiagnostic(message, kind)
}

private fun CfirTypeRef.renderReadable(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.toString()
    else -> toString()
}

private fun <E> PersistentList<E>.push(element: E): PersistentList<E> = add(0, element)
private fun <E> PersistentList<E>.pushAll(collection: Collection<E>): PersistentList<E> = addAll(0, collection)
private fun ScopePersistentList.pushIfNotNull(scope: CfirScope?): ScopePersistentList = if (scope == null) this else push(scope)
