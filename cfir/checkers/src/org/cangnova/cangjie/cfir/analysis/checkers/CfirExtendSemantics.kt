package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty

import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.text

/**
 * extend 语义辅助层。
 *
 * 这里集中维护 extend 相关的跨 checker 语义规则，避免 `Any/CType`、FFI 边界、
 * 不可变目标识别、接口闭包递归和 `super` 关键字判断散落在不同 checker 中。
 *
 * 所有判断均基于 CFIR 节点完成，不依赖 PSI。
 */
internal object CfirExtendSemantics {
    private val anyClassId: ClassId = ClassId.topLevel(StandardNames.FqNames.anyFqName)
    private val cTypeClassId: ClassId = ClassId.topLevel(
        StandardNames.STD_CORE_PACKAGE_FQ_NAME.child(StandardNames.CTYPE),
    )
    private val superReferenceName: Name = Name.special("<super>")

    fun CfirTypeRef.toClassIdOrNull(): ClassId? =
        (this as? CfirResolvedTypeRef)?.coneType?.classIdOrPrimitiveClassId

    /**
     * 判断 extend 目标是否为不可变类型。
     *
     * 官方编译器 `IsImmutableType()` = `(kind >= TYPE_UNIT && kind <= TYPE_FUNC) || IsString() || IsRange()`
     * 在 TypeKind.inc 中 TYPE_ENUM 排在 TYPE_FUNC 之前（是 immutable），
     * TYPE_STRUCT 排在 TYPE_FUNC 之后（不是 immutable）。
     *
     * 因此只有 enum（以及原始类型、String、Range 等内置类型）被视为 immutable，
     * struct 不是 immutable。
     */
    fun isImmutableTarget(coneType: ConeCangJieType): Boolean =
        coneType is ConeEnumType

    /**
     * 判断 extend 目标是否为不可变的非 enum 类型。
     *
     * 官方编译器中 index assignment check 用 `!ed.ty->IsEnum()` 排除 enum，
     * 即 index assignment 限制只对非 enum 的 immutable 类型生效。
     * 由于 struct 不是 immutable，当前没有 class-like 类型满足此条件，
     * 此方法保留给未来可能的原始类型 extend 场景。
     */
    fun isImmutableNonEnumTarget(coneType: ConeCangJieType): Boolean =
        isImmutableTarget(coneType) && coneType !is ConeEnumType

    fun isProtectedInterface(classId: ClassId?): Boolean =
        classId == anyClassId || classId == cTypeClassId

    fun isSuperReference(reference: CfirReference): Boolean {
        if (reference is CfirSuperReference) return true
        val namedReference = reference as? CfirNamedReference ?: return false
        return namedReference.name == superReferenceName
    }

    fun resolveDeclaration(context: CheckerContext, classId: ClassId): CfirClassLikeDeclaration? {
        return context.session.cfirProvider.getCfirClassifierByFqName(classId)
            ?: context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
    }

    fun targetDeclaration(context: CheckerContext, extend: CfirExtend): CfirClassLikeDeclaration? {
        val targetClassId = extend.extendedTypeRef.toClassIdOrNull() ?: return null
        return resolveDeclaration(context, targetClassId)
    }

    fun findMutPropertyLeak(context: CheckerContext, interfaceClassId: ClassId): MutPropertyLeak? {
        return findMutPropertyLeak(context, interfaceClassId, linkedSetOf())
    }

    /**
     * 判断 CFIR 声明是否标记了 FFI 互操作注解（@C / @Java）。
     *
     * TODO: 注解系统尚未完整实现，当前仅通过 CFIR 注解的 typeRef 做 ClassId 级别的判断。
     *       等注解系统完善后需要补充对内置注解的完整支持。
     */
    fun isForeignInteropBoundary(declaration: CfirClassLikeDeclaration): Boolean {
        return declaration.annotations.any { annotation ->
            val annotationClassId = annotation.typeRef.toClassIdOrNull()
            annotationClassId?.shortClassName in ffiBoundaryAnnotationNames ||
                annotation.source.annotationShortNameOrNull() in ffiBoundaryAnnotationNames
        } || declaration.source.containsFfiBoundaryAnnotation()
    }

