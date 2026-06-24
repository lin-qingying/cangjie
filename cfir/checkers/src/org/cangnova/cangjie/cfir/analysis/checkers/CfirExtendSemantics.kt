/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
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
     * 因此原始类型、tuple、enum、function、String、Range 被视为 immutable；
     * 普通 struct 不是 immutable。
     */
    fun isImmutableTarget(coneType: ConeCangJieType): Boolean = when (coneType) {
        is ConePrimitiveType,
        is ConeTupleType,
        is ConeEnumType,
        is ConeFunctionType -> true

        is ConeTypeAliasType -> coneType.expandedType?.let(::isImmutableTarget) ?: false
        else -> coneType.classIdOrPrimitiveClassId in immutableStructLikeClassIds
    }

    /**
     * 判断 extend 目标是否为不可变的非 enum 类型。
     *
     * 官方编译器中 index assignment check 用 `!ed.ty->IsEnum()` 排除 enum，
     * 即 index assignment 限制只对非 enum 的 immutable 类型生效。
     */
    fun isImmutableNonEnumTarget(coneType: ConeCangJieType): Boolean =
        isImmutableTarget(coneType) && !coneType.isEnumTarget()

    private val immutableStructLikeClassIds: Set<ClassId> = setOf(
        StdlibClassIds.String,
        StdlibClassIds.Range,
    )

    private fun ConeCangJieType.isEnumTarget(): Boolean = when (this) {
        is ConeEnumType -> true
        is ConeTypeAliasType -> expandedType?.isEnumTarget() == true
        else -> false
    }

    fun isProtectedInterface(classId: ClassId?): Boolean =
        classId == anyClassId || classId == cTypeClassId

    fun isCType(classId: ClassId?): Boolean =
        classId == cTypeClassId

    /**
     * 标准库以 builtin extend 形式为原始类型提供的隐式接口。
     *
     * 这些接口不是原始类型合成声明的普通 `superTypeRefs`，但官方
     * `TypeManager::GetAllExtendInterfaceTy` 会把它们纳入继承/重复接口判断。
     */
    fun implicitPrimitiveInterfaceClassIds(
        context: CheckerContext,
        targetType: ConeCangJieType,
    ): Set<ClassId> {
        val semanticType = targetType.fullyExpandedType(context.session)
        return when (semanticType) {
            is ConePrimitiveType -> primitiveImplicitInterfaceClassIds
            else -> emptySet()
        }
    }

    private val primitiveImplicitInterfaceClassIds: Set<ClassId> = setOf(
        StdlibClassIds.ToString,
    )

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
     * 官方 `CheckImmutExtendInhertMutSuper` 在 immutable extend 继承含 mut 属性接口时，
     * 以整个 extend 声明报告 `sema_interface_is_not_extendable`，并把该错误作为该
     * supertype 的主诊断处理。
     */
    fun immutableMutInterfaceLeak(context: CheckerContext, extend: CfirExtend, superTypeRef: CfirTypeRef): MutPropertyLeak? {
        val targetType = (extend.extendedTypeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.fullyExpandedType(context.session)
            ?: return null
        if (!isImmutableTarget(targetType)) return null

        val interfaceClassId = superTypeRef.toClassIdOrNull() ?: return null
        return findMutPropertyLeak(context, interfaceClassId)
    }

    fun isInsideImmutableMutInterfaceSupertype(
        context: CheckerContext,
        extend: CfirExtend,
        typeRef: CfirTypeRef,
    ): Boolean {
        val source = typeRef.source ?: return false
        if (isSourceInsideImmutableMutInterfaceExtendHeader(context, source)) return true
        return isSourceInsideImmutableMutInterfaceSupertype(context, extend, source)
    }

    fun isSourceInsideImmutableMutInterfaceExtendHeader(context: CheckerContext, source: CjSourceElement): Boolean {
        val stackExtends = context.containingDeclarations.asSequence().filterIsInstance<CfirExtend>()
        val fileExtends = context.containingFileSymbol
            ?.cfir
            ?.declarations
            ?.asSequence()
            ?.filterIsInstance<CfirExtend>()
            ?: emptySequence()

        return (stackExtends + fileExtends).distinct().any { extend ->
            extend.headerSourceContains(source) &&
                extend.superTypeRefs.any { superTypeRef -> immutableMutInterfaceLeak(context, extend, superTypeRef) != null }
        }
    }

    fun isSourceInsideImmutableMutInterfaceSupertype(
        context: CheckerContext,
        extend: CfirExtend,
        source: CjSourceElement,
    ): Boolean {
        return extend.superTypeRefs.any { superTypeRef ->
            immutableMutInterfaceLeak(context, extend, superTypeRef) != null &&
                (context.containingElements.any { it === superTypeRef } || superTypeRef.source.contains(source))
        }
    }

    fun hasAnnotation(declaration: CfirClassLikeDeclaration, annotationName: Name): Boolean {
        return declaration.annotations.any { annotation ->
            val annotationClassId = annotation.typeRef.toClassIdOrNull()
            annotationClassId?.shortClassName == annotationName ||
                annotation.source.annotationShortNameOrNull() == annotationName
        } || declaration.source.containsAnnotation(annotationName)
    }

    /**
     * 判断 CFIR 声明是否标记了 FFI 互操作注解（@C / @Java）。
     *
     * TODO: 注解系统尚未完整实现，当前仅通过 CFIR 注解的 typeRef 做 ClassId 级别的判断。
     *       等注解系统完善后需要补充对内置注解的完整支持。
     */
    fun isForeignInteropBoundary(declaration: CfirClassLikeDeclaration): Boolean {
        return ffiBoundaryAnnotationNames.any { annotationName -> hasAnnotation(declaration, annotationName) }
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

    private val ffiBoundaryAnnotationNames: Set<Name> = setOf(Name.identifier("C"))

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

    private fun org.cangnova.cangjie.source.CjSourceElement?.containsAnnotation(annotationName: Name): Boolean {
        val rawText = this?.text?.toString().orEmpty()
        return rawText.contains("@${annotationName.asString()}")
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

    private fun CjSourceElement?.contains(other: CjSourceElement): Boolean {
        this ?: return false
        return startOffset <= other.startOffset && other.endOffset <= endOffset
    }

    private fun CfirExtend.headerSourceContains(source: CjSourceElement): Boolean {
        val extendSource = this.source ?: return false
        if (!extendSource.contains(source)) return false
        val bodyStartOffset = extendSource.text
            ?.toString()
            ?.indexOf('{')
            ?.takeIf { it >= 0 }
            ?.let { extendSource.startOffset + it }
            ?: extendSource.endOffset
        return source.endOffset <= bodyStartOffset
    }
}

internal data class MutPropertyLeak(
    val interfaceClassId: ClassId,
    val propertyName: Name,
)
