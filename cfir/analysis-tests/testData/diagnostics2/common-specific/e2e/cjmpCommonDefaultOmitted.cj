// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// common 自带默认实现时 specific 可省略（MustMatchWithPlatform 豁免）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func a(): Int64 {
    1
}
public common func b(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func b(): Int64 { 2 }
