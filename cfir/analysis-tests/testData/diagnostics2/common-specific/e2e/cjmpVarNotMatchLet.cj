// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T01_VarNotMatchLet
// common let 由 specific var 实现：`'specific' 'var' can not match 'common' 'let'`（锚 specific 声明，官方 MatchCJMPVar）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common let x: Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_VAR_NOT_MATCH_LET!>public specific var x: Int64 = 1<!>
