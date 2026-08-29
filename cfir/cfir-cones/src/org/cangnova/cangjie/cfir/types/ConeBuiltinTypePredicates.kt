package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.ClassId

/**
 * 类型层的内建类型谓词。
 *
 * 仓颉官方在内建类型上区分两个层次，二者成员集合**不同**，不能合并：
 *
 * - **类型层**：`Ty::IsBuiltin()` 与 `Ty::IsExtendable()`，即本文件。含 primitive，不含 `CFunc`。
 *   用途是"该类型没有关联的 `Decl` 指针，extend 查询必须走 `TypeManager::builtinTyToExtendMap`
 *   而非 `declToExtendMap`"，以及装箱、子类型短路等类型层判定。
 * - **声明层**：`AST::BuiltInType`，即 `{Array, VArray, CPointer, CString, CFunc}`。不含 primitive，含 `CFunc`。
 *   用途是"该名字要能被解析、能带泛型参数与约束、能被 typealias 引用、能随 `.cjo` 传播"，
 *   官方通过 `AddBuiltIn*Decl` 把它们注入 `std.core` 后走正常符号表。
 *
 * 之所以两个集合不同：`CFunc` 需要名字解析但 `CFuncTy` 是 `FuncTy` 的一种、不需要第二张 extend 表；
 * primitive 需要类型层谓词但在官方是 lexer 关键字、没有声明。
 */

/**
 * 当前类型是否为官方语义下的内建类型。
 *
 * 严格对齐官方 `Ty::IsBuiltin()`：
 *
 * ```cpp
 * return (kind >= TypeKind::TYPE_UNIT && kind <= TypeKind::TYPE_BOOLEAN) || kind == TypeKind::TYPE_ARRAY ||
 *     kind == TypeKind::TYPE_POINTER || kind == TypeKind::TYPE_CSTRING || kind == TypeKind::TYPE_VARRAY;
 * ```
 *
 * 其中 `[TYPE_UNIT, TYPE_BOOLEAN]` 即 `Ty::IsPrimitive()` 的范围，覆盖 `Unit` / 各整数与浮点 /
 * `IdealInt` / `IdealFloat` / `Rune` / `Nothing` / `Bool`，与 [PrimitiveTypeKind] 的条目一一对应。
 *
 * 两点与官方的表示差异：
 *
 * - 官方 `TYPE_ARRAY` 指内建的 `RawArray`（`Ty::IsArray()`），与标准库 struct `Array<T>`
 *   （`Ty::IsStructArray()`）是并存的两种表示。当前 RawArray 尚未有独立的 Cone 节点，
 *   但其保留的 `std.core.RawArray` classifier identity 仍必须被识别；标准库 `Array<T>`
 *   的 `ConeStructType` 不会命中该分支。
 * - 官方 `CFunc` 不在 `IsBuiltin()` 内（它是 `FuncTy` 的一种），这里同样不含。
 *
 * 该谓词不展开 typealias，与官方 `IsBuiltin()` 直接判 `kind` 的行为一致；
 * 调用方若需要按展开后的真实类型判定，应先调用 `fullyExpandedType`。
 */
val ConeCangJieType.isBuiltin: Boolean
    get() = when (this) {
        // TYPE_UNIT..TYPE_BOOLEAN：Ty::IsPrimitive() 的全部范围。
        is ConePrimitiveType -> true
        // IdealInt / IdealFloat 在推断阶段的承载形式，对应同一段 primitive kind 区间。
        is ConeIdealLiteralType -> true
        // TYPE_POINTER
        is ConePointerType -> true
        // TYPE_CSTRING
        is ConeCStringType -> true
        // TYPE_VARRAY
        is ConeVArrayType -> true
        // TYPE_ARRAY；RawArray 的 classifier identity 是当前过渡表示中的唯一可靠标记。
        is ConeClassLikeType -> classId == StdlibClassIds.RawArray
        else -> false
    }

/**
 * 当前类型是否可以作为 `extend` 的目标。
 *
 * 严格对齐官方 `Ty::IsExtendable()`：
 *
 * ```cpp
 * return kind == TYPE_CLASS || kind == TYPE_ENUM || kind == TYPE_STRUCT || IsArray() ||
 *     IsPointer() || IsPrimitive() || IsCString();
 * ```
 *
 * `ConeClassLikeType.isInterface` 保留了 class/interface 的语义差异，因此 interface 不可作为
 * 目标；`VArray` 与 `CFunc` 也明确不在官方集合中。typealias 则递归检查展开类型，并在发现
 * 循环时返回 `false`，对应官方对 `IN_REFERENCE_CYCLE` 的保护。
 */
val ConeCangJieType.isExtendable: Boolean
    get() = isExtendable(linkedSetOf())

/**
 * 带 typealias 访问集合的内部实现，避免循环别名在类型层谓词中造成无限递归。
 */
private fun ConeCangJieType.isExtendable(visitedAliases: MutableSet<ClassId>): Boolean =
    when (this) {
        is ConeTypeAliasType -> visitedAliases.add(classId) && expandedType?.isExtendable(visitedAliases) == true
        is ConeClassLikeType -> !isInterface || classId == StdlibClassIds.RawArray
        is ConeStructType, is ConeEnumType, is ConePrimitiveType, is ConePointerType, is ConeCStringType -> true
        // Official IsExtendable() excludes VArray and Func/CFunc.
        else -> false
    }
