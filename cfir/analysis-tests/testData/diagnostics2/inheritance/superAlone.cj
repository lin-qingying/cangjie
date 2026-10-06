// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：构造器里单独写 `super`（不带调用/点访问）
//   → error: invalid super expression, it can only be used on the left-hand side of a dot（kind sema_illegal_super_alone，锚 `super` 首字母 1 个字符）。
// 官方实现 TypeCheckReference.cpp：super 表达式不是 `super.xxx` 的左值时报 sema_illegal_super_alone。
open class Base {
    public init() {}
}

class Child <: Base {
    public init() {
        <!ILLEGAL_SUPER_ALONE!>s<!>uper
    }
}
