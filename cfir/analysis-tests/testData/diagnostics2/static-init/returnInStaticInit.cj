// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 实测：
//   `static init() { return }` → error: 'return' cannot be used inside the static initializer（锚 `return` 关键字）
//   `static init() { return 1 }` → 同一诊断（锚 `return 1` 整段，含实参）；官方不再报返回类型不匹配
//   `static init() { if (true) { return } }` → 同一诊断（嵌套块内同样非法）
//   类同时有实例 `init()` 时静态初始化器依旧是 static init，诊断不变。
// 官方实现 PreCheck.cpp::CheckReturnAndJump：当前函数体是静态初始化器时报 sema_invalid_return_in_static_init。
// 注意：官方对 static init 内的 `return 1` 只报这一条，不额外报返回类型不匹配。
class C {
    static var x: Int64 = 0

    static init() {
        <!INVALID_RETURN_IN_STATIC_INIT!>return 1<!>
    }
}

class D {
    static var x: Int64 = 0

    init() {
    }

    static init() {
        if (true) {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        }
    }
}
