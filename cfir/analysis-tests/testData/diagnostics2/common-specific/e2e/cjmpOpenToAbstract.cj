// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T10_OpenToAbstract
// open common 成员被 abstract specific 实现：`open common function can not be overridden with abstract specific function`
//（锚 specific 成员；官方同位置的修饰符失配诊断被诊断引擎去重，只剩这一条）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common abstract class O {
    public common init()
    public common open func f(): Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific abstract class O {
    public specific init() {}
    <!OPEN_ABSTRACT_SPECIFIC_CAN_NOT_REPLACE_OPEN_COMMON!>public specific abstract func f(): Int64<!>
}
