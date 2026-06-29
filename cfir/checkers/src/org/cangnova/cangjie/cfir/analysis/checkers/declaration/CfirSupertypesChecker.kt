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
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAllowsDelegatedScopeTraversalDiagnostic
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.withArguments
import org.cangnova.cangjie.cfir.types.withoutAbbreviation
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * class-like 直接父类型合法性检查器。
 *
 * 该检查器覆盖重复父类型、接口继承非接口、多个具体父类型以及继承 final concrete 类型等规则。
 */
object CfirSupertypesChecker : CfirClassLikeChecker() {
    /**
     * 检查 class-like 声明的所有直接父类型规则。
     */
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

    /**
     * 检查重复父类型，优先处理泛型实例化父接口重复。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDuplicateSupertypes(declaration: CfirClassLikeDeclaration) {
        if (checkInstantiatedDuplicateSuperInterfaces(declaration)) return
        checkDirectDuplicateSupertypes(declaration)
    }

    /**
     * 检查直接写出的重复父类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDirectDuplicateSupertypes(declaration: CfirClassLikeDeclaration) {
        val firstByKey = linkedMapOf<ConeCangJieType, ConcreteSupertype>()
        val reportedKeys = linkedSetOf<ConeCangJieType>()

        for (superTypeRef in declaration.superTypeRefs) {
            val superDeclaration = superTypeRef.toResolvedSuperDeclaration(context) ?: continue
            val key = superTypeRef.duplicateSupertypeKey(context) ?: continue
            val first = firstByKey.putIfAbsent(key, ConcreteSupertype(superTypeRef, superDeclaration.classLikeName()))
            if (first == null || !reportedKeys.add(key)) continue

            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.SUPER_TYPES_DUPLICATE,
                a = first.name,
            )
        }
    }

    /**
     * 检查实例化后的父接口是否重复。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInstantiatedDuplicateSuperInterfaces(declaration: CfirClassLikeDeclaration): Boolean {
        val duplicatedInterface = declaration.findInstantiatedDuplicateSuperInterface(
            substitutor = ConeSubstitutor.Empty,
            passedDeclarations = linkedSetOf(),
        ) ?: return false
        val source = declaration.classLikeDeclarationHeaderDiagnosticSource() ?: return false

        // 官方 PreCheck 对声明自身执行重复父接口递归检查；IDE 侧按项目 range policy
        // 报在声明头部。声明级重复已覆盖时不再额外产出直接 typeRef 重复诊断。
        reporter.reportOn(
            source = source,
            factory = CfirErrors.SUPER_TYPES_DUPLICATE,
            a = duplicatedInterface,
        )
        return true
    }

    /**
     * 检查 interface 是否只继承 interface。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInterfaceSupertypes(declaration: CfirInterface) {
        val ownerName = declaration.classLikeName()
        for (superTypeRef in declaration.superTypeRefs) {
            val superType = superTypeRef.toResolvedSupertypeForInterfaceLegality() ?: continue
            if (superType.toResolvedSuperDeclaration(context)?.classKindOrNull() == CfirClassKind.INTERFACE) {
                continue
            }

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS,
                a = ownerName,
                b = superType.supertypeDiagnosticName(context),
            )
        }
    }

    /**
     * 检查 class/struct/enum 是否声明了多个具体父类型。
     */
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

    /**
     * 检查具体父类型是否允许被当前声明继承。
     */
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

/**
 * 直接父类型及其诊断展示名称。
 *
 * @property typeRef 父类型引用。
 * @property name 父声明名称。
 */
private data class ConcreteSupertype(
    /**
     * 父类型引用。
     */
    val typeRef: CfirTypeRef,

    /**
     * 父声明名称。
     */
    val name: Name,
)

/**
 * 将父类型引用解析为 class-like 声明。
 */
private fun CfirTypeRef.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    return resolvedTypeRef.coneType.effectiveNominalSupertypeOrNull()?.toResolvedSuperDeclaration(context)
}

/**
 * interface 直接父类型必须是单一 nominal interface。
 *
 * 解析失败的错误类型不继续级联；但实参数量错误这类已解析到 nominal owner 的错误类型，
 * 可以按 delegated type 继续参与父类型合法性检查。
 */
