// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C07）：common let 字段不能在构造器中
// 赋值，报 `cannot assign to immutable variable 'v'`（锚赋值表达式，官方 InitializationChecker::CheckLetFlag）。
package cjmp_p

public common class M {
    public common let v: Int64
    public common init() {
        <!COMMON_ASSIGN_TO_COMMON_IMMUTABLE_IN_CTOR!>v = 1<!>
    }
}
