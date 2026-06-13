package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

object CfirSupertypesChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        checkDuplicateSupertypes(declaration)
        when (declaration) {
            is CfirInterface -> checkInterfaceSupertypes(declaration)
            is CfirClass, is CfirStruct, is CfirEnum -> {
                checkConcreteSupertypesOpenForInheritance(declaration)
                checkMultipleConcreteSupertypes(declaration)
            }
            else -> Unit
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDuplicateSupertypes(declaration: CfirClassLikeDeclaration) {
        val firstByKey = linkedMapOf<ConeCangJieType, ConcreteSupertype>()
        val reportedKeys = linkedSetOf<ConeCangJieType>()

        for (superTypeRef in declaration.superTypeRefs) {
            val superDeclaration = superTypeRef.toResolvedSuperDeclaration(context) ?: continue
            val key = superTypeRef.duplicateSupertypeKey(context) ?: continue
            val first = firstByKey.putIfAbsent(key, ConcreteSupertype(superTypeRef, superDeclaration.classLikeName()))
            if (first == null || !reportedKeys.add(key)) continue

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.SUPER_TYPES_DUPLICATE,
                a = first.name,
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInterfaceSupertypes(declaration: CfirInterface) {
        val ownerName = declaration.classLikeName()
        for (superTypeRef in declaration.superTypeRefs) {
            val superDeclaration = superTypeRef.toResolvedSuperDeclaration(context) ?: continue
            if (superDeclaration.classKindOrNull() == CfirClassKind.INTERFACE) continue

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS,
                a = ownerName,
                b = superDeclaration.classLikeName(),
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMultipleConcreteSupertypes(declaration: CfirClassLikeDeclaration) {
        val concreteSupers = declaration.superTypeRefs.mapNotNull { superTypeRef ->
            val superDeclaration = superTypeRef.toResolvedSuperDeclaration(context) ?: return@mapNotNull null
            if (superDeclaration.classKindOrNull() == CfirClassKind.INTERFACE) return@mapNotNull null
            ConcreteSupertype(superTypeRef, superDeclaration.classLikeName())
        }

        if (concreteSupers.size <= 1) return

        concreteSupers.drop(1).forEach { concreteSuper ->
            reporter.reportOn(
                source = concreteSuper.typeRef.source,
                factory = CfirErrors.MULTIPLE_CLASS_SUPER_TYPES,
                a = declaration.classLikeName(),
                b = concreteSupers.map { it.name },
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConcreteSupertypesOpenForInheritance(declaration: CfirClassLikeDeclaration) {
        val ownerKey = declaration.classLikeIdentityKey()

        for (superTypeRef in declaration.superTypeRefs) {
            val superDeclaration = superTypeRef.toResolvedSuperDeclaration(context) ?: continue
            if (superDeclaration.classKindOrNull() == CfirClassKind.INTERFACE) continue
            if (ownerKey != null && ownerKey == superDeclaration.classLikeIdentityKey()) continue
            if (!superDeclaration.requiresOpenForInheritance()) continue

            // 这是直接继承规则，属于 declaration checker 的职责；
            // 这里不借用类型不匹配等兜底诊断，而是稳定产出专门的继承语义错误。
            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.CLASS_NOT_OPEN_FOR_INHERITANCE,
                a = superDeclaration.classLikeName(),
            )
        }
    }
}

private data class ConcreteSupertype(
    val typeRef: CfirTypeRef,
    val name: Name,
)

private fun CfirTypeRef.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    if (resolvedTypeRef.coneType is ConeErrorType) return null
    return resolvedTypeRef.coneType.toResolvedSuperDeclaration(context)
}

private fun CfirTypeRef.duplicateSupertypeKey(context: CheckerContext): ConeCangJieType? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    if (resolvedTypeRef.coneType is ConeErrorType) return null
    return when (val type = resolvedTypeRef.coneType.fullyExpandedType(context.session)) {
        is ConePrimitiveType,
        is ConeClassLikeType,
        is ConeStructType,
        is ConeEnumType -> type
        else -> null
    }
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

    return context.session.cfirProvider.getCfirClassifierByFqName(classId)
        ?: context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
}

private fun ConeCangJieType.fullyExpandTypeAlias(): ConeCangJieType {
    var current = this
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
    }
    return current
}

private fun CfirClassLikeDeclaration.classKindOrNull(): CfirClassKind? = when (this) {
    is CfirPrimitiveTypeDeclaration -> CfirClassKind.CLASS
    is CfirClass -> CfirClassKind.CLASS
    is CfirInterface -> CfirClassKind.INTERFACE
    is CfirStruct -> CfirClassKind.STRUCT
    is CfirEnum -> CfirClassKind.ENUM
    is CfirTypeAlias -> null
}

private fun CfirClassLikeDeclaration.classLikeName(): Name = when (this) {
    is CfirPrimitiveTypeDeclaration -> name
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}

private fun CfirClassLikeDeclaration.classLikeIdentityKey(): String? = when (this) {
    is CfirPrimitiveTypeDeclaration -> "primitive:${kind.typeName}"
    is CfirClass -> "class:${symbol.classId}"
    is CfirInterface -> "interface:${symbol.classId}"
    is CfirStruct -> "struct:${symbol.classId}"
    is CfirEnum -> "enum:${symbol.classId}"
    is CfirTypeAlias -> null
}

private fun CfirClassLikeDeclaration.requiresOpenForInheritance(): Boolean = when (this) {
    is CfirPrimitiveTypeDeclaration -> true
    is CfirStruct -> true
    is CfirEnum -> true
    is CfirClass -> !status.isOpen && !status.isAbstract && !status.isSealed
    is CfirInterface -> false
    is CfirTypeAlias -> false
}

private val org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.classId: ClassId
    get() = ClassId.fromString(typeName)
