// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T14_SpecificAbstractMemberNoExplicit
// specific abstract 类中无体且未写 specific/abstract 的成员：
// `'specific' abstract class function must have explicit 'specific' or 'abstract' modifier`（锚成员名）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common abstract class A {
    public common init()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific abstract class A {
    public specific init() {}
    public func <!CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER!>f<!>(): Int64
}
