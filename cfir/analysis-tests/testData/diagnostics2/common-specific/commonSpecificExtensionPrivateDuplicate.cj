// LANGUAGE_VERSION: 1.1.0
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 `--experimental --output-type=chir` 实测（探针 cjmp_probe113/p9_done/C04）：同一类型的多个无接口
// common extend 中重名 private 成员报 `declaration 'common' extend 'C' has a conflicting private function 'f'`，
// 锚后出现的成员声明（官方 CheckCommonExtensions）。
package cjmp_p

public class C {
    public init() {}
}

common extend C {
    private func f(): Int64 { 1 }
}

common extend C {
    <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(): Int64 { 2 }<!>
}
