// RUN_PIPELINE_TILL: FRONTEND
// subject 的 constructor 已知时，enum pattern 与泛型 enum 的 use-site constructor 必须被认作同一个。
//
// `Slot<Int64>.Used(7)` 经 substitution scope 解析到的是 constructor 的 substitution override 副本，
// pattern 的 constructor 引用指向 enum 声明里的原始 constructor。常量传播按 constructor 身份比较 tag：
// 两侧不归一，匹配的分支会被误判为不可达，真正不可达的后续分支反而漏报。
//
// 官方由 CHIR 常量传播给出结论（`chir_unreachable_pattern` -> UNREACHABLE_PATTERN，锚整个 pattern）。
// CHIR 只在 sema 无错误时运行，所以本文件不能含任何 sema 错误；整份文件用 cjc 1.0.5 / 1.1.3 编译取证，
// 两版结论一致（不写 main：静态库目标下官方会多报一条与本题无关的 `chir_dce_unused_function_main`）。

enum Slot<T> {
    | Free
    | Used(T)
}

enum Pair<A, B> {
    | Both(A, B)
    | Neither
}

type SlotAlias<T> = Slot<T>

func knownPayloadConstructor(): Int64 {
    var slot: Slot<Int64> = Slot<Int64>.Used(7)
    match (slot) {
        case Used(value) => value
        case <!UNREACHABLE_PATTERN!>Free<!> => 0
    }
}

func knownPayloadConstructorThroughAlias(): Int64 {
    var slot: SlotAlias<Int64> = Slot<Int64>.Used(7)
    match (slot) {
        case Used(value) => value
        case <!UNREACHABLE_PATTERN!>Free<!> => 0
    }
}

func knownNullaryConstructor(): Int64 {
    var slot: Slot<Int64> = Slot<Int64>.Free
    match (slot) {
        case <!UNREACHABLE_PATTERN!>Used(value)<!> => value
        case Free => 0
    }
}

func knownConstructorOfTwoParameterEnum(): Int64 {
    var pair: Pair<Int64, Int64> = Pair<Int64, Int64>.Both(1, 2)
    match (pair) {
        case Both(left, right) => left + right
        case <!UNREACHABLE_PATTERN!>Neither<!> => 0
    }
}

func knownQualifiedPattern(): Int64 {
    var slot: Slot<Int64> = Slot<Int64>.Used(7)
    match (slot) {
        case Slot<Int64>.Used(value) => value
        case <!UNREACHABLE_PATTERN!>Slot<Int64>.Free<!> => 0
    }
}

func unknownConstructorKeepsBothBranches(slot: Slot<Int64>): Int64 {
    match (slot) {
        case Used(value) => value
        case Free => 0
    }
}
