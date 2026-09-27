// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译（common --output-type=chir；specific --common-part-cjo）实测，探针 cjmp_probe113/p9/T09b_DuplicateExtensionsEmpty
// 同键 specific extend 出现两次：`declaration 'specific' extend 'C' has a conflicting extension`（官方 MergeCJMPExtensions）。
// 官方按 unordered_set 迭代挑选被报的那一个（实测锚第一个、区间始于 extend 关键字）；本仓库按源码顺序确定地报后出现者。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class C {
    public common init()
}

common extend C {}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific class C {
    public specific init() {}
}

specific extend C {}

<!SPECIFIC_HAS_DUPLICATE_EXTENSIONS!>specific extend C {}<!>
