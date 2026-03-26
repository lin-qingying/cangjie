package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.calls.InaccessibleImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.body.collectTowerDataElementsForClass
import org.cangnova.cangjie.cfir.resolve.body.typeParametersForTower
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.util.PrivateForInline
import java.util.IdentityHashMap
import java.util.EnumMap

class BodyResolveContext(
    @set:PrivateForInline
    var returnTypeCalculator: ReturnTypeCalculator,
    val dataFlowAnalyzerContext: CfirDataFlowAnalyzerContext,
    private val isContextCollectorMode: Boolean = false,
) {
    val fileImportsScope: MutableList<CfirScope> = mutableListOf()

    @set:PrivateForInline
    lateinit var file: CfirFile

    @PrivateForInline
    var regularTowerDataContexts: CfirRegularTowerDataContexts =
        CfirRegularTowerDataContexts(regular = CfirTowerDataContext())

    @PrivateForInline
    val specialTowerDataContexts: CfirSpecialTowerDataContexts = CfirSpecialTowerDataContexts()

    @OptIn(PrivateForInline::class)
    val towerDataContext: CfirTowerDataContext
        get() = regularTowerDataContexts.currentContext
            ?: throw AssertionError("No regular data context found, towerDataMode = $towerDataMode")

    val implicitValueStorage: ImplicitValueStorage
        get() = towerDataContext.implicitValueStorage

    @OptIn(PrivateForInline::class)
    var towerDataMode: CfirTowerDataMode
        get() = regularTowerDataContexts.activeMode
        set(value) {
            regularTowerDataContexts = regularTowerDataContexts.replaceTowerDataMode(value)
        }

    @set:PrivateForInline
    var containers: ArrayDeque<CfirDeclaration> = ArrayDeque()

    val containerIfAny: CfirDeclaration?
        get() = containers.lastOrNull()

    @set:PrivateForInline
    var containingRegularClass: CfirClass? = null

    @set:PrivateForInline
    var inferenceSession: CfirInferenceSession = CfirInferenceSession.DEFAULT

    @set:PrivateForInline
    var isInsideAssignmentRhs: Boolean = false

    @set:PrivateForInline
    var publicApiInlineFunction: CfirFunction? = null

    @set:PrivateForInline
    var containingClassDeclarations: ArrayDeque<CfirClass> = ArrayDeque()

    @set:PrivateForInline
    var targetedLocalClasses: Set<CfirDeclaration> = emptySet()

    private var currentLocalScope: CfirLocalScopeImpl? = null

    @OptIn(PrivateForInline::class)
    inline fun <T> withFile(file: CfirFile, f: () -> T): T {
        val oldFile = if (::file.isInitialized) this.file else null
        val oldImports = fileImportsScope.toList()
        this.file = file
        fileImportsScope.clear()
        return try {
            withContainer(file, f)
        } finally {
            fileImportsScope.clear()
            fileImportsScope.addAll(oldImports)
            if (oldFile != null) {
                this.file = oldFile
            }
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withContainer(declaration: CfirDeclaration, f: () -> T): T {
        val oldContainingRegularClass = containingRegularClass
        containers.addLast(declaration)
        if (declaration is CfirClass) {
            containingRegularClass = declaration
        }
        return try {
            f()
        } finally {
            containers.removeLast()
            containingRegularClass = oldContainingRegularClass
        }
    }

    inline fun <T> withContainingClass(declaration: CfirClass, f: () -> T): T {
        containingClassDeclarations.addLast(declaration)
        return try {
            f()
        } finally {
            containingClassDeclarations.removeLast()
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withTowerDataContexts(newContexts: CfirRegularTowerDataContexts, f: () -> T): T {
        val old = regularTowerDataContexts
        regularTowerDataContexts = newContexts
        return try {
            f()
        } finally {
            regularTowerDataContexts = old
        }
    }
    inline fun <T> withTowerDataContext(newContext: CfirTowerDataContext, f: () -> T): T {
        val initialContext = towerDataContext
        return try {
            replaceTowerDataContext(newContext)
            f()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    inline fun <R> withTowerDataCleanup(l: () -> R): R {
        val initialContext = towerDataContext
        return try {
            l()
        } finally {
            replaceTowerDataContext(initialContext)
        }
    }

    inline fun <T> withTowerDataMode(mode: CfirTowerDataMode?, f: () -> T): T {
        val oldMode = towerDataMode
        if (mode != null) {
            towerDataMode = mode
        }
        return try {
            f()
        } finally {
            towerDataMode = oldMode
        }
    }

    fun <T> withScopesForClass(
        owner: CfirClassLikeDeclaration,
        holder: SessionAndScopeSessionHolder,
        f: () -> T,
    ): T {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*>
            ?: return f()
        val ownerType = ownerSymbol.constructType()
        val towerElementsForClass = holder.collectTowerDataElementsForClass(owner, ownerType)

        val base = towerDataContext.addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
        val statics = base.addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
        val typeParameterScope = owner.typeParametersForTower()
            .takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeParameterScopeImpl)

        val withTypeParameters = if (typeParameterScope != null) {
            towerDataContext
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScope(typeParameterScope)
        } else {
            statics
        }

        val forMembersResolution = withTypeParameters
            .addReceiver(null, towerElementsForClass.thisReceiver)

        val forNestedClasses = statics

        val constructor = (owner as? CfirClass)?.declarations?.firstOrNull { it is CfirConstructor } as? CfirConstructor
        val (primaryConstructorPureParametersScope, primaryConstructorAllParametersScope) =
            if (constructor?.isPrimary == true) {
                val scope = buildConstructorParametersScope(constructor)
                scope to scope
            } else {
                null to null
            }

        val forConstructorHeaders = if (!isContextCollectorMode) {
            val inaccessibleThisInHeader = InaccessibleImplicitReceiverValue(
                ownerSymbol,
                ownerType,
                InaccessibleReceiverKind.CLASS_HEADER,
                holder.session,
                holder.scopeSession,
            )

            towerDataContext
                .addReceiver(null, inaccessibleThisInHeader)
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScopeIfNotNull(typeParameterScope)
        } else {
            withTypeParameters
        }

        val forEnumConstructors = if (owner is CfirEnum && !isContextCollectorMode) {
            val inaccessibleReceiver = InaccessibleImplicitReceiverValue(
                ownerSymbol,
                ownerType,
                InaccessibleReceiverKind.ENUM_CONSTRUCTOR,
                holder.session,
                holder.scopeSession,
            )

            towerDataContext
                .addReceiver(null, inaccessibleReceiver)
                .addNonLocalTowerDataElements(towerElementsForClass.superClassesStaticScopes)
                .addNonLocalScopeIfNotNull(towerElementsForClass.staticScope)
                .addNonLocalScopeIfNotNull(typeParameterScope)
        } else {
            withTypeParameters
        }

        val newContexts = CfirRegularTowerDataContexts(
            regular = forMembersResolution,
            forNestedClasses = forNestedClasses,
            forStaticMembers = statics,
            forConstructorHeaders = forConstructorHeaders,
            forEnumConstructors = forEnumConstructors,
            primaryConstructorPureParametersScope = primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope = primaryConstructorAllParametersScope,
        )

        return withTowerDataContexts(newContexts, f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withInferenceSession(inferenceSession: CfirInferenceSession, block: CfirInferenceSession.() -> T): T {
        val oldSession = this.inferenceSession
        this.inferenceSession = inferenceSession
        return try {
            inferenceSession.block()
        } finally {
            this.inferenceSession = oldSession
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <R> withAssignmentRhs(block: () -> R): R {
        val old = isInsideAssignmentRhs
        isInsideAssignmentRhs = true
        return try {
            block()
        } finally {
            isInsideAssignmentRhs = old
        }
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withPublicApiInlineFunction(function: CfirFunction?, block: () -> T): T {
        val old = publicApiInlineFunction
        publicApiInlineFunction = function
        return try {
            block()
        } finally {
            publicApiInlineFunction = old
        }
    }

    @OptIn(PrivateForInline::class)
    fun replaceTowerDataContext(newContext: CfirTowerDataContext) {
        regularTowerDataContexts = regularTowerDataContexts.replaceCurrentlyActiveContext(newContext)
        currentLocalScope = newContext.localScopes.lastOrNull() as? CfirLocalScopeImpl
    }

    @OptIn(PrivateForInline::class)
    fun clear() {
        specialTowerDataContexts.clear()
        fileImportsScope.clear()
        dataFlowAnalyzerContext.reset()
    }

    fun addNonLocalScope(scope: CfirScope) {
        addNonLocalTowerDataElement(scope.asTowerDataElement(isLocal = false))
    }

    fun addNonLocalScopes(scopes: List<CfirScope>) {
        if (scopes.isEmpty()) return
        addNonLocalTowerDataElements(scopes.map { it.asTowerDataElement(isLocal = false) })
    }

    fun addNonLocalTowerDataElement(element: CfirTowerDataElement) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(listOf(element)))
    }

    fun addNonLocalTowerDataElements(newElements: List<CfirTowerDataElement>) {
        replaceTowerDataContext(towerDataContext.addNonLocalTowerDataElements(newElements))
    }

    fun addLocalScope(localScope: CfirLocalScopeImpl) {
        replaceTowerDataContext(towerDataContext.addLocalScope(localScope))
        currentLocalScope = localScope
    }

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>) {
        replaceTowerDataContext(towerDataContext.addReceiver(name, implicitReceiverValue))
    }

    fun storeVariable(name: Name, symbol: CfirCallableSymbol<*>) {
        replaceTowerDataContext(towerDataContext.addLocalVariable(name, symbol))
        currentLocalScope = towerDataContext.localScopes.lastOrNull() as? CfirLocalScopeImpl
    }

    fun storeFunction(name: Name, symbol: CfirFunctionSymbol<*>) {
        val lastScope = (towerDataContext.localScopes.lastOrNull() as? CfirLocalScopeImpl) ?: return
        val newLastScope = lastScope.withFunction(name, symbol)
        replaceTowerDataContext(towerDataContext.setLastLocalScope(newLastScope))
        currentLocalScope = newLastScope
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withAnonymousFunctionTowerDataContext(symbol: CfirFunctionSymbol<*>, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getAnonymousFunctionContext(symbol), f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withCallableReferenceTowerDataContext(access: CfirExpression, f: () -> T): T {
        return withTemporaryRegularContext(specialTowerDataContexts.getCallableReferenceContext(access), f)
    }

    @OptIn(PrivateForInline::class)
    inline fun <T> withTemporaryRegularContext(newContext: CfirPostponedAtomsResolutionContext?, f: () -> T): T {
        val context = newContext ?: return f()
        return withTowerDataContexts(regularTowerDataContexts.replaceAndSetActiveRegularContext(context.towerDataContext)) {
            if (context.inferenceSession !== inferenceSession) {
                withInferenceSession(context.inferenceSession) { f() }
            } else {
                f()
            }
        }
    }

    @OptIn(PrivateForInline::class)
    fun storeContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.storeAnonymousFunctionContext(symbol, towerDataContext, inferenceSession)
    }

    @OptIn(PrivateForInline::class)
    fun dropContextForAnonymousFunction(anonymousFunction: CfirFunction) {
        val symbol = anonymousFunction.symbol as? CfirFunctionSymbol<*> ?: return
        specialTowerDataContexts.dropAnonymousFunctionContext(symbol)
    }

    @OptIn(PrivateForInline::class)
    fun storeCallableReferenceContext(callableReferenceAccess: CfirExpression) {
        specialTowerDataContexts.storeCallableReferenceContext(callableReferenceAccess, towerDataContext, inferenceSession)
    }

    @OptIn(PrivateForInline::class)
    fun dropCallableReferenceContext(callableReferenceAccess: CfirExpression) {
        specialTowerDataContexts.dropCallableReferenceContext(callableReferenceAccess)
    }

    private fun buildConstructorParametersScope(constructor: CfirConstructor): CfirLocalScopeImpl {
        var scope = CfirLocalScopeImpl()
        for (parameter in constructor.valueParameters) {
            val symbol = parameter.symbol as? CfirCallableSymbol<*> ?: continue
            scope = scope.withVariable(parameter.name, symbol)
        }
        return scope
    }
}

enum class CfirTowerDataMode {
    REGULAR,
    NESTED_CLASS,
    STATIC_MEMBER,
    CONSTRUCTOR_HEADER,
    ENUM_CONSTRUCTOR,
}

class CfirRegularTowerDataContexts private constructor(
    private val modeMap: EnumMap<CfirTowerDataMode, CfirTowerDataContext>,
    val primaryConstructorPureParametersScope: CfirLocalScopeImpl? = null,
    val primaryConstructorAllParametersScope: CfirLocalScopeImpl? = null,
    val activeMode: CfirTowerDataMode = CfirTowerDataMode.REGULAR,
) {
    constructor(
        regular: CfirTowerDataContext,
        forNestedClasses: CfirTowerDataContext? = null,
        forStaticMembers: CfirTowerDataContext? = null,
        forConstructorHeaders: CfirTowerDataContext? = null,
        forEnumConstructors: CfirTowerDataContext? = null,
        primaryConstructorPureParametersScope: CfirLocalScopeImpl? = null,
        primaryConstructorAllParametersScope: CfirLocalScopeImpl? = null,
    ) : this(
        enumMap(regular, forNestedClasses, forStaticMembers, forConstructorHeaders, forEnumConstructors),
        primaryConstructorPureParametersScope,
        primaryConstructorAllParametersScope,
        CfirTowerDataMode.REGULAR,
    )

    val currentContext: CfirTowerDataContext?
        get() = modeMap[activeMode]

    fun replaceTowerDataMode(newMode: CfirTowerDataMode): CfirRegularTowerDataContexts =
        if (newMode == activeMode) this
        else CfirRegularTowerDataContexts(modeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, newMode)

    fun replaceCurrentlyActiveContext(newContext: CfirTowerDataContext): CfirRegularTowerDataContexts {
        val newModeMap = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
        newModeMap.putAll(modeMap)
        newModeMap[activeMode] = newContext
        return CfirRegularTowerDataContexts(newModeMap, primaryConstructorPureParametersScope, primaryConstructorAllParametersScope, activeMode)
    }

    fun replaceAndSetActiveRegularContext(newContext: CfirTowerDataContext): CfirRegularTowerDataContexts {
        val newModeMap = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
        newModeMap.putAll(modeMap)
        newModeMap[CfirTowerDataMode.REGULAR] = newContext
        return CfirRegularTowerDataContexts(
            newModeMap,
            primaryConstructorPureParametersScope,
            primaryConstructorAllParametersScope,
            CfirTowerDataMode.REGULAR,
        )
    }

    companion object {
        private fun enumMap(
            regular: CfirTowerDataContext,
            forNestedClasses: CfirTowerDataContext?,
            forStaticMembers: CfirTowerDataContext?,
            forConstructorHeaders: CfirTowerDataContext?,
            forEnumConstructors: CfirTowerDataContext?,
        ): EnumMap<CfirTowerDataMode, CfirTowerDataContext> {
            val result = EnumMap<CfirTowerDataMode, CfirTowerDataContext>(CfirTowerDataMode::class.java)
            result[CfirTowerDataMode.REGULAR] = regular
            result[CfirTowerDataMode.NESTED_CLASS] = forNestedClasses ?: regular
            result[CfirTowerDataMode.STATIC_MEMBER] = forStaticMembers ?: regular
            result[CfirTowerDataMode.CONSTRUCTOR_HEADER] = forConstructorHeaders ?: regular
            result[CfirTowerDataMode.ENUM_CONSTRUCTOR] = forEnumConstructors ?: regular
            return result
        }
    }
}

data class CfirPostponedAtomsResolutionContext(
    val towerDataContext: CfirTowerDataContext,
    val inferenceSession: CfirInferenceSession,
)

class CfirSpecialTowerDataContexts {
    private val anonymousFunctionContexts = LinkedHashMap<CfirFunctionSymbol<*>, CfirPostponedAtomsResolutionContext>()
    private val callableReferenceContexts = IdentityHashMap<CfirExpression, CfirPostponedAtomsResolutionContext>()

    fun clear() {
        anonymousFunctionContexts.clear()
        callableReferenceContexts.clear()
    }

    fun getAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>): CfirPostponedAtomsResolutionContext? =
        anonymousFunctionContexts[symbol]

    fun storeAnonymousFunctionContext(
        symbol: CfirFunctionSymbol<*>,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        anonymousFunctionContexts[symbol] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    fun dropAnonymousFunctionContext(symbol: CfirFunctionSymbol<*>) {
        anonymousFunctionContexts.remove(symbol)
    }

    fun getCallableReferenceContext(access: CfirExpression): CfirPostponedAtomsResolutionContext? =
        callableReferenceContexts[access]

    fun storeCallableReferenceContext(
        access: CfirExpression,
        towerDataContext: CfirTowerDataContext,
        inferenceSession: CfirInferenceSession,
    ) {
        callableReferenceContexts[access] = CfirPostponedAtomsResolutionContext(towerDataContext, inferenceSession)
    }

    fun dropCallableReferenceContext(access: CfirExpression) {
        callableReferenceContexts.remove(access)
    }
}

abstract class CfirInferenceSession {
    open fun baseConstraintStorageForCandidate(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? = null

    open fun customCompletionModeInsteadOfFull(call: org.cangnova.cangjie.cfir.CfirResolvable): ConstraintSystemCompletionMode? = null

    open fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : org.cangnova.cangjie.cfir.CfirResolvable, T : org.cangnova.cangjie.cfir.expressions.CfirExpression {
    }

    open fun runLambdaCompletion(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        block()
        return null
    }

    open fun addSubtypeConstraintIfCompatible(
        lowerType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        upperType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        system: ConstraintSystemImpl,
    ) {
    }

    companion object {
        val DEFAULT: CfirInferenceSession = object : CfirInferenceSession() {}
    }
}

class CfirPCLAInferenceSession(
    private val candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
    private val inferenceComponents: org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents,
) : CfirInferenceSession()

private fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)
