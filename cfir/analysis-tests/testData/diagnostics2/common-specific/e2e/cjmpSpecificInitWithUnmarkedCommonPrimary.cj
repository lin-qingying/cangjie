// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9/T17
// common 类的未标记主构造器是普通成员（主构造器不能带 common/specific），specific init 只与 common 标记的 init
// 配对：零错误。`sema_specific_init_common_primary_constructor` 在 1.1.3 无可达触发点，不得因 common 侧存在主构造器而报告。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class P2 {
    public P2(x: Int64) {}
    public common init(x: Bool)
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class P2 {
    public specific init(x: Bool) {}
}
