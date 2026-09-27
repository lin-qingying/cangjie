// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9_done/T08_DifferentSuperType
// 超类型集合不同：`'specific' class super types is not match 'common' super types`（锚 specific 声明，官方 MatchCommonNominalDeclWithSpecific）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public interface I {}

public common class S <: I {
    public common init()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!SPECIFIC_HAS_DIFFERENT_SUPER_TYPE!>public specific class S {
    public specific init() {}
}<!>
