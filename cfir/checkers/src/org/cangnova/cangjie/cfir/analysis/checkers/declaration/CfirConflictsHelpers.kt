package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.symbols.constructType

import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap

import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.CjmpGate
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.declarations.hasInitialDefaultValue
import org.cangnova.cangjie.cfir.declarations.typeConstraintDiagnosticData
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.scopes.CfirPackageScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckCandidate
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckInput
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckNames
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckFunctionKind
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckScopeIdentity
import org.cangnova.cangjie.cfir.scopes.CfirDeclarationPreCheckView
import org.cangnova.cangjie.cfir.scopes.declarationPreCheckCandidateProvider
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.declarations.declarationOverloadabilityHelper
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirErrorCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.utils.SmartSet

/**
 * 声明冲突图的共享收集器。general 与函数参数签名分别执行官方预检查规则，
 * 所有名字、角色、物理/逻辑归属和声明顺序都来自同一不可变输入。
 */
internal class CfirDeclarationCollector<D : CfirBasedSymbol<*>>(
    internal val context: CheckerContext,
) {
    private val sourceOrder = CfirDeclarationConflictSourceOrder(context.session)
    val declarationConflictingSymbols: HashMap<D, SmartSet<CfirBasedSymbol<*>>> = hashMapOf()
    var hasLogicalMemberRedeclarationContext: Boolean = false
        private set

    private fun collectInput(input: CfirDeclarationPreCheckInput, reported: Set<CfirBasedSymbol<*>>?) {
        hasLogicalMemberRedeclarationContext = input.isCjmpActualized
        collectGeneralRedeclarations(input, reported)
        for (group in input.functionGroups) {
            val functions = group.candidates.filter { it.declaration.symbol.isCollectable() }
            for (index in functions.indices) {
                val current = functions[index]
                for (previous in functions.take(index)) {
                    if (isPackagePeerOutsideDependencyGraph(input, current, previous) ||
                        areTopLevelPrivateDeclarationsInDifferentFiles(current, previous)
                    ) continue
                    val currentSymbol = current.declaration.symbol as CfirCallableSymbol<*>
                    val previousSymbol = previous.declaration.symbol as CfirCallableSymbol<*>
                    if (context.session.declarationOverloadabilityHelper.isConflicting(
                            currentSymbol, previousSymbol,
                            substitutorByMap((current.ownerTypeParameterMapping).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, context.session),
                            substitutorByMap((previous.ownerTypeParameterMapping).entries.associate { (origin, target) -> origin.symbol to target.symbol.constructType() }, context.session),
                        )
                    ) {
                        recordConflict(currentSymbol, previousSymbol, reported)
                    }
                }
            }
        }
    }

    /** general 阶段保留所有声明，按 current 与 found.front 的自身角色成对豁免。 */
    private fun collectGeneralRedeclarations(input: CfirDeclarationPreCheckInput, reported: Set<CfirBasedSymbol<*>>?) {
        val declarationsByName = linkedMapOf<CfirDeclarationPreCheckNames, MutableList<CfirDeclarationPreCheckCandidate>>()
        for (candidate in input.rawCandidates.flatMap { generalCandidates(it, input.scope) }) {
            val names = candidate.names ?: continue
            if (!candidate.declaration.symbol.isCollectable()) continue
            val previous = declarationsByName.getOrPut(names, ::mutableListOf)
            val kind = candidate.functionKind
            val functionOverloading = kind != null && kind != CfirDeclarationPreCheckFunctionKind.MACRO &&
                    previous.any { it.functionKind != null && it.functionKind != CfirDeclarationPreCheckFunctionKind.MACRO }
            val macroOverloading = kind == CfirDeclarationPreCheckFunctionKind.MACRO &&
                    previous.any { it.functionKind == CfirDeclarationPreCheckFunctionKind.MACRO }
            // 原生 enum VAR/FUNC 两类构造项可共名；同一类别的后续声明仍进入正常重定义。
            val enumConstructorOverloading = candidate.declaration is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor &&
                    previous.none { (it.functionKind != null) == (kind != null) }
            val ignoredHere = functionOverloading || macroOverloading || enumConstructorOverloading ||
                    kind == CfirDeclarationPreCheckFunctionKind.CONSTRUCTOR ||
                    kind == CfirDeclarationPreCheckFunctionKind.MAIN
            val first = previous.firstOrNull()
            previous += candidate
            if (ignoredHere || first == null || isPackagePeerOutsideDependencyGraph(input, candidate, first) ||
                areTopLevelPrivateDeclarationsInDifferentFiles(candidate, first) ||
                isOppositeCjmpRolePair(input, candidate, first)
            ) continue
            recordConflict(candidate.declaration.symbol, first.declaration.symbol, reported)
        }
    }

    /** irrefutable pattern 的可见绑定分别参与 general 重定义，wildcard 没有声明名。 */
    private fun generalCandidates(
        candidate: CfirDeclarationPreCheckCandidate,
        scope: CfirDeclarationPreCheckScopeIdentity,
    ): List<CfirDeclarationPreCheckCandidate> {
        val pattern = candidate.declaration as? CfirPatternVariable ?: return listOf(candidate)
        return pattern.pattern.visibleBindingVariables().map { binding ->
            candidate.copy(
                declaration = binding,
                names = CfirDeclarationPreCheckNames(binding.name, scope),
                attributes = candidate.attributes.copy(
                    isCommon = binding.status.isCommon,
                    isSpecific = binding.status.isSpecific,
                ),
                sourceStartOffset = binding.source?.startOffset,
                sourceEndOffset = binding.source?.endOffset,
            )
        }
    }

    private fun isOppositeCjmpRolePair(
        input: CfirDeclarationPreCheckInput,
        current: CfirDeclarationPreCheckCandidate,
        previous: CfirDeclarationPreCheckCandidate,
    ): Boolean = input.view != CfirDeclarationPreCheckView.RAW &&
            (current.attributes.isCommon && previous.attributes.isSpecific ||
                    current.attributes.isSpecific && previous.attributes.isCommon)

    private fun areTopLevelPrivateDeclarationsInDifferentFiles(
        first: CfirDeclarationPreCheckCandidate,
        second: CfirDeclarationPreCheckCandidate,
    ): Boolean =
        first.logicalOwner == null && second.logicalOwner == null &&
                (first.declaration as? CfirMemberDeclaration)?.status?.visibility == Visibilities.Private &&
                (second.declaration as? CfirMemberDeclaration)?.status?.visibility == Visibilities.Private &&
                first.sourceFileIdentity != null && second.sourceFileIdentity != null &&
                first.sourceFileIdentity != second.sourceFileIdentity

    private fun isPackagePeerOutsideDependencyGraph(
        input: CfirDeclarationPreCheckInput,
        first: CfirDeclarationPreCheckCandidate,
        second: CfirDeclarationPreCheckCandidate,
    ): Boolean {
        if (input.scope !is CfirDeclarationPreCheckScopeIdentity.Package) return false
        val firstModule = first.declaration.moduleData
        val secondModule = second.declaration.moduleData
        return firstModule != secondModule &&
                firstModule !in secondModule.dependencies && firstModule !in secondModule.allRefinementDependencies &&
                secondModule !in firstModule.dependencies && secondModule !in firstModule.allRefinementDependencies
    }

    @Suppress("UNCHECKED_CAST")
    private fun recordConflict(
        current: CfirBasedSymbol<*>,
        previous: CfirBasedSymbol<*>,
        reported: Set<CfirBasedSymbol<*>>?,
    ) {
        fun record(owner: CfirBasedSymbol<*>, peer: CfirBasedSymbol<*>) {
            if (reported == null || owner in reported) {
                declarationConflictingSymbols.getOrPut(owner as D) { SmartSet.create() } += peer
            }
        }
        val signature = functionConflictSignature(current)
        if (signature != null && signature == functionConflictSignature(previous)) {
            record(current, previous)
            record(previous, current)
        } else if (!hasLogicalMemberRedeclarationContext &&
            sourceOrder.compareForDiagnosticRepresentative(current, previous) < 0
        ) {
            record(previous, current)
        } else {
            record(current, previous)
        }
    }

    /** 包级输入跨文件共用，但每个文件仅报告其真正拥有的声明。 */
    fun collectTopLevel(file: CfirFile) {
        val reported = file.declarations.flatMap { declaration ->
            if (declaration is CfirPatternVariable) declaration.pattern.visibleBindingVariables().map { it.symbol }
            else listOf(declaration.symbol)
        }.toSet()
        collectInput(context.declarationPreCheckInputFor(file), reported)
    }

    fun collectClassMembers(declaration: CfirClassLikeDeclaration) {
        collectInput(context.declarationPreCheckInputFor(declaration), reported = null)
    }

    fun collectExtendMembers(declaration: CfirExtend) {
        collectInput(context.declarationPreCheckInputFor(declaration), reported = null)
    }
}

