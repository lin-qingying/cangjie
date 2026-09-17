package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.resolvedInteropInfoOrNull
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Objective-C 类型兼容性语义的唯一 owner。
 *
 * `ObjCPointer`、`ObjCFunc` 和 `ObjCBlock` 都是 `objc.lang` 中的库声明，不能
 * 通过短名称或前缀判断。该对象只消费已经解析的 Cone 类型和 ClassId；声明级
 * checker 负责选择适用的诊断，不在这里重复实现目标/作用域规则。
 */
public object CfirObjCTypeSemantics {
    private val objcLang = FqName("objc.lang")

    /** `objc.lang.ObjCPointer` 的稳定 ClassId。 */
    public val objCPointerClassId: ClassId = ClassId(objcLang, Name.identifier("ObjCPointer"))

    /** `objc.lang.ObjCFunc` 的稳定 ClassId。 */
    public val objCFuncClassId: ClassId = ClassId(objcLang, Name.identifier("ObjCFunc"))

    /** `objc.lang.ObjCBlock` 的稳定 ClassId。 */
    public val objCBlockClassId: ClassId = ClassId(objcLang, Name.identifier("ObjCBlock"))

    /** `objc.lang.ObjCId` 的稳定 ClassId。 */
    public val objCIdClassId: ClassId = ClassId(objcLang, Name.identifier("ObjCId"))

    /** Objective-C ABI 支持的基础值类型。String/Rune 不属于该集合。 */
    private val primitiveTypes = setOf(
        PrimitiveTypeKind.UNIT,
        PrimitiveTypeKind.BOOLEAN,
        PrimitiveTypeKind.INT8,
        PrimitiveTypeKind.UINT8,
        PrimitiveTypeKind.INT16,
        PrimitiveTypeKind.UINT16,
        PrimitiveTypeKind.INT32,
        PrimitiveTypeKind.UINT32,
        PrimitiveTypeKind.INT64,
        PrimitiveTypeKind.UINT64,
        PrimitiveTypeKind.INT_NATIVE,
        PrimitiveTypeKind.UINT_NATIVE,
        PrimitiveTypeKind.FLOAT32,
        PrimitiveTypeKind.FLOAT64,
    )

    /** 判断一个值类型能否出现在 Objective-C mirror/impl 的普通签名中。 */
    public fun isObjCCompatible(session: CfirSession, type: ConeCangJieType): Boolean =
        isObjCCompatible(session, type.fullyExpandedType(session), linkedSetOf())

    /** 判断 `ObjCFunc<F>`/`ObjCBlock<F>` 的 F 是否为合法 Objective-C 函数签名。 */
    public fun isObjCFunctionSignature(session: CfirSession, type: ConeCangJieType): Boolean {
        val expanded = type.fullyExpandedType(session)
        if (expanded is ConeErrorType) return true
        if (expanded !is ConeFunctionType) return false
        // ObjCFunc/ObjCBlock wrap an Objective-C function signature, not a C
        // function signature. The ABI bit is part of the resolved function
        // type and must not be inferred from the wrapper class name.
        if (expanded.isCFunc) return false
        return expanded.parameterTypes.all { isObjCCompatible(session, it) } &&
            isObjCCompatible(session, expanded.returnType)
    }

    /** 判断 `ObjCPointer<T>` 的 T 是否符合指针专用规则。 */
    public fun isObjCPointerPointee(session: CfirSession, type: ConeCangJieType): Boolean =
        isObjCCompatible(session, type.fullyExpandedType(session))

    /**
     * 判断 `ObjCPointer<T>` 是否直接指向 Objective-C mirror class。
     *
     * ARC 规则禁止这种类型出现在任何仓颉方法或属性的返回类型中；这是返回位置的
     * 额外约束，不应混入普通 `ObjCPointer<T>` 参数兼容性判断。
     */
    public fun isObjCPointerToClass(session: CfirSession, type: ConeCangJieType): Boolean {
        val pointer = type.fullyExpandedType(session)
        if (pointer.classIdOrPrimitiveClassId != objCPointerClassId) return false
        val pointee = pointer.typeArguments.singleOrNull()?.type?.fullyExpandedType(session)
        val pointeeClassLike = pointee as? ConeClassLikeType ?: return false
        val declaration = session.symbolProvider
            .getClassLikeSymbolByClassId(pointeeClassLike.classId)
            ?.cfir
        return declaration is CfirClass &&
            isAnnotatedObjCDeclaration(session, pointeeClassLike.classId, linkedSetOf())
    }

    private fun isObjCCompatible(
        session: CfirSession,
        type: ConeCangJieType,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean = when (type) {
        is ConeErrorType -> true
        is ConePrimitiveType -> type.kind in primitiveTypes
        is ConeClassLikeType,
        is ConeStructType,
        -> {
            val classId = type.classIdOrPrimitiveClassId ?: return false
            when (classId) {
            StdlibClassIds.Option -> {
                val argument = type.typeArguments.singleOrNull()?.type ?: return false
                isNullableObjCReference(session, argument, activeClassIds)
            }

            objCPointerClassId -> {
                val pointee = type.typeArguments.singleOrNull()?.type ?: return false
                isObjCCompatible(session, pointee, activeClassIds)
            }

            objCFuncClassId, objCBlockClassId -> {
                val function = type.typeArguments.singleOrNull()?.type ?: return false
                isObjCFunctionSignature(session, function, activeClassIds)
            }

            objCIdClassId -> true
            else -> if (type is ConeStructType) {
                isCStruct(session, classId, activeClassIds)
            } else {
                isAnnotatedObjCDeclaration(session, classId, activeClassIds)
            }
            }
        }
        else -> false
    }

    private fun isNullableObjCReference(
        session: CfirSession,
        type: ConeCangJieType,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean {
        val expanded = type.fullyExpandedType(session)
        val classId = (expanded as? ConeClassLikeType)?.classId ?: return false
        if (classId == objCIdClassId) return true
        return isAnnotatedObjCReference(session, classId, activeClassIds)
    }

    private fun isObjCFunctionSignature(
        session: CfirSession,
        type: ConeCangJieType,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean {
        val expanded = type.fullyExpandedType(session) as? ConeFunctionType ?: return false
        if (expanded.isCFunc) return false
        return expanded.parameterTypes.all { isObjCCompatible(session, it, activeClassIds) } &&
            isObjCCompatible(session, expanded.returnType, activeClassIds)
    }

    private fun isAnnotatedObjCDeclaration(
        session: CfirSession,
        classId: ClassId,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean {
        if (!activeClassIds.add(classId)) return false
        return try {
            val declaration = session.symbolProvider
                .getClassLikeSymbolByClassId(classId)
                ?.cfir as? CfirClassLikeDeclaration
                ?: return false
            declaration.resolvedInteropInfoOrNull()?.objc?.let { info ->
                info.isMirror || info.isImpl
            } == true
        } finally {
            activeClassIds.remove(classId)
        }
    }

    private fun isAnnotatedObjCReference(
        session: CfirSession,
        classId: ClassId,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean = isAnnotatedObjCDeclaration(session, classId, activeClassIds)

    /** Objective-C compatible C struct is a value-layout type, not an annotated class. */
    private fun isCStruct(
        session: CfirSession,
        classId: ClassId,
        activeClassIds: MutableSet<ClassId>,
    ): Boolean {
        if (!activeClassIds.add(classId)) return false
        return try {
            val declaration = session.symbolProvider
                .getClassLikeSymbolByClassId(classId)
                ?.cfir as? CfirStruct
                ?: return false
            declaration.status.isC
        } finally {
            activeClassIds.remove(classId)
        }
    }

}
