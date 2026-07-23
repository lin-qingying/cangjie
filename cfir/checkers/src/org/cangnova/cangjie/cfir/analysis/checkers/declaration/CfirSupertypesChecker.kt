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
import org.cangnova.cangjie.cfir.resolve.providers.DeclaredSupertypeClassification
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.declaredSupertypeClassifierSource
import org.cangnova.cangjie.cfir.resolve.providers.independentNominalCheckTypeOrNull
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeRecoverableNominalDiagnostic
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.withArguments
import org.cangnova.cangjie.cfir.types.withoutAbbreviation
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

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
                checkConcreteSupertypeRules(declaration)
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

            // 外层 nominal 声明只负责提供父类型种类；完整类型树负责判定父类型是否有效。
            // 例如 I<V> 即使能够解析到 interface I，未解析的 V 仍使整个继承类型无效。
            if (
                !superType.completeType.contains { it is ConeErrorType } &&
                superType.nominalType.toResolvedSuperDeclaration(context)?.classKindOrNull() == CfirClassKind.INTERFACE
            ) {
                continue
            }

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS,
                a = ownerName,
                b = superType.nominalType.supertypeDiagnosticName(context),
            )
        }
    }

    /**
     * 检查 class/struct/enum 的 concrete 父类型顺序、多继承和可继承性。
     *
     * 官方 `CheckAndAddSubDecls` 逐个父类型维护两个状态：
     * - 是否已经出现 concrete 父 class；
     * - 当前父类型在继承列表中的顺序。
     * 这能区分“第二个父 class”和“父 class 放在接口之后”两类诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkConcreteSupertypeRules(declaration: CfirClassLikeDeclaration) {
        val ownerKey = declaration.classLikeIdentityKey()
        var hasConcreteSuperClass = false
        var superClassLikeCount = 0

        for (superTypeRef in declaration.superTypeRefs) {
            val classification = superTypeRef.classifyDeclaredSupertype(context.session)
            when (classification) {
                is DeclaredSupertypeClassification.InvalidTargetKind -> {
                    reporter.reportOn(
                        source = superTypeRef.declaredSupertypeClassifierSource(),
                        factory = CfirErrors.CLASS_INHERIT_NON_CLASS_NOR_INTERFACE,
                        a = declaration.classLikeName(),
                    )
                    continue
                }

                is DeclaredSupertypeClassification.PrimaryResolutionError -> continue

                is DeclaredSupertypeClassification.ValidNominal,
                is DeclaredSupertypeClassification.RecoverableNominalError,
                is DeclaredSupertypeClassification.LoopError -> Unit
            }
            val nominalType = classification.independentNominalCheckTypeOrNull() ?: continue
            val superDeclaration = nominalType.toResolvedSuperDeclaration(context) ?: continue
            val supertypeSource = superTypeRef.declaredSupertypeClassifierSource()
            if (superDeclaration.classKindOrNull() == CfirClassKind.INTERFACE) {
                superClassLikeCount++
                continue
            }

            if (hasConcreteSuperClass) {
                reporter.reportOn(
                    source = supertypeSource,
                    factory = CfirErrors.ILLEGAL_MULTI_INHERITANCE,
                    a = declaration.classLikeName(),
                )
            } else if (superClassLikeCount != 0) {
                reporter.reportOn(
                    source = supertypeSource,
                    factory = CfirErrors.SUPERCLASS_MUST_BE_PLACED_AT_FIRST,
                    a = superDeclaration.classLikeName(),
                )
            }

            if (
                (ownerKey == null || ownerKey != superDeclaration.classLikeIdentityKey()) &&
                superDeclaration.requiresOpenForInheritance()
            ) {
                reporter.reportOn(
                    source = supertypeSource,
                    factory = CfirErrors.NON_INHERITABLE_SUPER_CLASS,
                    a = superDeclaration.classLikeName(),
                )
            }

            hasConcreteSuperClass = true
            superClassLikeCount++
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
 * 父类型引用解析结果。
 *
 * @property declaration 父类型引用对应的 class-like 声明。
 * @property source 触发父类型诊断的源码位置，错误类型场景下取其委托的原始 typeRef。
 */
private data class ResolvedSupertypeDeclaration(
    /**
     * 父类型引用对应的 class-like 声明。
     */
    val declaration: CfirClassLikeDeclaration,

    /**
     * 触发父类型诊断的源码位置。
     */
    val source: CjSourceElement?,
)

/**
 * interface 父类型合法性检查所需的两个类型视图。
 *
 * [completeType] 保留完整类型实参树，用于识别嵌套错误类型；[nominalType] 保留可解析的
 * 外层声明，用于在完整类型有效时判断父类型究竟是 interface 还是 concrete 类型。
 */
private data class InterfaceSupertypeLegality(
    val completeType: ConeCangJieType,
    val nominalType: ConeCangJieType,
)

/**
 * 将父类型引用解析为 class-like 声明。
 */
private fun CfirTypeRef.toResolvedSuperDeclaration(context: CheckerContext): CfirClassLikeDeclaration? =
    toResolvedSuperDeclarationWithSource(context)?.declaration

/**
 * 将父类型引用解析为 class-like 声明，并保留 resolve 阶段错误类型背后的原始父类型。
 */
private fun CfirTypeRef.toResolvedSuperDeclarationWithSource(
    context: CheckerContext,
): ResolvedSupertypeDeclaration? =
    toResolvedSuperDeclarationWithSource(context, seenTypeRefs = linkedSetOf())

/**
 * 将父类型引用解析为 class-like 声明，并保留 resolve 阶段错误类型背后的原始父类型。
 */
private fun CfirTypeRef.toResolvedSuperDeclarationWithSource(
    context: CheckerContext,
    seenTypeRefs: MutableSet<CfirTypeRef>,
): ResolvedSupertypeDeclaration? {
    if (!seenTypeRefs.add(this)) return null
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    resolvedTypeRef.coneType.effectiveNominalSupertypeOrNull()?.toResolvedSuperDeclaration(context)?.let {
        return ResolvedSupertypeDeclaration(it, source)
    }
    return resolvedTypeRef.delegatedTypeRef?.toResolvedSuperDeclarationWithSource(context, seenTypeRefs)
}

/**
 * interface 直接父类型必须是完整有效的单一 nominal interface。
 *
 * resolve 阶段可能在完整类型无效时仍保留可解析的外层 nominal 声明。检查器不能丢弃
 * 完整类型树，否则 I<ErrorType> 会被误判为合法 interface 父类型。
 */
private fun CfirTypeRef.toResolvedSupertypeForInterfaceLegality(): InterfaceSupertypeLegality? {
    val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return null
    val completeType = resolvedTypeRef.coneType
    return InterfaceSupertypeLegality(
        completeType = completeType,
        nominalType = completeType.effectiveNominalSupertypeOrNull() ?: completeType,
    )
}

private fun ConeCangJieType.effectiveNominalSupertypeOrNull(): ConeCangJieType? =
    if (this is ConeErrorType) {
        if (diagnostic is ConeRecoverableNominalDiagnostic) delegatedType else null
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
