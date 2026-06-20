package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 对齐 Kotlin FIR 的 constructor delegation issues checker 思路：
 * - 构造器委托调用是“构造器语义”，不是普通函数调用；
 * - 参数匹配仍复用调用解析基础设施；
 * - delegation 的位置、循环与父类构造器要求在专门的 constructor checker 中统一处理。
 */
object CfirConstructorDelegationChecker : CfirConstructorChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirConstructor) {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return
        val body = declaration.body

        declaration.checkMultiplePrimaryConstructors(owner)

        val delegationCalls = body?.collectDelegationCalls().orEmpty()
        val firstStatementDelegation = body?.statements?.firstOrNull().asDelegationCallOrNull()

        delegationCalls
            .filter { delegation -> delegation.call !== firstStatementDelegation?.call }
            .forEach { delegation ->
                reporter.reportOn(
                    source = delegation.call.delegationDiagnosticSource()?.firstCharacterDiagnosticSource()
                        ?: declaration.source?.firstCharacterDiagnosticSource(),
                    factory = CfirErrors.sema_illegal_place_of_calling_this_or_super,
                    a = delegation.kind.keyword,
                )
            }

        firstStatementDelegation?.checkArgumentMemberAccessBeforeInitialization(owner)

        when (firstStatementDelegation?.kind) {
            ConstructorDelegationCallKind.THIS -> {
                if (declaration.isPrimary) {
                    reporter.reportOn(
                        source = firstStatementDelegation.call.delegationDiagnosticSource()?.firstCharacterDiagnosticSource()
                            ?: declaration.source?.firstCharacterDiagnosticSource(),
                        factory = CfirErrors.sema_illegal_place_of_calling_this_primary_constructor,
                    )
                    return
                }
                checkThisDelegation(owner, declaration, firstStatementDelegation.call)
            }
            ConstructorDelegationCallKind.SUPER -> checkSuperDelegation(owner, declaration, firstStatementDelegation.call)
            null -> checkImplicitSuperRequirement(owner, declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun CfirConstructor.checkMultiplePrimaryConstructors(owner: CfirClassLikeDeclaration) {
        if (!isPrimary) return
        val primaryConstructors = owner.declarations
            .asSequence()
            .filterIsInstance<CfirConstructor>()
            .filter(CfirConstructor::isPrimary)
            .toList()
        val firstPrimary = primaryConstructors.minByOrNull { constructor ->
            constructor.source?.startOffset ?: Int.MAX_VALUE
        } ?: return
        if (this === firstPrimary) return

        reporter.reportOn(
            source = source?.firstCharacterDiagnosticSource(),
            factory = CfirErrors.sema_multiple_primary_constructors,
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkThisDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        val constructors = owner.declarations.filterIsInstance<CfirConstructor>()
        val resolvedConstructor = call.resolvedDelegatedConstructorOrNull()?.takeIf { constructor -> constructor in constructors }
        val candidates = resolvedConstructor?.let(::listOf)
            ?: constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> {
                if (reportConstructorArgumentCountMismatch(listOf(declaration) + constructors.filter { it !== declaration }, call)) {
                    return
                }
                reporter.reportOn(
                    source = call.delegationDiagnosticSource() ?: declaration.source,
                    factory = CfirErrors.NO_CONSTRUCTOR,
                )
            }

            candidates.size > 1 -> reporter.reportOn(
                source = call.delegationDiagnosticSource() ?: declaration.source,
                factory = CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
                a = owner.classLikeName(),
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        // 官方实现只在存在实际父类时检查父类构造器；无显式父类的 `super()` 不产生 NO_CONSTRUCTOR。
        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: return
        val constructors = superDeclaration.declarations.filterIsInstance<CfirConstructor>()
        val resolvedConstructor = call.resolvedDelegatedConstructorOrNull()?.takeIf { constructor -> constructor in constructors }
        val candidates = resolvedConstructor?.let(::listOf)
            ?: constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> {
                if (reportConstructorArgumentCountMismatch(constructors, call)) {
                    return
                }
                reporter.reportOn(
                    source = call.delegationDiagnosticSource() ?: declaration.source,
                    factory = CfirErrors.NO_CONSTRUCTOR,
                )
            }

            candidates.size > 1 -> reporter.reportOn(
                source = call.delegationDiagnosticSource() ?: declaration.source,
                factory = CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
                a = superDeclaration.classLikeName(),
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkImplicitSuperRequirement(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
    ) {
        if (owner !is CfirClass) return

        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: return
        val hasImplicitSuper = superDeclaration.declarations
            .filterIsInstance<CfirConstructor>()
            .any { constructor -> constructor.requiredParameterCount() == 0 }
        if (hasImplicitSuper) return

        reporter.reportOn(
            source = declaration.constructorNameDiagnosticSource(),
            factory = CfirErrors.sema_no_non_param_constructor_in_super_class,
        )
    }
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun reportConstructorArgumentCountMismatch(
    constructors: List<CfirConstructor>,
    call: CfirFunctionCall,
): Boolean {
    val argumentCount = call.argumentList.arguments.size
    val tooManyTarget = constructors.firstOrNull { constructor -> argumentCount > constructor.valueParameters.size }
    if (tooManyTarget != null) {
        val source = call.argumentList.arguments.getOrNull(tooManyTarget.valueParameters.size)?.source
            ?: call.delegationDiagnosticSource()
        reporter.reportOn(
            source = source,
            factory = CfirErrors.TOO_MANY_ARGUMENTS,
            a = call.delegationName(),
        )
        return true
    }

    val missingTarget = constructors.firstOrNull { constructor -> argumentCount < constructor.requiredParameterCount() }
        ?: return false
    val missingParameter = missingTarget.valueParameters
        .drop(argumentCount)
        .firstOrNull { parameter -> parameter.defaultValue == null }
        ?: return false
    reporter.reportOn(
        source = call.source ?: call.delegationDiagnosticSource(),
        factory = CfirErrors.NO_VALUE_FOR_PARAMETER,
        a = missingParameter.name,
    )
    return true
}

internal enum class ConstructorDelegationCallKind(val keyword: String) {
    THIS("this"),
    SUPER("super"),
}

private data class ConstructorDelegationCall(
    val kind: ConstructorDelegationCallKind,
    val call: CfirFunctionCall,
)

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun ConstructorDelegationCall.checkArgumentMemberAccessBeforeInitialization(owner: CfirClassLikeDeclaration) {
    val place = when (kind) {
        ConstructorDelegationCallKind.THIS -> ConstructorMemberAccessPlace.THIS_DELEGATION_ARGUMENT
        ConstructorDelegationCallKind.SUPER -> ConstructorMemberAccessPlace.SUPER_DELEGATION_ARGUMENT
    }
    call.argumentList.arguments.forEach { argument ->
        argument.checkConstructorMemberAccessBeforeInitialization(owner, place)
    }
}

/**
 * 构造器 delegation 相关诊断都应该尽量锚定在 `this` / `super` 关键字本身，
 * 这样既贴近 Kotlin FIR 的报错体验，也能避免把整段调用都染成同一类构造器语义错误。
 */
private fun CfirFunctionCall.delegationDiagnosticSource() = calleeReference.source ?: source

private fun CfirFunctionCall.delegationName(): Name =
    (calleeReference as? CfirNamedReference)?.name ?: Name.special("<constructor>")

private fun CfirElement?.asDelegationCallOrNull(): ConstructorDelegationCall? {
    val call = constructorDelegationCallOrNull() ?: return null
    val kind = call.constructorDelegationKindOrNull() ?: return null
    return ConstructorDelegationCall(kind, call)
}

internal fun CfirElement?.constructorDelegationCallOrNull(): CfirFunctionCall? {
    if (this is CfirWrappedExpression) {
        return expression.constructorDelegationCallOrNull()
    }
    val call = this as? CfirFunctionCall ?: return null
    return call.takeIf { it.constructorDelegationKindOrNull() != null }
}

internal fun CfirFunctionCall.constructorDelegationKindOrNull(): ConstructorDelegationCallKind? {
    return when (origin.toDelegationKindOrNull()) {
        ConstructorDelegationCallKind.THIS -> ConstructorDelegationCallKind.THIS
        ConstructorDelegationCallKind.SUPER -> ConstructorDelegationCallKind.SUPER
        null -> when ((calleeReference as? CfirNamedReference)?.name?.asString()) {
            "this" -> ConstructorDelegationCallKind.THIS
            "super" -> ConstructorDelegationCallKind.SUPER
            else -> null
        }
    }
}

private fun CfirFunctionCallOrigin.toDelegationKindOrNull(): ConstructorDelegationCallKind? {
    return when (this) {
        CfirFunctionCallOrigin.ConstructorDelegationThis -> ConstructorDelegationCallKind.THIS
        CfirFunctionCallOrigin.ConstructorDelegationSuper -> ConstructorDelegationCallKind.SUPER
        else -> null
    }
}

private fun org.cangnova.cangjie.cfir.expressions.CfirBlock.collectDelegationCalls(): List<ConstructorDelegationCall> {
    val result = mutableListOf<ConstructorDelegationCall>()
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            element.asDelegationCallOrNull()?.let(result::add)
            element.acceptChildren(this, null)
        }
    }, null)
    return result
}

private fun CfirConstructor.matchesDelegationCall(call: CfirFunctionCall): Boolean {
    val argumentCount = call.argumentList.arguments.size
    val minimum = requiredParameterCount()
    val maximum = valueParameters.size
    return argumentCount in minimum..maximum
}

private fun CfirFunctionCall.resolvedDelegatedConstructorOrNull(): CfirConstructor? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirConstructor
        is CfirNamedReferenceWithCandidateBase ->
            reference.takeUnless { it is CfirDiagnosticHolder }?.candidateSymbol?.cfir as? CfirConstructor
        else -> null
    }
}

internal fun CfirConstructor.requiredParameterCount(): Int =
    valueParameters.count { it.defaultValue == null }

internal fun CfirClassLikeDeclaration.directConcreteSuperDeclaration(
    context: CheckerContext,
    includeLoopInSupertypeError: Boolean = false,
): CfirClassLikeDeclaration? {
    return superTypeRefs
        .mapNotNull { superTypeRef ->
            superTypeRef.toResolvedSuperDeclaration(
                context = context,
                includeLoopInSupertypeError = includeLoopInSupertypeError,
            )
        }
        .firstOrNull { superDeclaration -> superDeclaration !is CfirInterface }
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toResolvedSuperDeclaration(
    context: CheckerContext,
    includeLoopInSupertypeError: Boolean,
): CfirClassLikeDeclaration? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    val coneType = resolvedTypeRef.coneType
    val effectiveTypeRef = if (coneType is ConeErrorType) {
        if (!includeLoopInSupertypeError) return null
        val diagnostic = coneType.diagnostic as? ConeSimpleDiagnostic ?: return null
        if (diagnostic.kind != DiagnosticKind.LoopInSupertype) return null
        resolvedTypeRef.delegatedTypeRef as? CfirResolvedTypeRef ?: return null
    } else {
        resolvedTypeRef
    }
    return effectiveTypeRef.coneType.toResolvedSuperDeclaration(context)
}

private fun ConeCangJieType.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val expanded = fullyExpandTypeAlias(context)
    val classId = when (expanded) {
        is ConePrimitiveType -> expanded.kind.classId
        is ConeClassLikeType -> expanded.classId
        is ConeStructType -> expanded.classId
        is ConeEnumType -> expanded.classId
        is ConeTypeAliasType -> expanded.classId
        else -> null
    } ?: return null

    return context.session.cfirProvider.getCfirClassifierByFqName(classId)
        ?: context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

private fun ConeCangJieType.fullyExpandTypeAlias(context: CheckerContext): ConeCangJieType {
    var current = this
    val visitedAliases = linkedSetOf<ClassId>()
    while (current is ConeTypeAliasType && visitedAliases.add(current.classId)) {
        val embeddedExpandedType = current.expandedType
        if (embeddedExpandedType != null) {
            current = embeddedExpandedType
            continue
        }
        val typeAlias = context.session.cfirProvider.getCfirClassifierByFqName(current.classId) as? CfirTypeAlias
            ?: context.session.symbolProvider.getClassLikeSymbolByClassId(current.classId)?.cfir as? CfirTypeAlias
            ?: break
        val expandedType = (typeAlias.expandedTypeRef as? CfirResolvedTypeRef)?.coneType ?: break
        current = expandedType
    }
    return current
}

private fun CfirClassLikeDeclaration.classLikeName(): Name = when (this) {
    is CfirPrimitiveTypeDeclaration -> name
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}

private val org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.classId: ClassId
    get() = ClassId.fromString(typeName)
