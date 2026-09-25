// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// class 与 struct 种类不同：种类诊断 + 成员与声明双向 NOT_MATCHED
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!NOT_MATCHED!>public common class K {
    <!NOT_MATCHED!>public common init()<!>
}<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_KIND!>public specific struct K {
    <!NOT_MATCHED!>public specific init() {}<!>
}<!>
