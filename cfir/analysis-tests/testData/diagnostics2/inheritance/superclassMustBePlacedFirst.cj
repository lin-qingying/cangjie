// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：`class C <: I & A {}`（接口在前、类在后）→
//   error: super class 'A' must be placed at the beginning of supertype list（kind sema_superclass_must_be_placed_at_first，锚父类名首字母 1 个字符）。
// 官方实现 TypeCheckClassLike::CheckAndAddSubDecls：父类不是列表第一个且前面已有接口时报该诊断。
// 合法对照：类在前接口在后（`class C <: A & I {}`）零诊断；两个父类报 ILLEGAL_MULTI_INHERITANCE（官方 kind sema_illegal_multi_inheritance，见 multipleClassSupers.cj）。
interface Marker {}

open class Base {
    public init() {}
}

class Child <: Marker & <!SUPERCLASS_MUST_BE_PLACED_AT_FIRST!>B<!>ase {
    init() {
        super()
    }
}
