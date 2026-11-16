// RUN_PIPELINE_TILL: FRONTEND
// 限定名 enum pattern（`Owner.Ctor` / `Owner<Args>.Ctor(x)`）的 owner 判定。
//
// 官方实现（external/cangjie_compiler/src/Sema/TypeCheckPattern.cpp）：
// - FindEnumPatternTargets：constructor 是 MemberAccess 时走 InferMemberAccess，在**限定符指向的类型**里
//   找 constructor，与 subject 无关；限定符可以是 enum 本身，也可以是指向 enum 的 typealias；
// - ChkEnumPattern：找到的 constructor 类型必须与 subject 类型相等，否则报 sema_pattern_not_match；
//   payload 个数不符报 sema_enum_pattern_param_size_error。
//
// 每个函数都与文件头的类型声明一起用 cjc 1.0.5 / 1.1.3 单独编译取证，两版结论一致；标记为本项目诊断名：
// sema_pattern_not_match -> PATTERN_NOT_MATCH、sema_undeclared_identifier -> UNRESOLVED_REFERENCE、
// sema_enum_pattern_param_size_error -> ENUM_PATTERN_PARAM_SIZE_ERROR。

enum Shape {
    | Circle
    | Square(Int64)
}

enum Color {
    | Red
    | Green
}

enum Slot<T> {
    | Free
    | Used(T)
}

enum Box {
    | Wrap(Shape)
    | Empty
}

type ShapeAlias = Shape

type SlotAlias<T> = Slot<T>

type IntSlot = Slot<Int64>

type StringSlot = Slot<String>

type Opt<T> = Option<T>

// ── 正例：限定符是 enum 本身或指向它的 typealias，零诊断 ──

func ownerIsEnum(shape: Shape): Int64 {
    match (shape) {
        case Shape.Circle => 1
        case Shape.Square(side) => side
    }
}

func ownerIsAlias(shape: Shape): Int64 {
    match (shape) {
        case ShapeAlias.Circle => 1
        case ShapeAlias.Square(side) => side
    }
}

func ownerIsInstantiatedGenericEnum(slot: Slot<Int64>): Int64 {
    match (slot) {
        case Slot<Int64>.Free => 0
        case Slot<Int64>.Used(value) => value
    }
}

func ownerIsBareGenericEnum(slot: Slot<Int64>): Int64 {
    match (slot) {
        case Slot.Free => 0
        case Slot.Used(value) => value
    }
}

func ownerIsAliasWithFixedArguments(slot: Slot<Int64>): Int64 {
    match (slot) {
        case IntSlot.Free => 0
        case IntSlot.Used(value) => value
    }
}

func ownerIsGenericAlias(slot: Slot<Int64>): Int64 {
    match (slot) {
        case SlotAlias<Int64>.Free => 0
        case SlotAlias.Used(value) => value
    }
}

func ownerIsOptionAlias(value: ?Int64): Int64 {
    match (value) {
        case Opt<Int64>.Some(inner) => inner
        case Opt.None => 0
    }
}

func ownerAliasInNestedPattern(box: Box): Int64 {
    match (box) {
        case Box.Wrap(ShapeAlias.Circle) => 1
        case Box.Wrap(ShapeAlias.Square(side)) => side
        case Box.Empty => 0
    }
}

func ownerAliasInLetCondition(shape: Shape): Int64 {
    if (let ShapeAlias.Square(side) <- shape) {
        return side
    }
    return 0
}

// ── owner 与 subject 不是同一个类型：constructor 能在限定符里找到，但类型不相等 ──

func ownerIsOtherEnum(shape: Shape): Int64 {
    match (shape) {
        case <!PATTERN_NOT_MATCH!>Color.Red<!> => 0
        case _ => 1
    }
}

func ownerTypeArgumentsDiffer(slot: Slot<Int64>): Int64 {
    match (slot) {
        case <!PATTERN_NOT_MATCH!>Slot<String>.Free<!> => 0
        case _ => 1
    }
}

func ownerAliasTypeArgumentsDiffer(slot: Slot<Int64>): Int64 {
    match (slot) {
        case <!PATTERN_NOT_MATCH!>StringSlot.Free<!> => 0
        case _ => 1
    }
}

func ownerIsOtherEnumInNestedPattern(box: Box): Int64 {
    match (box) {
        case Box.Wrap(<!PATTERN_NOT_MATCH!>Color.Red<!>) => 2
        case _ => 3
    }
}

func subjectIsNotEnum(count: Int64): Int64 {
    match (count) {
        case <!PATTERN_NOT_MATCH!>Shape.Circle<!> => 0
        case _ => 1
    }
}

// ── owner 正确但 payload 个数不符 ──

func ownerMatchesButArityDiffers(shape: Shape): Int64 {
    match (shape) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Shape.Circle(side)<!> => 0
        case _ => 1
    }
}

func aliasOwnerMatchesButArityDiffers(shape: Shape): Int64 {
    match (shape) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>ShapeAlias.Circle(side)<!> => 0
        case _ => 1
    }
}

// ── 限定符本身未声明：只报限定符，不再叠加 pattern 级诊断 ──

func ownerIsUndeclared(shape: Shape): Int64 {
    match (shape) {
        case <!UNRESOLVED_REFERENCE!>Missing<!>.Circle => 0
        case _ => 1
    }
}

main(): Int64 {
    return 0
}
