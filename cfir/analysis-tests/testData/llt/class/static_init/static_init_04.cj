// 官方 cjc 1.0.5 实测：`static init() { if (1 > 2) { return } else { return } }`
//   → error: 'return' cannot be used inside the static initializer（两处 `return` 各报一条，常量假分支内同样遍历）。
// 官方实现 PreCheck.cpp::CheckReturnAndJump：当前函数体是静态初始化器时，对每个 return 表达式报
// sema_invalid_return_in_static_init，并在该分支把 return 的类型置为 invalid（不再报返回类型不匹配）。
// 该 LLT 用例此前按 CFIR 旧行为（不报）固化期望，本批次按官方证据补上标记。

class A {
    static init() {
        if (1 > 2) {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        } else {
            <!INVALID_RETURN_IN_STATIC_INIT!>return<!>
        }
    }
}

main() {
    return 0
}
