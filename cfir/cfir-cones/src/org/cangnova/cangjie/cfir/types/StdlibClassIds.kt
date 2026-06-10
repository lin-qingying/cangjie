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

    private val core: FqName get() = StandardNames.FqNames.core
    private val effect: FqName get() = StandardNames.FqNames.effect

    // ---- std.core 核心类型 ----

    @JvmField val Object = ClassId(core, StandardNames.OBJECT)
    @JvmField val Any = ClassId(core, StandardNames.ANY)
    @JvmField val String = ClassId(core, StandardNames.STRING)
    @JvmField val Array = ClassId(core, StandardNames.ARRAY)
    @JvmField val Option = ClassId(core, StandardNames.OPTION)
    @JvmField val Range = ClassId(core, StandardNames.RANGE)
    @JvmField val Exception = ClassId(core, StandardNames.EXCEPTION)
    @JvmField val Error = ClassId(core, StandardNames.ERROR)
    @JvmField val Resource = ClassId(core, StandardNames.RESOURCE)

    // ---- std.core 核心接口 ----

    @JvmField val Comparable = ClassId(core, StandardNames.COMPARABLE)
    @JvmField val Equatable = ClassId(core, StandardNames.EQUATABLE)
    @JvmField val Countable = ClassId(core, StandardNames.COUNTABLE)
    @JvmField val Iterable = ClassId(core, StandardNames.ITERABLE)
    @JvmField val Collection = ClassId(core, StandardNames.COLLECTION)
    @JvmField val ToString = ClassId(core, StandardNames.TOSTRING)
    @JvmField val Future = ClassId(core, StandardNames.FUTURE)

    // ---- stdx.effect effect handlers ----

    @JvmField val Command = ClassId(effect, StandardNames.COMMAND)
    @JvmField val Resumption = ClassId(effect, StandardNames.RESUMPTION)

    /** 所有标准库核心类型 ClassId 集合 */
    @JvmField
    val allClassIds: Set<ClassId> = setOf(
        Object, Any, String, Array, Option, Range, Exception, Error, Resource,
        Comparable, Equatable, Countable, Iterable, Collection, ToString, Future,
        Command, Resumption,
    )
}