/** 诊断语境选择明确输入域，使用 CheckerContext 的语言门；FINAL 从不按异常回退。 */
internal fun CheckerContext.declarationPreCheckInputFor(owner: CfirDeclaration): CfirDeclarationPreCheckInput {
    val view = if (CjmpGate.isEnabled(this) && session.isCjmpSpecificCompilationEnabled()) {
        CfirDeclarationPreCheckView.CJMP_FINAL
    } else {
        CfirDeclarationPreCheckView.RAW
    }
    return session.declarationPreCheckCandidateProvider.inputFor(owner, scopeSession, view)
}

/**
 * 检查同一局部声明列表中的重声明。
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun checkForLocalRedeclarations(elements: List<org.cangnova.cangjie.cfir.CfirElement>) {
    if (elements.size <= 1) return

    val groupedByName = linkedMapOf<Name, MutableList<CfirBasedSymbol<*>>>()

    for (element in elements) {
        val (symbol, name) = when (element) {
            is CfirVariable -> element.symbol to element.symbol.name
            is CfirClassLikeDeclaration -> {
                val symbol = element.symbol as? CfirClassLikeSymbol<*> ?: continue
                symbol to symbol.name
            }
            is CfirTypeParameterRef -> element.symbol to element.symbol.name
            else -> continue
        }

        if (!name.isSpecial) {
            groupedByName.getOrPut(name, ::mutableListOf).add(symbol)
        }
    }

    groupedByName.values.forEach { conflictingSymbols ->
        if (conflictingSymbols.size <= 1) return@forEach
        val rendered = conflictingSymbols.renderNames()
        conflictingSymbols.sortedBy { symbol ->
            symbol.boundSourceOrNull()?.startOffset ?: Int.MAX_VALUE
        }.drop(1).forEach { conflictingSymbol ->
            reporter.reportOn(conflictingSymbol.boundSourceOrNull(), CfirErrors.REDECLARATION, rendered)
        }
    }
}

/**
 * 判断 callable 符号从当前 class 声明内是否可见。
 */