private fun CfirTypeRef.toResolvedSupertypeForInterfaceLegality(): ConeCangJieType? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    return resolvedTypeRef.coneType.effectiveNominalSupertypeOrNull() ?: resolvedTypeRef.coneType
}

private fun ConeCangJieType.effectiveNominalSupertypeOrNull(): ConeCangJieType? =
    if (this is ConeErrorType) {
        if (diagnostic is ConeAllowsDelegatedScopeTraversalDiagnostic) delegatedType else null
    } else {
        this
    }

private fun ConeCangJieType.supertypeDiagnosticName(context: CheckerContext): Name {
    toResolvedSuperDeclaration(context)?.classLikeName()?.let { return it }
    return when (this) {
        is ConePrimitiveType -> Name.identifier(kind.typeName)
        is ConeIntersectionType -> Name.identifier("intersection")
        else -> Name.identifier("supertype")
    }
}

/**
 * 生成重复父类型比较使用的规范类型 key。
 */
private fun CfirTypeRef.duplicateSupertypeKey(context: CheckerContext): ConeCangJieType? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    if (resolvedTypeRef.coneType is ConeErrorType) return null
    return when (val type = resolvedTypeRef.coneType.normalizedDuplicateSupertypeKey(context)) {
        is ConePrimitiveType,
        is ConeClassLikeType,
        is ConeStructType,
        is ConeEnumType -> type
        else -> null
    }
}

/**
 * 规范化重复父类型比较中的类型。
 */
private fun ConeCangJieType.normalizedDuplicateSupertypeKey(context: CheckerContext): ConeCangJieType {
    val expanded = fullyExpandedType(context.session).withoutAbbreviation()
    if (expanded.typeArguments.isEmpty()) return expanded
    return expanded.withArguments { projection ->
        projection.type.normalizedDuplicateSupertypeKey(context)
    }
}

/**
 * 将 cone 类型解析为对应 class-like 声明。
 */
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

/**
 * 完整展开 typealias 类型。
 */
private fun ConeCangJieType.fullyExpandTypeAlias(): ConeCangJieType {
    var current = this
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
    }
    return current
}

/**
 * 返回 class-like 声明的类型种类。
 */
private fun CfirClassLikeDeclaration.classKindOrNull(): CfirClassKind? = when (this) {
    is CfirPrimitiveTypeDeclaration -> CfirClassKind.CLASS
    is CfirClass -> CfirClassKind.CLASS
    is CfirInterface -> CfirClassKind.INTERFACE
    is CfirStruct -> CfirClassKind.STRUCT
    is CfirEnum -> CfirClassKind.ENUM
    is CfirTypeAlias -> null
}

/**
 * 返回 class-like 声明的名称。
 */
private fun CfirClassLikeDeclaration.classLikeName(): Name = when (this) {
    is CfirPrimitiveTypeDeclaration -> name
    is CfirClass -> name
    is CfirInterface -> name
    is CfirStruct -> name
    is CfirEnum -> name
    is CfirTypeAlias -> name
}

/**
 * 返回 class-like 声明用于自继承过滤的稳定身份 key。
 */
private fun CfirClassLikeDeclaration.classLikeIdentityKey(): String? = when (this) {
    is CfirPrimitiveTypeDeclaration -> "primitive:${kind.typeName}"
    is CfirClass -> "class:${symbol.classId}"
    is CfirInterface -> "interface:${symbol.classId}"
    is CfirStruct -> "struct:${symbol.classId}"
    is CfirEnum -> "enum:${symbol.classId}"
    is CfirTypeAlias -> null
}

/**
 * 判断声明是否要求显式 open/abstract/sealed 才能被继承。
 */
private fun CfirClassLikeDeclaration.requiresOpenForInheritance(): Boolean = when (this) {
    is CfirPrimitiveTypeDeclaration -> true
    is CfirStruct -> true
    is CfirEnum -> true
    is CfirClass -> !status.isOpen && !status.isAbstract && !status.isSealed
    is CfirInterface -> false
    is CfirTypeAlias -> false
}

/**
 * primitive kind 对应的 ClassId。
 */
private val org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.classId: ClassId
    get() = ClassId.fromString(typeName)
