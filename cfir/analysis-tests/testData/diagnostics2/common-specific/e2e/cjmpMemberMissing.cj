// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，见计划 §10 Phase 4.4
// common 类成员缺 specific 实现：成员级 NOT_MATCHED（锚 common 成员）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class R {
    public common init()
    <!NOT_MATCHED!>public common func f(): Int64<!>
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class R {
    public specific init() {}
}
