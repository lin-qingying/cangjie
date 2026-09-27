// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C06、p9/T15）：static init 给
// common static let 赋值同样由 InitializationChecker 报 `cannot assign to immutable variable 'x'`（锚赋值表达式）；
// `sema_common_static_let_cant_be_initialized_in_static_init`（GlobalVarChecker）被其先行拦截，1.1.3 无可达触发点。
package cjmp_p

public common class S {
    public common init() {}
    public common static let x: Int64
    static init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>x = 1<!>
    }
}
