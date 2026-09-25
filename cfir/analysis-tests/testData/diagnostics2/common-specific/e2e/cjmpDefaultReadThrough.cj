// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// common 侧默认值对 specific 调用读穿（D7），调用 a() 合法
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func a(x!: Int64 = 1): Int64

public func use(): Int64 {
    a()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func a(x!: Int64): Int64 { x }

public func use2(): Int64 {
    a()
}
