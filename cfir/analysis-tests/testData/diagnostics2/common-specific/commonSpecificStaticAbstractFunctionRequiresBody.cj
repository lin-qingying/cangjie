// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 权威证据：psi/build/cjmp-verification/20261113-cjmp-extend-static-abstract-authority/cjc-results.json
// static-abstract-common（SDK 1.1.3，tag v1.1.3，commit 14276740ffec1262b81b6b5236b1d840af270b37）。
// 下方源码与权威 probe 一致：无 Parse 错误，进入 Sema 后唯一诊断是 sema_missing_func_body。
// 官方 firstChar 只作语义证据；本仓按 CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS 锚 func 至返回类型。
package cjmp_p

public common abstract class AuthorityCommon {
    public common init()
    public static abstract <!MISSING_FUNC_BODY!>func absFn(): Unit<!>
}
