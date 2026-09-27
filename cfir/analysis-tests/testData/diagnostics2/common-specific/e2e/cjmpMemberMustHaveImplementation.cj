// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T04_MemberMustHaveImpl
// common 成员有默认实现而 specific 改为 abstract：`the member f must have body in 'specific' R`
//（锚 specific 成员，官方 TrySetSpecificImpl / NeedToReportMissingBody）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common abstract class R {
    public common init()
    public common func f(): Int64 {
        1
    }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific abstract class R {
    public specific init() {}
    <!SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION!>public specific abstract func f(): Int64<!>
}
