// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（CheckDeprecationLevelOnInheritors，锚点是子声明整条语句，**不含** `@Deprecated` 注解行）。
//   父类必须写 `open`，否则官方先报 sema_non_inheritable_super_class，语义诊断取不到。
//   父类 `@Deprecated["old", strict: true]` + 子类无注解 → error: overridden class 'B' should be marked with @Deprecated
//     （kind sema_deprecation_override_error，锚整条子类声明，含构造器体）
//   同上但父类非 strict → warning: overridden class 'B' should be marked with @Deprecated（kind sema_deprecation_override_warning）
//   父 strict + 子 `@Deprecated["old"]`（放宽） → error: strictness of @Deprecated can not be weaken on inheritors
//     （kind sema_deprecation_weakening，锚整条子类声明，不含注解行）
//   同时两处弃用使用点：子类继承列表里的父类名 `A` / `C`（sema_deprecated_error，锚父类名首字母 1 个字符）
//     与 `super` 关键字（sema_deprecated_error，锚 `super` 5 个字符不含括号，消息 "class 'A' is deprecated. old."）。
// 本项目：子类声明的标记按官方范围（不含注解行）写。

// FILE: strictParent.cj
@Deprecated["old", strict: true]
open class A {
    public init() {}
}

<!DEPRECATION_OVERRIDE_ERROR!>class B <: <!DEPRECATED_ERROR!>A<!> {
    public init() {
        <!DEPRECATED_ERROR!>super<!>()
    }
}<!>

// FILE: weakening.cj
@Deprecated["old", strict: true]
open class C {
    public init() {}
}

<!DEPRECATION_WEAKENING!>class D <: <!DEPRECATED_ERROR!>C<!> {
    public init() {
        <!DEPRECATED_ERROR!>super<!>()
    }
}<!>
