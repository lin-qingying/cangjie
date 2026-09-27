// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T11_NonSpecificAbstractMember
// specific abstract 类新增非 specific 的 abstract 成员：`specific abstract class 'N' cannot have non-specific abstract function`
//（锚成员声明，官方 CheckAbstractClassMembers）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common abstract class N {
    public common init()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific abstract class N {
    public specific init() {}
    <!CJMP_NON_SPECIFIC_ABSTRACT_MEMBER_IN_SPECIFIC_CLASS!>public abstract func g(): Int64<!>
}
