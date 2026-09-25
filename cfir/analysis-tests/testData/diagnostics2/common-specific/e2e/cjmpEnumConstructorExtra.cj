// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 证据：cjc 1.1.3 两段式编译实测（.workbuddy/tmp/cjmp_probe113/probe_results7c.txt）
// specific enum 多出构造器（common 穷尽）：specific 方向 NOT_MATCHED（锚构造器，官方 specific.cj:4:11）
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common enum E {
    | A
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific enum E {
    | A | <!NOT_MATCHED!>B<!>
}