private fun CfirCallableSymbol<*>.isVisibleInClass(
    classDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val ownerClassId = getContainingClass()?.classId ?: return true
    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return ownerClassId == currentClassId
}

/**
 * 判断 callable 是否是 scope 生成的 substitution override。
 */
private fun CfirCallableSymbol<*>.isScopeGeneratedSubstitutionOverride(): Boolean =
    isBound && cfir.origin is CfirDeclarationOrigin.SubstitutionOverride

/**
 * 判断 classifier 符号从当前 class 声明内是否可见。
 */
private fun CfirClassLikeSymbol<*>.isVisibleInClass(classDeclaration: CfirClassLikeDeclaration): Boolean {
    if (!isBound) return true
    if (cfir.status.visibility != Visibilities.Private) return true

    val currentClassId = (classDeclaration.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return true
    return classId == currentClassId
}

/**
 * 收集 class-like 声明的构造器及其冲突表示文本。
 */
private fun collectConstructorsForClassLike(classLikeSymbol: CfirClassLikeSymbol<*>): List<Pair<CfirConstructorSymbol, String>> {
    if (!classLikeSymbol.isBound) return emptyList()
    val declaration = classLikeSymbol.cfir
    val constructors = mutableListOf<Pair<CfirConstructorSymbol, String>>()

    declaration.declarations
        .filterIsInstance<CfirConstructor>()
        .forEach { constructor ->
            val symbol = constructor.symbol
            val representation = CfirRedeclarationPresenter.represent(symbol, classLikeSymbol) ?: return@forEach
            constructors += symbol to representation
        }

    return constructors
}

/**
 * 判断符号是否应该纳入冲突收集。
 */
private fun CfirBasedSymbol<*>.isCollectable(): Boolean {
    if (!isBound) return true

    if (this is CfirCallableSymbol<*>) {
        if (this is CfirErrorCallableSymbol<*>) return false
        if (typeParameterSymbolsHaveNoName()) return false
    }

    return when (this) {
        is CfirFunctionSymbol<*> -> isCollectableAccordingToSource && name != SpecialNames.NO_NAME_PROVIDED
        is CfirClassLikeSymbol<*> -> name != SpecialNames.NO_NAME_PROVIDED
        is CfirPropertySymbol -> cfir.source?.kind != CjFakeSourceElementKind.EnumGeneratedDeclaration
        else -> cfir.source?.kind != CjFakeSourceElementKind.ClassDelegationField
    }
}

/**
 * 判断 callable 的类型参数中是否存在无名参数。
 */
private fun CfirCallableSymbol<*>.typeParameterSymbolsHaveNoName(): Boolean {
    if (!isBound) return false
    return cfir.typeParameters.filterIsInstance<CfirTypeParameter>().any { it.symbol.name == SpecialNames.NO_NAME_PROVIDED }
}

/**
 * 判断函数符号按源码来源是否可收集。
 */
private val CfirFunctionSymbol<*>.isCollectableAccordingToSource: Boolean
    get() = if (!isBound) {
        true
    } else {
        cfir.source?.kind !is CjFakeSourceElementKind || cfir.source?.kind == CjFakeSourceElementKind.DataClassGeneratedMembers
    }

/**
 * 返回已绑定符号的源码范围。
 */
private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

/**
 * redeclaration 诊断展示名渲染器。
 */
internal object CfirRedeclarationPresenter {
    /**
     * 官方 CheckCommonExtensions 比较 rawMangleName，不比较类型检查后的重载签名。
     * 保留原始类型注解、泛型名称和命名参数事实；推断结果不能补成源码未写的返回注解。
     */
    fun representCommonExtensionMember(
        member: CfirCallableDeclaration,
        owner: CfirExtend,
    ): CfirCommonExtensionMemberSignature? {
        require(member is CfirNamedFunction || member is CfirProperty)
        val ownerSource = checkNotNull(owner.source)
        val ownerTypes = CfirCjmpSourceTypeMangler.extendAnnotations(ownerSource) ?: return null
        val ownerType = ownerTypes.firstOrNull() ?: return null
        val constraints = owner.attributes.typeConstraintDiagnosticData?.typeConstraints.orEmpty()
            .map { constraint ->
                val name = CfirCjmpSourceTypeMangler.typeParameterReference(constraint.source) ?: return null
                val bounds = CfirCjmpSourceTypeMangler.directAnnotations(constraint.constraintSource) ?: return null
                name to bounds.sortedWith(cjmpUtf8Order)
            }
            .sortedWith(compareBy(cjmpUtf8Order) { it.first })
        val rawName = buildString {
            append(ownerType).append("<:")
            append(ownerTypes.drop(1).sortedWith(cjmpUtf8Order).joinToString("&"))
            for ((name, bounds) in constraints) {
                append('W').append(name)
                for (bound in bounds) append(':').append(bound)
            }
            append('X').append(cjmpRawMangleName(member.symbol.name.asString()))
            if (member.typeParameters.isNotEmpty()) {
                append('<')
                member.typeParameters.forEach { append(cjmpRawMangleName(it.symbol.name.asString())) }
                append('>')
            }
            if (member is CfirNamedFunction) {
                append('F')
                for (parameter in member.valueParameters) {
                    val annotations = CfirCjmpSourceTypeMangler.directAnnotations(checkNotNull(parameter.source)) ?: return null
                    if (annotations.size > 1) return null
                    annotations.singleOrNull()?.let(::append)
                    if (parameter.isNamed) {
                        append('!').append(cjmpRawMangleName(parameter.name.asString()))
                        if (parameter.hasInitialDefaultValue) append('=')
                    }
                }
                append("\$\$")
                val annotations = CfirCjmpSourceTypeMangler.directAnnotations(checkNotNull(member.source)) ?: return null
                if (annotations.size > 1) return null
                annotations.singleOrNull()?.let(::append)
            } else append('P')
        }
        return CfirCommonExtensionMemberSignature(member.symbol.name, rawName)
    }

    /**
     * 渲染任意可支持符号的 redeclaration 表示。
     */
    fun represent(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> represent(symbol)
        is CfirCallableSymbol<*> -> represent(symbol)
        else -> null
    }

    /**
     * 渲染 classifier 符号。
     */
    fun represent(symbol: CfirClassLikeSymbol<*>): String? {
        return symbol.classId.shortClassName.asString()
    }

    /**
     * 渲染 callable 符号的稳定签名表示。
     */
    fun represent(symbol: CfirCallableSymbol<*>): String? {
        if (!symbol.isBound) return symbol.name.asString()

        return when (symbol) {
            is CfirConstructorSymbol -> {
                val ownerClassName = symbol.callableId.classId?.shortClassName ?: symbol.name
                val constructor = symbol.cfir
                // 静态初始化器 `static init()`（官方建模为 `static.init`）与普通构造器 `init`
                // 语义不同：同签名不应互相冲突，只应与同签名静态初始化器冲突。因此用独立的
                // static 前缀区分签名，避免把 `static init()` 与普通 `init()` 误判为重声明。
                val ownerText = if (constructor.status.isStatic) {
                    "static." + ownerClassName.asString()
                } else {
                    ownerClassName.asString()
                }
                constructorRepresentation(Name.identifier(ownerText), constructor.valueParameters.map(CfirVariable::returnTypeRef))
            }

            is CfirEnumConstructorSymbol -> {
                val enumConstructor = symbol.cfir
                constructorRepresentation(symbol.name, enumConstructor.valueParameters.map(CfirVariable::returnTypeRef))
            }

            is CfirFunctionSymbol<*> -> {
                val function = symbol.cfir
                val receiverPrefix = function.dispatchReceiverType?.renderForDebugging()?.let { "$it." }.orEmpty()
                val parameters = function.valueParameters.joinToString(",") { parameter ->
                    parameter.returnTypeRef.toStableSignatureKey()
                }
                receiverPrefix + symbol.name.asString() + "(" + parameters + ")"
            }

            else -> {
                val callable = symbol.cfir
                val receiverPrefix = callable.dispatchReceiverType?.renderForDebugging()?.let { "$it." }.orEmpty()
                receiverPrefix + constructorRepresentation(symbol.name, emptyList())
            }
        }
    }

    /**
     * 使用指定 owner class 渲染构造器符号。
     */
    fun represent(symbol: CfirConstructorSymbol, containingClass: CfirClassLikeSymbol<*>): String? {
        if (!symbol.isBound) return null
        val constructor = symbol.cfir
        return constructorRepresentation(
            containingClass.classId.shortClassName,
            constructor.valueParameters.map(CfirVariable::returnTypeRef),
        )
    }

    /**
     * 渲染构造器样式的 `Name(paramTypes)` 表示。
     */
    fun constructorRepresentation(name: Name, parameterTypeRefs: List<CfirTypeRef>): String {
        val parameterTypes = parameterTypeRefs.joinToString(",") { it.toStableSignatureKey() }
        return "${name.asString()}($parameterTypes)"
    }

    /**
     * 渲染诊断参数中的短名称。
     */
    fun diagnosticName(symbol: CfirBasedSymbol<*>): String? = when (symbol) {
        is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
        is CfirCallableSymbol<*> -> symbol.name.asString()
        else -> symbol.debugName
    }
}

/** common extend 的原始声明签名；static 与属性类型不属于官方 rawMangleName 的区分项。 */
internal data class CfirCommonExtensionMemberSignature(val name: Name, val rawName: String)

/**
 * 将类型引用渲染为稳定签名 key。
 */
private fun CfirTypeRef.toStableSignatureKey(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.renderForDebugging()
    else -> toString()
}

/**
 * 渲染符号集合的诊断名称列表。
 */
private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()
