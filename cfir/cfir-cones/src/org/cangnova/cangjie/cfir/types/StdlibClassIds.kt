package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 标准库（std.core）类型 ClassId 常量。
 *
 * 仅包含标准库中的类/接口/结构体类型，
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
        Comparable, Equatable, Countable, Iterable, Collection, ToString, Future,
        Command, Resumption,
    )
}
