package org.cangjie.cfir.types

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

    // ---- std.core 核心类型 ----

    @JvmField val Object = ClassId(core, StandardNames.OBJECT)
    @JvmField val Any = ClassId(core, StandardNames.ANY)
    @JvmField val String = ClassId(core, StandardNames.STRING)
    @JvmField val Array = ClassId(core, StandardNames.ARRAY)
    @JvmField val Option = ClassId(core, StandardNames.OPTION)
    @JvmField val Range = ClassId(core, StandardNames.RANGE)
    @JvmField val Exception = ClassId(core, StandardNames.EXCEPTION)
    @JvmField val Resource = ClassId(core, StandardNames.RESOURCE)

    // ---- std.core 核心接口 ----

    @JvmField val Comparable = ClassId(core, StandardNames.COMPARABLE)
    @JvmField val Equatable = ClassId(core, StandardNames.EQUATABLE)
    @JvmField val Countable = ClassId(core, StandardNames.COUNTABLE)
    @JvmField val Iterable = ClassId(core, StandardNames.ITERABLE)
    @JvmField val ToString = ClassId(core, StandardNames.TOSTRING)
    @JvmField val Future = ClassId(core, StandardNames.FUTURE)

    /** 所有标准库核心类型 ClassId 集合 */
    @JvmField
    val allClassIds: Set<ClassId> = setOf(
        Object, Any, String, Array, Option, Range, Exception, Resource,
        Comparable, Equatable, Countable, Iterable, ToString, Future,
    )
}
