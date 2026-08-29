package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.name.ClassId

/**
 * 内建类型声明的种类，对齐官方 `AST::BuiltInType`。
 *
 * 这是**声明层**的内建类型集合：官方在编译 `std.core` 时通过 `AddBuiltIn*Decl` 把这五项
 * 作为普通全局声明注入包内，随后走正常符号表构建，并随 `.cjo` 序列化传播。它解决的是
 * "名字要能被解析、能带泛型参数与约束、能被 typealias 引用、能跨包传播"。
 *
 * 与类型层的 `ConeCangJieType.isBuiltin`（对齐 `Ty::IsBuiltin()`）是**不同集合**：
 * 此处含 [CFUNC] 而不含 primitive，类型层则相反。两者用途不同，不要互相替代。
 *
 * 枚举顺序与 `ModuleFormat.fbs` 的 `enum BuiltInType : uint8 { Array, VArray, CPointer, CString, CFunc }`
 * 一致，而非官方 C++ 头文件中 `enum class BuiltInType` 的声明顺序（后者为
 * `ARRAY, POINTER, CSTRING, CFUNC, VARRAY`）。`.cjo` 反序列化以 FlatBuffers 顺序为准。
 *
 * @property classId 该内建声明在 `std.core` 下的稳定 class id。
 * @property typeParameterCount 官方声明携带的类型参数个数。
 */
enum class CfirBuiltInTypeKind(
    /**
     * 该内建声明在 `std.core` 下的稳定 class id。
     */
    val classId: ClassId,
    /**
     * 官方声明携带的类型参数个数。
     */
    val typeParameterCount: Int,
) {
    /**
     * `RawArray<T>`。
     *
     * 官方 `AddBuiltInArrayDecl` 注入，属性只有 `GLOBAL | GENERIC`，**没有** `PUBLIC`，
     * 因此仅 `std.core` 内部可见。用户代码书写的 `Array<T>` 是标准库 struct，并非此类型。
     */
    ARRAY(StdlibClassIds.RawArray, typeParameterCount = 1),

    /**
     * `VArray<T, $N>`。
     *
     * 官方 `AddBuiltInVArrayDecl` 注入，声明只带一个类型参数 `T`；
     * 定长 `$N` 属于类型头部（官方 `VArrayTy::size` 字段），不是类型实参。
     */
    VARRAY(StdlibClassIds.VArray, typeParameterCount = 1),

    /**
     * `CPointer<T>`。
     *
     * 官方 `AddBuiltInPointerDecl` 注入，除类型参数 `T` 外还带 `T <: CType` 约束
     * （`CreateConstraintForFFI(CTYPE_NAME)`）。
     */
    CPOINTER(StdlibClassIds.CPointer, typeParameterCount = 1),

    /**
     * `CString`。
     *
     * 官方 `AddBuiltInCStringDecl` 注入，`generic` 显式置空，无类型参数。
     */
    CSTRING(StdlibClassIds.CString, typeParameterCount = 0),

    /**
     * `CFunc<T>`。
     *
     * 官方 `AddBuiltinCFuncDecl` 注入。注意它虽在声明层是内建类型，
     * 但其类型 `CFuncTy` 属于 `FuncTy`，不在 `Ty::IsBuiltin()` 集合内。
     */
    CFUNC(StdlibClassIds.CFunc, typeParameterCount = 1),
    ;

    /**
     * 该内建声明的短名称。
     */
    val typeName: String get() = classId.shortClassName.asString()

    /**
     * 内建类型种类的查表入口。
     */
    companion object {
        /**
         * 按 [ClassId] 反查内建类型种类，非内建声明返回 `null`。
         */
        fun fromClassIdOrNull(classId: ClassId): CfirBuiltInTypeKind? =
            entries.firstOrNull { it.classId == classId }
    }
}
