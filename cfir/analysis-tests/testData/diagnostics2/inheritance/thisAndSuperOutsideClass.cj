// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   类成员函数（非构造器）里 `this()` → error: invalid calling 'this' outside the constructor（kind sema_invalid_this_call_outside_ctor，锚 `this` 首字母 1 个字符）
//   顶层函数里 `super(1)` → error: 'super' cannot be used outside class or struct or interface（kind sema_this_super_use_error_outside_class，锚 `super` 首字母 1 个字符）
// 官方实现 TypeCheckCall.cpp / TypeCheckReference.cpp：`this`/`super` delegation 出现在类外报
// sema_this_super_use_error_outside_class；出现在非构造器的函数体报 sema_invalid_this_call_outside_ctor。
// FILE: delegationInMemberFunc.cj
class C {
    public init() {}

    public func notCtor(): Unit {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>t<!>his()
    }
}

// FILE: delegationOutsideClass.cj
func outside(): Unit {
    <!THIS_SUPER_USE_ERROR_OUTSIDE_CLASS!>s<!>uper(1)
}
