// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.1.3 实测（本轮无法使用 1.0.5；CheckDeprecationLevelOnInheritors）；这里只记录官方诊断语义，不用 cjc 范围裁剪 CFIR 标记。
//   父类必须写 `open`，否则官方先报 sema_non_inheritable_super_class，语义诊断取不到。
//   父类 `@Deprecated["old", strict: true]` + 子类无注解 → error: overridden class 'B' should be marked with @Deprecated
//     （kind sema_deprecation_override_error）
//   同上但父类非 strict → warning: overridden class 'B' should be marked with @Deprecated（kind sema_deprecation_override_warning）
//   父 strict + 子 `@Deprecated["old"]`（放宽） → error: strictness of @Deprecated can not be weaken on inheritors
//     （kind sema_deprecation_weakening）
//   同时两处弃用使用点：子类继承列表里的父类名 `A` / `C`，以及 `super` 调用。
// CFIR 标记遵守 CfirDeclaration.source，覆盖从注解到声明体的整个声明单元。

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

<!DEPRECATION_WEAKENING!>@Deprecated["old"]
class D <: <!DEPRECATED_ERROR!>C<!> {
    public init() {
        <!DEPRECATED_ERROR!>super<!>()
    }
}<!>
