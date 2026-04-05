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
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
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

        val delegationCalls = body?.collectDelegationCalls().orEmpty()
        val firstStatementDelegation = body?.statements?.firstOrNull().asDelegationCallOrNull()

        delegationCalls
            .filter { delegation -> delegation.call !== firstStatementDelegation?.call }
            .forEach { delegation ->
                reporter.reportOn(
                    source = delegation.call.source ?: declaration.source,
                    factory = CfirErrors.ILLEGAL_THIS_OR_SUPER_CALL,
                    a = delegation.kind.keyword,
                )
            }

        when (firstStatementDelegation?.kind) {
            DelegationKind.THIS -> checkThisDelegation(owner, declaration, firstStatementDelegation.call)
            DelegationKind.SUPER -> checkSuperDelegation(owner, declaration, firstStatementDelegation.call)
            null -> checkImplicitSuperRequirement(owner, declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkThisDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        val constructors = owner.declarations.filterIsInstance<CfirConstructor>()
        val candidates = constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.NO_CONSTRUCTOR,
            )

            candidates.size > 1 -> reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL,
                a = owner.classLikeName(),
            )

            declaration.hasDelegationCycle(constructors) -> reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.RECURSIVE_CONSTRUCTOR_CALL,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperDelegation(
        owner: CfirClassLikeDeclaration,
        declaration: CfirConstructor,
        call: CfirFunctionCall,
    ) {
        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: run {
            reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.NO_CONSTRUCTOR,
            )
            return
        }
        val constructors = superDeclaration.declarations.filterIsInstance<CfirConstructor>()
        val candidates = constructors.filter { constructor -> constructor.matchesDelegationCall(call) }

        when {
            candidates.isEmpty() -> reporter.reportOn(
                source = call.source ?: declaration.source,
                factory = CfirErrors.NO_CONSTRUCTOR,
            )

            candidates.size > 1 -> reporter.reportOn(
                source = call.source ?: declaration.source,
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
        if (declaration.body?.collectDelegationCalls()?.isNotEmpty() == true) return

        val superDeclaration = owner.directConcreteSuperDeclaration(context) ?: return
        val hasImplicitSuper = superDeclaration.declarations
            .filterIsInstance<CfirConstructor>()
            .any { constructor -> constructor.requiredParameterCount() == 0 }
        if (hasImplicitSuper) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.EXPLICIT_SUPER_CALL_REQUIRED,
        )
    }
}

private enum class DelegationKind(val keyword: String) {
    THIS("this"),
    SUPER("super"),
}

private data class ConstructorDelegationCall(
    val kind: DelegationKind,
    val call: CfirFunctionCall,
)

private fun CfirElement?.asDelegationCallOrNull(): ConstructorDelegationCall? {
    if (this is CfirWrappedExpression) {
        return expression.asDelegationCallOrNull()
    }
    val call = this as? CfirFunctionCall ?: return null
    val calleeName = (call.calleeReference as? CfirNamedReference)?.name?.asString() ?: return null
    return when (calleeName) {
        "this" -> ConstructorDelegationCall(DelegationKind.THIS, call)
        "super" -> ConstructorDelegationCall(DelegationKind.SUPER, call)
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

private fun CfirConstructor.requiredParameterCount(): Int =
    valueParameters.count { it.defaultValue == null }

private fun CfirConstructor.hasDelegationCycle(constructors: List<CfirConstructor>): Boolean {
    val visited = linkedSetOf<CfirConstructor>()
    var current: CfirConstructor? = this

    while (current != null && visited.add(current)) {
        val delegationCall = current.body?.statements?.firstOrNull().asDelegationCallOrNull()
        if (delegationCall?.kind != DelegationKind.THIS) return false

        val candidates = constructors.filter { constructor -> constructor.matchesDelegationCall(delegationCall.call) }
        if (candidates.size != 1) return false
        current = candidates.single()
    }

    return current != null
}

private fun CfirClassLikeDeclaration.directConcreteSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    return superTypeRefs
        .mapNotNull { superTypeRef -> superTypeRef.toResolvedSuperDeclaration(context) }
        .firstOrNull { superDeclaration -> superDeclaration !is CfirInterface }
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    if (resolvedTypeRef.coneType is ConeErrorType) return null
    return resolvedTypeRef.coneType.toResolvedSuperDeclaration(context)
}

private fun ConeCangJieType.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val expanded = fullyExpandTypeAlias()
    val classId = when (expanded) {
        is ConePrimitiveType -> expanded.kind.classId
        is ConeClassLikeType -> expanded.classId
        is ConeStructType -> expanded.classId
        is ConeEnumType -> expanded.classId
        is ConeTypeAliasType -> expanded.classId
        else -> null
    } ?: return null

    return context.session.cfirProvider.getClassByClassId(classId)
        ?: context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

private fun ConeCangJieType.fullyExpandTypeAlias(): ConeCangJieType {
    var current = this
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
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
