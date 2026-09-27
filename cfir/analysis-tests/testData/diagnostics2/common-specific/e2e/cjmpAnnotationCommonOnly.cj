// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T05_DifferentAnnotation
// 仅 common 侧带 @OverflowWrapping：官方零诊断（common 注解随 cjo 传播到 specific，不构成失配）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

@OverflowWrapping
public common func f(): Int64

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific func f(): Int64 {
    1
}
