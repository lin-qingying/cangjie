// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9/T18
// specific 类的主构造器带 `specific`：官方只报 `unexpected modifier 'specific' on primary constructor`（锚修饰符）。
// `sema_specific_primary_unmatched_var_decl` 在 1.1.3 无可达触发点，不得在修饰符错误之外追加。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Q {
    public common init(x: Int64)
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class Q {
    public <!WRONG_MODIFIER_TARGET!>specific<!> Q(x: Int64) {}
}
