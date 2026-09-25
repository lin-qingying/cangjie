// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// common 穷尽 enum 与 specific 非穷尽 enum 不兼容
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum E {
    | A | B
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!COMMON_NON_EXHAUSTIVE_PLATFORM_EXHAUSTIVE_MISMATCH!>public specific enum E {
    | A | B | ...
}<!>