    /**
     * 判断 extend 目标是否落在 FFI 边界上。
     *
     * 通过 CFIR provider 和 symbol provider 解析目标声明，然后检查其注解。
     *
     * TODO: 注解系统尚未完整实现，等完善后需要增强 FFI 边界判断逻辑。
     */
    fun isForeignInteropBoundaryTarget(context: CheckerContext, extend: CfirExtend): Boolean {
        val targetDeclaration = targetDeclaration(context, extend) ?: return false
        return isForeignInteropBoundary(targetDeclaration)
    }

    /**
     * 判断 extend 目标是否属于当前包。
     *
     * orphan rule 的本质是限制"跨包为外部类型引入全新外部接口"。
     * 优先用 ClassId 的包名做判断；若不可靠，再查 CFIR 声明 symbol 的 classId。
     *
     * 当无法解析目标声明时，保守起见认为目标在当前包中，
     * 避免因 provider 查找失败导致的 orphan rule 误报。
     */
    fun isTargetDeclaredInPackage(
        context: CheckerContext,
        extend: CfirExtend,
        currentPackage: FqName,
    ): Boolean {
        val targetClassId = extend.extendedTypeRef.toClassIdOrNull()
        if (targetClassId?.packageFqName == currentPackage) {
            return true
        }

        // 同文件内的声明可能 ClassId 包名尚不一致，通过 provider 确认
        val targetDeclaration = targetDeclaration(context, extend)
        if (targetDeclaration == null) {
            // 无法解析目标声明 → 保守认定为本包声明，避免误报
            return true
        }
        return targetDeclaration.symbol.classId.packageFqName == currentPackage
    }

    private fun findMutPropertyLeak(
        context: CheckerContext,
        interfaceClassId: ClassId,
        visited: MutableSet<ClassId>,
    ): MutPropertyLeak? {
        if (!visited.add(interfaceClassId)) return null

        val declaration = resolveDeclaration(context, interfaceClassId) as? CfirInterface ?: return null
        declaration.declarations
            .asSequence()
            .filterIsInstance<CfirProperty>()
            .firstOrNull { property -> property.status.isMut }
            ?.let { property ->
            return MutPropertyLeak(interfaceClassId, property.name)
        }

        for (superTypeRef in declaration.superTypeRefs) {
            val superClassId = superTypeRef.toClassIdOrNull() ?: continue
            val leak = findMutPropertyLeak(context, superClassId, visited)
            if (leak != null) {
                return leak
            }
        }

        return null
    }

    private val ffiBoundaryAnnotationNames: Set<Name> = setOf(
        Name.identifier("C"),
        Name.identifier("Java"),
    )

    private fun org.cangnova.cangjie.source.CjSourceElement?.annotationShortNameOrNull(): Name? {
        val rawText = this?.text?.toString()?.trim().orEmpty()
        if (!rawText.startsWith("@")) return null

        val shortName = rawText
            .removePrefix("@")
            .substringBefore('(')
            .substringAfterLast('.')
            .trim()
        return Name.identifierIfValid(shortName)
    }

    private fun org.cangnova.cangjie.source.CjSourceElement?.containsFfiBoundaryAnnotation(): Boolean {
        val rawText = this?.text?.toString().orEmpty()
        return ffiBoundaryAnnotationNames.any { annotationName ->
            rawText.contains("@${annotationName.asString()}")
        }
    }

    private fun CfirClassLikeDeclaration.classKindOrNull(): CfirClassKind? = when (this) {
        is CfirPrimitiveTypeDeclaration -> CfirClassKind.CLASS
        is CfirClass -> CfirClassKind.CLASS
        is CfirInterface -> CfirClassKind.INTERFACE
        is CfirStruct -> CfirClassKind.STRUCT
        is CfirEnum -> CfirClassKind.ENUM
        else -> null
    }

    private fun CfirClassLikeDeclaration.superTypeRefsOrEmpty(): List<CfirTypeRef> = when (this) {
        is CfirClass -> superTypeRefs
        is CfirInterface -> superTypeRefs
        is CfirStruct -> superTypeRefs
        is CfirEnum -> superTypeRefs
        else -> emptyList()
    }
}

internal data class MutPropertyLeak(
    val interfaceClassId: ClassId,
    val propertyName: Name,
)
