package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 标准库（std.core）类型 ClassId 常量。
 *
 * 包含两组内容：
 *
 * - 标准库中的普通类/接口/结构体名义类型（[allClassIds]）；
 * - 官方 `AST::BuiltInType` 对应的内建类型声明（[builtInClassIds]）——它们由官方编译器注入
 *   `std.core` 并随 `.cjo` 传播，在名字解析层面同属该包。
 *
 * 不包含内建原始类型（Int64, Bool 等由 [PrimitiveTypeKind] 表示）。
 */
object StdlibClassIds {

    /**
     * `std.core` 包名。
     */
    private val core: FqName get() = StandardNames.FqNames.core

    /**
     * `stdx.effect` 包名。
     */
    private val effect: FqName get() = StandardNames.FqNames.effect

    // ---- std.core 核心类型 ----

    /** `std.core.Object`。 */
    @JvmField val Object = ClassId(core, StandardNames.OBJECT)
    /** `std.core.Any`。 */
    @JvmField val Any = ClassId(core, StandardNames.ANY)
    /** `std.core.String`。 */
    @JvmField val String = ClassId(core, StandardNames.STRING)
    /** `std.core.Array`。 */
    @JvmField val Array = ClassId(core, StandardNames.ARRAY)
    /** `std.core.Option`。 */
    @JvmField val Option = ClassId(core, StandardNames.OPTION)
    /** `std.core.Range`。 */
    @JvmField val Range = ClassId(core, StandardNames.RANGE)
    /** `std.core.CType`。 */
    @JvmField val CType = ClassId(core, StandardNames.CTYPE)
    /** `std.core.Exception`。 */
    @JvmField val Exception = ClassId(core, StandardNames.EXCEPTION)
    /** `std.core.Error`。 */
    @JvmField val Error = ClassId(core, StandardNames.ERROR)
    /** `std.core.Resource`。 */
    @JvmField val Resource = ClassId(core, StandardNames.RESOURCE)

    // ---- std.core 核心接口 ----

    /** `std.core.Comparable`。 */
    @JvmField val Comparable = ClassId(core, StandardNames.COMPARABLE)
    /** `std.core.Equatable`。 */
    @JvmField val Equatable = ClassId(core, StandardNames.EQUATABLE)
    /** `std.core.Countable`。 */
    @JvmField val Countable = ClassId(core, StandardNames.COUNTABLE)
    /** `std.core.Iterable`。 */
    @JvmField val Iterable = ClassId(core, StandardNames.ITERABLE)
    /** `std.core.Collection`。 */
    @JvmField val Collection = ClassId(core, StandardNames.COLLECTION)
    /** `std.core.ToString`。 */
    @JvmField val ToString = ClassId(core, StandardNames.TOSTRING)
    /** `std.core.Future`。 */
    @JvmField val Future = ClassId(core, StandardNames.FUTURE)
    /** `std.core.ThreadContext`。 */
    @JvmField val ThreadContext = ClassId(core, StandardNames.THREAD_CONTEXT)

    // ---- std.core 内建类型声明（官方 BuiltInDecl）----

    /*
     * 以下五项对应官方 `AST::BuiltInType` 的全部条目。官方在编译 `std.core` 时通过
     * `AddBuiltIn*Decl` 把它们作为普通全局声明注入 `pkg.files[0]->decls`，随后走正常符号表构建，
     * 并随 `.cjo` 序列化传播（`ASTWriter::SaveBuiltInDecl` / `ASTLoader::LoadBuiltInDecl`）。
     * 因此它们在名字解析层面就是 `std.core` 下的 classifier，与本对象其余条目同属一个包。
     *
     * 这一组与类型层的 [isBuiltin] 谓词是**不同集合**：此处含 `CFunc` 而不含 primitive，
     * `Ty::IsBuiltin()` 则相反。两者用途不同，不要互相替代。
     */

    /**
     * `std.core.RawArray`。
     *
     * 官方 `AddBuiltInArrayDecl` 注入，只带 `GLOBAL | GENERIC` 而**没有** `PUBLIC`，仅 `std.core` 内部可见。
     * 用户代码书写的 `Array<T>` 是标准库 struct（对应 [Array]），并非此类型。
     */
    @JvmField val RawArray = ClassId(core, StandardNames.RAW_ARRAY)

    /** `std.core.VArray`。官方声明只带一个类型参数 `T`，定长 `$N` 属于类型头部而非类型实参。 */
    @JvmField val VArray = ClassId(core, StandardNames.VARRAY)

    /** `std.core.CPointer`。官方声明带类型参数 `T` 及 `T <: CType` 约束。 */
    @JvmField val CPointer = ClassId(core, StandardNames.CPOINTER)

    /** `std.core.CString`。官方声明无类型参数。 */
    @JvmField val CString = ClassId(core, StandardNames.CSTRING)

    /** `std.core.CFunc`。官方声明带一个类型参数 `T`。 */
    @JvmField val CFunc = ClassId(core, StandardNames.CFUNC)

    // ---- stdx.effect effect handlers ----

    /** `stdx.effect.Command`。 */
    @JvmField val Command = ClassId(effect, StandardNames.COMMAND)
    /** `stdx.effect.Resumption`。 */
    @JvmField val Resumption = ClassId(effect, StandardNames.RESUMPTION)

    /**
     * 所有标准库核心类型 ClassId 集合。
     */
    @JvmField
    val allClassIds: Set<ClassId> = setOf(
        Object, Any, String, Array, Option, Range, CType, Exception, Error, Resource,
        Comparable, Equatable, Countable, Iterable, Collection, ToString, Future, ThreadContext,
        Command, Resumption,
    )

    /**
     * 官方 `BuiltInDecl` 对应的内建类型 ClassId 集合。
     *
     * 与 [allClassIds] 分开维护：后者是标准库中的普通名义类型，这里是编译器注入的内建声明，
     * 两者在可见性、成员来源和 extend 索引路径上的处理不同。
     */
    @JvmField
    val builtInClassIds: Set<ClassId> = setOf(
        RawArray, VArray, CPointer, CString, CFunc,
    )
}
